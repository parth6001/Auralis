package com.auralis.player.data

import com.auralis.player.audio.AudioProfile
import com.auralis.player.audio.OutputMode
import com.auralis.player.audio.ReplayGainMode
import org.json.JSONArray
import org.json.JSONObject

object ProfileCodec {
    fun encode(profile: AudioProfile): String {
        require(profile.valid())
        return JSONObject().put("schema", 1).put("name", profile.name).put("mode", profile.mode.name)
            .put("bands", JSONArray(profile.bands)).put("preamp", profile.preamp).put("balance", profile.balance)
            .put("crossfeed", profile.crossfeed).put("limiter", profile.limiter).put("replayGain", profile.replayGain.name).toString(2)
    }
    fun decode(text: String): AudioProfile {
        require(text.length <= 16_384) { "Preset exceeds 16 KB" }
        val json = JSONObject(text)
        require(json.getInt("schema") == 1) { "Unsupported preset version" }
        val bands = json.getJSONArray("bands")
        require(bands.length() == 5)
        return AudioProfile(json.getString("name"), OutputMode.valueOf(json.getString("mode")),
            (0..4).map { bands.getDouble(it).toFloat() }, json.getDouble("preamp").toFloat(),
            json.getDouble("balance").toFloat(), json.getDouble("crossfeed").toFloat(), json.getBoolean("limiter"), ReplayGainMode.valueOf(json.optString("replayGain", "OFF")))
            .also { require(it.valid()) { "Preset values out of range" } }
    }
}
