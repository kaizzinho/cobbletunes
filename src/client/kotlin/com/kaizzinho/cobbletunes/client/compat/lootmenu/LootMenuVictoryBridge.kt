package com.kaizzinho.cobbletunes.client.compat.lootmenu

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screen.Screen

object LootMenuVictoryBridge {
    private const val MOD_ID = "cobblemon_loot_menu"
    private const val LOOT_SCREEN_CLASS =
        "dev.cobblemonlootmenu.client.gui.LootSelectionScreen"

    val available: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded(MOD_ID)
    }

    fun isLootScreen(screen: Screen?): Boolean =
        available && screen?.javaClass?.name == LOOT_SCREEN_CLASS
}
