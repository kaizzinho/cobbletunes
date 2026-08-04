package com.kaizzinho.cobbletunes.client.config

import com.google.gson.GsonBuilder
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import java.io.File

data class CobbleTunesClientConfig(
    var replaceAmbience: Boolean = true,
    var replaceMenuMusic: Boolean = true,
    var replaceBattleMusic: Boolean = true,
    var musicVolume: Float = 1.0f,
    var crossfadeSeconds: Float = 2.5f,
    var shuffleAmbienceTracks: Boolean = true,
    // world/dimension join uses one fixed gap; track/biome gaps use ranges
    // biome transitions debounce and restart if the biome changes again
    var worldJoinSilenceSeconds: Float = 10f,
    var trackEndSilenceMinSeconds: Float = 90f,
    var trackEndSilenceMaxSeconds: Float = 180f,
    var biomeTransitionSilenceMinSeconds: Float = 4f,
    var biomeTransitionSilenceMaxSeconds: Float = 8f,
    // handy for bug reports; server-side traces have their own cfg file
    var debugLogging: Boolean = false
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val configFile: File
            get() = File(FabricLoader.getInstance().configDir.toFile(), "$MOD_ID-client.json")

        fun load(): CobbleTunesClientConfig {
            val file = configFile
            if (!file.exists()) {
                val default = CobbleTunesClientConfig()
                default.save()
                return default
            }
            return try {
                file.reader().use { gson.fromJson(it, CobbleTunesClientConfig::class.java) }
                    ?: CobbleTunesClientConfig()
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to read client config, falling back to defaults", e)
                CobbleTunesClientConfig()
            }
        }
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writer().use { gson.toJson(this, it) }
        } catch (e: Exception) {
            LOGGER.warn("[$MOD_ID] Failed to write client config", e)
        }
    }
}