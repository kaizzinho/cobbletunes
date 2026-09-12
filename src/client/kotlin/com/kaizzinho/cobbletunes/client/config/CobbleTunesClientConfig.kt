package com.kaizzinho.cobbletunes.client.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import java.io.File

data class CobbleTunesClientConfig(
    var replaceAmbience: Boolean = true,
    var replaceMenuMusic: Boolean = true,
    var replaceBattleMusic: Boolean = true,
    var enableEvolutionMusic: Boolean = true,
    var musicVolume: Float = 1.0f,
    var crossfadeSeconds: Float = 2.5f,
    var shuffleAmbienceTracks: Boolean = true,
    var worldJoinSilenceSeconds: Float = 10f,
    var trackEndSilenceMinSeconds: Float = 90f,
    var trackEndSilenceMaxSeconds: Float = 180f,
    var biomeTransitionSilenceMinSeconds: Float = 4f,
    var biomeTransitionSilenceMaxSeconds: Float = 8f,
    var debugLogging: Boolean = false
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val configRoot: File
            get() = FabricLoader.getInstance().configDir.toFile()

        private val configFile: File
            get() = File(File(configRoot, MOD_ID), "client.json")

        private val legacyConfigFile: File
            get() = File(configRoot, "$MOD_ID-client.json")

        fun load(): CobbleTunesClientConfig {
            val file = configFile
            migrateLegacyConfig(file)
            if (!file.exists()) {
                val default = CobbleTunesClientConfig()
                default.save()
                return default
            }
            return try {
                val json = file.reader().use { JsonParser.parseReader(it) }
                val loaded = gson.fromJson(json, CobbleTunesClientConfig::class.java)
                    ?: CobbleTunesClientConfig()
                if (!json.asJsonObject.has("enableEvolutionMusic")) {
                    loaded.enableEvolutionMusic = true
                }
                loaded
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to read client config, falling back to defaults", e)
                CobbleTunesClientConfig()
            }
        }

        private fun migrateLegacyConfig(file: File) {
            val legacy = legacyConfigFile
            if (file.exists() || !legacy.exists()) return

            try {
                file.parentFile?.mkdirs()
                legacy.copyTo(file, overwrite = false)
                if (!legacy.delete()) {
                    LOGGER.warn("[$MOD_ID] Migrated client config but could not remove old file: ${legacy.path}")
                }
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to migrate client config from ${legacy.path}", e)
            }
        }
    }

    fun copyFrom(other: CobbleTunesClientConfig) {
        replaceAmbience = other.replaceAmbience
        replaceMenuMusic = other.replaceMenuMusic
        replaceBattleMusic = other.replaceBattleMusic
        enableEvolutionMusic = other.enableEvolutionMusic
        musicVolume = other.musicVolume
        crossfadeSeconds = other.crossfadeSeconds
        shuffleAmbienceTracks = other.shuffleAmbienceTracks
        worldJoinSilenceSeconds = other.worldJoinSilenceSeconds
        trackEndSilenceMinSeconds = other.trackEndSilenceMinSeconds
        trackEndSilenceMaxSeconds = other.trackEndSilenceMaxSeconds
        biomeTransitionSilenceMinSeconds = other.biomeTransitionSilenceMinSeconds
        biomeTransitionSilenceMaxSeconds = other.biomeTransitionSilenceMaxSeconds
        debugLogging = other.debugLogging
    }

    fun save(): Boolean {
        return try {
            configFile.parentFile?.mkdirs()
            configFile.writer().use { gson.toJson(this, it) }
            true
        } catch (e: Exception) {
            LOGGER.warn("[$MOD_ID] Failed to write client config", e)
            false
        }
    }
}
