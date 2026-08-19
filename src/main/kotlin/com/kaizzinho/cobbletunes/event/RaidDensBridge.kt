package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.kaizzinho.cobbletunes.LOGGER
import net.fabricmc.loader.api.FabricLoader
import java.lang.reflect.Method

object RaidDensBridge {
    private const val MOD_ID = "cobblemonraiddens"
    private const val RAID_BATTLE_CLASS = "com.necro.raid.dens.common.util.IRaidBattle"
    private const val RAID_INSTANCE_CLASS = "com.necro.raid.dens.common.raids.RaidInstance"
    private const val RAID_BOSS_CLASS = "com.necro.raid.dens.common.data.raid.RaidBoss"
    private var compatibilityFailureLogged = false

    private data class ApiMethods(
        val raidBattleClass: Class<*>,
        val isRaidBattle: Method,
        val getRaidBattle: Method,
        val getRaidBoss: Method,
        val getTier: Method
    )

    private val apiMethods: ApiMethods? by lazy {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return@lazy null

        runCatching {
            val raidBattleClass = Class.forName(RAID_BATTLE_CLASS)
            val raidInstanceClass = Class.forName(RAID_INSTANCE_CLASS)
            val raidBossClass = Class.forName(RAID_BOSS_CLASS)
            ApiMethods(
                raidBattleClass = raidBattleClass,
                isRaidBattle = raidBattleClass.getMethod("crd_isRaidBattle"),
                getRaidBattle = raidBattleClass.getMethod("crd_getRaidBattle"),
                getRaidBoss = raidInstanceClass.getMethod("getRaidBoss"),
                getTier = raidBossClass.getMethod("getTier")
            )
        }.getOrElse {
            logCompatibilityFailure(it)
            null
        }
    }

    fun resolveTier(battle: PokemonBattle): String? {
        val methods = apiMethods ?: return null
        if (!methods.raidBattleClass.isInstance(battle)) return null

        return runCatching {
            val isRaid = methods.isRaidBattle.invoke(battle) as? Boolean ?: false
            if (!isRaid) return@runCatching null

            val raidInstance = methods.getRaidBattle.invoke(battle) ?: return@runCatching null
            val raidBoss = methods.getRaidBoss.invoke(raidInstance) ?: return@runCatching null
            val tier = methods.getTier.invoke(raidBoss) ?: return@runCatching null
            mapTier((tier as? Enum<*>)?.name ?: tier.toString()) ?: "EPIC"
        }.getOrElse {
            logCompatibilityFailure(it)
            null
        }
    }

    private fun mapTier(rawTier: String): String? = when (rawTier.uppercase()) {
        "TIER_ONE" -> "UNCOMMON"
        "TIER_TWO" -> "RARE"
        "TIER_THREE", "TIER_FOUR" -> "EPIC"
        "TIER_FIVE", "TIER_SIX" -> "LEGENDARY"
        "TIER_SEVEN" -> "MYTHIC"
        else -> null
    }

    private fun logCompatibilityFailure(error: Throwable) {
        if (compatibilityFailureLogged) return
        compatibilityFailureLogged = true
        LOGGER.warn(
            "[cobbletunes] Cobblemon Raid Dens integration disabled: {}",
            error.cause?.message ?: error.message ?: error.javaClass.simpleName
        )
    }
}
