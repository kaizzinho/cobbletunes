package com.kaizzinho.cobbletunes.client.compat.fancymenu

import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.SoundInstanceListener
import net.minecraft.sound.SoundCategory

object FancyMenuMusicSuppressor {
    private var registered = false
    private val loggedSoundIds = mutableSetOf<String>()

    val available: Boolean
        get() = FabricLoader.getInstance().isModLoaded("fancymenu")

    fun register(
        config: CobbleTunesClientConfig,
        debugLog: (String) -> Unit
    ) {
        if (registered || !available) return
        registered = true

        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            client.soundManager.registerListener(SoundInstanceListener { sound, _, _ ->
                val shouldSuppress = config.replaceMenuMusic &&
                    client.world == null &&
                    sound.category == SoundCategory.MUSIC &&
                    sound.id.namespace != MOD_ID

                if (shouldSuppress) {
                    client.soundManager.stop(sound)
                    if (loggedSoundIds.add(sound.id.toString())) {
                        debugLog("[FancyMenu] suppressed menu music: ${sound.id}")
                    }
                }
            })

            silenceExistingMenuMusic(client, config, debugLog)
            debugLog("[FancyMenu] menu music suppression active")
        }
    }

    fun silenceExistingMenuMusic(
        client: MinecraftClient,
        config: CobbleTunesClientConfig,
        debugLog: (String) -> Unit
    ) {
        if (!available || !config.replaceMenuMusic || client.world != null) return
        client.soundManager.stopSounds(null, SoundCategory.MUSIC)
        debugLog("[FancyMenu] cleared existing menu music")
    }
}
