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
        private val configFile: File
            get() = File(FabricLoader.getInstance().configDir.toFile(), "$MOD_ID-server.json")

        private var instance: CobbleTunesServerConfig = CobbleTunesServerConfig()

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
