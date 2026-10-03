package com.auralis.player.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OutputDevice(val id: Int, val key: String, val name: String, val type: String, val rates: List<Int>, val channels: List<Int>)
data class AudioStatus(val devices: List<OutputDevice> = emptyList(), val profileKey: String = "Speaker||",
    val profile: AudioProfile = AudioProfile(), val dspSupported: Boolean = false, val gainInfo: String = "ReplayGain tags unavailable",
    val notice: String = "Actual routed device and output PCM format are not exposed by this playback path.") {
    val path: String get() = if (profile.mode == OutputMode.ENHANCED_IEM && dspSupported) "DSP active" else "System mixed"
    fun report(): String = buildString {
        appendLine("Auralis local audio diagnostic")
        appendLine("Requested: ${profile.mode}; path: $path")
        appendLine("Profile: ${profile.name}")
        appendLine("Connected outputs (capabilities, not negotiated format):")
        devices.forEach { appendLine("${it.type}: ${it.name}; rates=${it.rates}; channels=${it.channels}") }
        appendLine("App DSP supported for current PCM: $dspSupported")
        appendLine("ReplayGain mode: ${profile.effective().replayGain}; $gainInfo")
        appendLine("Preamp: ${profile.effective().safePreamp()} dB; balance=${profile.effective().balance}; crossfeed=${profile.effective().crossfeed}; limiter=${profile.effective().limiter}")
        appendLine(notice)
        appendLine("Exclusive USB and bit-perfect output are not verified.")
    }
}

class AudioStatusStore {
    internal val mutable = MutableStateFlow(AudioStatus())
    val state = mutable.asStateFlow()
}

class AudioRouting(context: Context, private val changed: (List<OutputDevice>) -> Unit, private val removed: () -> Unit) : AutoCloseable {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { refresh() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any { it.isSink && type(it) in setOf("USB", "Wired", "Bluetooth") }) removed()
            refresh()
        }
    }
    init { manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())); refresh() }
    private fun refresh() {
        changed(manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { type(it) != "Other" }.map {
            val type = type(it)
            val name = it.productName?.toString()?.take(128).orEmpty().ifBlank { type }
            val address = if (Build.VERSION.SDK_INT >= 28) it.address else ""
            OutputDevice(it.id, RouteRules.profileKey(type, name, address), name, type, it.sampleRates.toList(), it.channelCounts.toList())
        }.sortedBy { when (it.type) { "USB" -> 0; "Wired" -> 1; "Bluetooth" -> 2; else -> 3 } })
    }
    override fun close() { manager.unregisterAudioDeviceCallback(callback) }
    private fun type(device: AudioDeviceInfo) = when (device.type) {
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
        else -> "Other"
    }
}
