package com.kaizzinho.cobbletunes.config

import com.google.gson.GsonBuilder
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import java.io.File

data class CobbleTunesServerConfig(
    var debugLogging: Boolean = false
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val configRoot: File
            get() = FabricLoader.getInstance().configDir.toFile()

        private val configFile: File
            get() = File(File(configRoot, MOD_ID), "server.json")

        private val legacyConfigFile: File
            get() = File(configRoot, "$MOD_ID-server.json")

        private var instance: CobbleTunesServerConfig = CobbleTunesServerConfig()

        val current: CobbleTunesServerConfig get() = instance

        fun load(): CobbleTunesServerConfig {
            val file = configFile
            migrateLegacyConfig(file)
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

        private fun migrateLegacyConfig(file: File) {
            val legacy = legacyConfigFile
            if (file.exists() || !legacy.exists()) return

            try {
                file.parentFile?.mkdirs()
                legacy.copyTo(file, overwrite = false)
                if (!legacy.delete()) {
                    LOGGER.warn("[$MOD_ID] Migrated server config but could not remove old file: ${legacy.path}")
                }
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to migrate server config from ${legacy.path}", e)
            }
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
