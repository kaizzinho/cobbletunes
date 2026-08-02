package com.kaizzinho.cobbletunes.config

import com.google.gson.GsonBuilder
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import java.io.File

/**
 * Server-side/common debug config — separate from CobbleTunesClientConfig
 * because CobblemonBattleListener and StructureZoneDetector live in
 * main/kotlin (common source set) and cannot reference client-only classes.
 * On a dedicated server this runs in an entirely separate JVM from any
 * client, so the two configs are genuinely independent files even though
 * they share the same field name/meaning — enabling debug logging for a
 * bug report means toggling BOTH files' debugLogging to true if the issue
 * could involve server-side behavior (structure detection, RCT tier
 * resolution), or just the client one if it's purely an audio/playback issue.
 *
 * Serialized to config/cobbletunes-server.json.
 */
data class CobbleTunesServerConfig(
    var debugLogging: Boolean = false
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val configFile: File
            get() = File(FabricLoader.getInstance().configDir.toFile(), "$MOD_ID-server.json")

        private var instance: CobbleTunesServerConfig = CobbleTunesServerConfig()

        /** Current loaded config — call load() once at startup before reading this. */
        val current: CobbleTunesServerConfig get() = instance

        fun load(): CobbleTunesServerConfig {
            val file = configFile
            if (!file.exists()) {
                instance = CobbleTunesServerConfig()
                instance.save()
                return instance
            }
            instance = try {
                file.reader().use { gson.fromJson(it, CobbleTunesServerConfig::class.java) }
                    ?: CobbleTunesServerConfig()
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to read server config, falling back to defaults", e)
                CobbleTunesServerConfig()
            }
            return instance
        }
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writer().use { gson.toJson(this, it) }
        } catch (e: Exception) {
            LOGGER.warn("[$MOD_ID] Failed to write server config", e)
        }
    }
}
