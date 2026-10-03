package com.auralis.player.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auralis.player.audio.*

@Composable
fun AudioControls(status: AudioStatus, update: (AudioProfile) -> Unit) {
    val p = status.profile
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Listen your way", style = MaterialTheme.typography.headlineSmall)
        Text("${status.path} · ${status.devices.firstOrNull()?.name ?: "System output"}")
        Text("Profile follows the highest-priority connected output. Android may route elsewhere; verify your output before listening.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(p.name, { if (it.length in 1..80) update(p.copy(name = it)) }, label = { Text("Output / IEM profile name") }, modifier = Modifier.fillMaxWidth())
        FilterChip(p.mode == OutputMode.PURE_DIRECT, { update(p.copy(mode = OutputMode.PURE_DIRECT)) }, label = { Text("Pure Direct Mode") })
        FilterChip(p.mode == OutputMode.ENHANCED_IEM, { update(p.copy(mode = OutputMode.ENHANCED_IEM)) }, label = { Text("Enhanced IEM Mode") })
        if (p.mode == OutputMode.PURE_DIRECT) Text("Application DSP is bypassed. Android may still mix or resample. Exclusive and bit-perfect output are not verified.")
        else {
            Text("Five-band EQ", style = MaterialTheme.typography.titleMedium)
            listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").forEachIndexed { index, label ->
                Text("$label · ${p.bands[index].toInt()} dB")
                Slider(p.bands[index], { value -> update(p.copy(bands = p.bands.mapIndexed { i, old -> if (i == index) value else old })) }, valueRange = -12f..12f, steps = 23, modifier = Modifier.semantics { contentDescription = "Equalizer $label" })
            }
            Text("Preamp ${p.preamp.toInt()} dB · applied headroom ${p.safePreamp().toInt()} dB")
            Slider(p.preamp, { update(p.copy(preamp = it)) }, valueRange = -24f..0f, modifier = Modifier.semantics { contentDescription = "Preamp" })
            Text("Left / right balance")
            Slider(p.balance, { update(p.copy(balance = it)) }, valueRange = -1f..1f, modifier = Modifier.semantics { contentDescription = "Left right balance" })
            Text("Crossfeed ${(p.crossfeed * 100).toInt()}%")
            Slider(p.crossfeed, { update(p.copy(crossfeed = it)) }, valueRange = 0f..0.3f, modifier = Modifier.semantics { contentDescription = "Crossfeed" })
            Row { Checkbox(p.limiter, { update(p.copy(limiter = it)) }); Text("Peak limiter", Modifier.padding(top = 12.dp)) }
            Text("DSP applies to mono/stereo PCM16. ${if (status.dspSupported) "Current PCM is supported." else "Start playback to check the current PCM format."}")
            Text("ReplayGain", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ReplayGainMode.entries.forEach { mode -> FilterChip(p.replayGain == mode, { update(p.copy(replayGain = mode)) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }) } }
            Text(status.gainInfo, style = MaterialTheme.typography.bodySmall)
            Text("Only source tags exposed by Media3 are used. Album falls back to track; absent tags apply no gain. Positive gain is limited by headroom.", style = MaterialTheme.typography.bodySmall)
        }
        Text("Start at a low system volume when changing IEMs or profiles. Auralis does not change the system volume.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Auralis audio diagnostic", status.report()))
        }) { Text("Copy audio diagnostic") }
        Text(status.notice, style = MaterialTheme.typography.bodySmall)
    }
}
