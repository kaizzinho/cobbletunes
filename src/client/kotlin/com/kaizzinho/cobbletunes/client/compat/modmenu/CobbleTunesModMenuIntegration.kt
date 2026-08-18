package com.kaizzinho.cobbletunes.client.compat.modmenu

import com.kaizzinho.cobbletunes.client.config.CobbleTunesConfigScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

// loaded only when mod menu is around
class CobbleTunesModMenuIntegration : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { parent ->
            CobbleTunesConfigScreen(parent)
        }
}
