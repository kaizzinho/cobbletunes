package com.kaizzinho.cobbletunes.client.compat.fancymenu

import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundCategory
import java.util.Collections
import java.util.WeakHashMap

object FancyMenuMusicSuppressor {
    private var registered = false
    private var config: CobbleTunesClientConfig? = null
    private var debugLog: ((String) -> Unit)? = null
    private val suppressedElements = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private val loggedElementTypes = mutableSetOf<String>()

    val available: Boolean
        get() = FabricLoader.getInstance().isModLoaded("fancymenu")

    fun register(
        config: CobbleTunesClientConfig,
        debugLog: (String) -> Unit
    ) {
        if (registered || !available) return
        registered = true
        this.config = config
        this.debugLog = debugLog
        debugLog("[FancyMenu] direct menu audio suppression active")
    }

    @JvmStatic
    fun shouldSuppressAudioElement(element: Any): Boolean {
        val currentConfig = config ?: return false
        if (!registered || !available || !currentConfig.replaceMenuMusic) {
            suppressedElements.remove(element)
            return false
        }

        val client = MinecraftClient.getInstance()
        if (client.world != null) {
            suppressedElements.remove(element)
            return false
        }

        val source = runCatching {
            element.javaClass.getMethod("getSoundSource").invoke(element)
        }.getOrNull() ?: return false

        if (!source.toString().equals("music", ignoreCase = true)) {
            suppressedElements.remove(element)
            return false
        }

        if (suppressedElements.add(element)) {
            runCatching {
                element.javaClass.getMethod("resetAudioElementKeepAudios").invoke(element)
            }.onFailure {
                debugLog?.invoke("[FancyMenu] failed to stop menu audio: ${it.javaClass.simpleName}")
            }
        }

        if (loggedElementTypes.add(element.javaClass.name)) {
            debugLog?.invoke("[FancyMenu] suppressed music audio element")
        }
        return true
    }

    fun silenceExistingMenuMusic(
        client: MinecraftClient,
        config: CobbleTunesClientConfig,
        debugLog: (String) -> Unit
    ) {
        if (!available || !config.replaceMenuMusic || client.world != null) return
        client.soundManager.stopSounds(null, SoundCategory.MUSIC)
        debugLog("[FancyMenu] cleared minecraft menu music")
    }
}
