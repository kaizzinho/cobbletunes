package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.UUID
import java.util.function.Consumer

object RaidDensBridge {
    private const val MOD_ID = "cobblemonraiddens"
    private const val RAID_ACCESSOR_CLASS = "com.necro.raid.dens.common.util.IRaidAccessor"
    private const val RAID_BATTLE_CLASS = "com.necro.raid.dens.common.util.IRaidBattle"
    private const val RAID_INSTANCE_CLASS = "com.necro.raid.dens.common.raids.RaidInstance"
    private const val RAID_BOSS_CLASS = "com.necro.raid.dens.common.data.raid.RaidBoss"
    private const val RAID_TIER_CLASS = "com.necro.raid.dens.common.data.raid.RaidTier"
    private const val RAID_HELPER_CLASS = "com.necro.raid.dens.common.raids.helpers.RaidHelper"
    private const val RAID_EVENTS_CLASS = "com.necro.raid.dens.common.events.RaidEvents"
    private const val RAID_END_EVENT_CLASS = "com.necro.raid.dens.common.events.RaidEndEvent"
    private var compatibilityFailureLogged = false
    private var raidEndCompatibilityFailureLogged = false

    private data class ApiMethods(
        val raidAccessorClass: Class<*>,
        val isRaidBoss: Method,
        val getEntityRaidBoss: Method,
        val getRaidId: Method,
        val raidBattleClass: Class<*>,
        val isRaidBattle: Method,
        val getRaidBattle: Method,
        val getInstanceRaidBoss: Method,
        val getTier: Method,
        val getStars: Method,
        val activeRaids: Field
    )

    private data class RaidEndMethods(
        val raidEndEventClass: Class<*>,
        val raidEndObservable: Any,
        val subscribeRaidEnd: Method,
        val raidEndPlayer: Method,
        val raidEndIsWin: Method
    )

    private val apiMethods: ApiMethods? by lazy {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return@lazy null

        runCatching {
            val raidAccessorClass = Class.forName(RAID_ACCESSOR_CLASS)
            val raidBattleClass = Class.forName(RAID_BATTLE_CLASS)
            val raidInstanceClass = Class.forName(RAID_INSTANCE_CLASS)
            val raidBossClass = Class.forName(RAID_BOSS_CLASS)
            val raidTierClass = Class.forName(RAID_TIER_CLASS)
            val raidHelperClass = Class.forName(RAID_HELPER_CLASS)

            ApiMethods(
                raidAccessorClass = raidAccessorClass,
                isRaidBoss = raidAccessorClass.getMethod("crd_isRaidBoss"),
                getEntityRaidBoss = raidAccessorClass.getMethod("crd_getRaidBoss"),
                getRaidId = raidAccessorClass.getMethod("crd_getRaidId"),
                raidBattleClass = raidBattleClass,
                isRaidBattle = raidBattleClass.getMethod("crd_isRaidBattle"),
                getRaidBattle = raidBattleClass.getMethod("crd_getRaidBattle"),
                getInstanceRaidBoss = raidInstanceClass.getMethod("getRaidBoss"),
                getTier = raidBossClass.getMethod("getTier"),
                getStars = raidTierClass.getMethod("getStars"),
                activeRaids = raidHelperClass.getField("ACTIVE_RAIDS")
            )
        }.getOrElse {
            logCompatibilityFailure(it)
            null
        }
    }


    private val raidEndMethods: RaidEndMethods? by lazy {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return@lazy null

        runCatching {
            val raidEventsClass = Class.forName(RAID_EVENTS_CLASS)
            val raidEndEventClass = Class.forName(RAID_END_EVENT_CLASS)
            val raidEndObservable = raidEventsClass.getField("RAID_END").get(null)

            RaidEndMethods(
                raidEndEventClass = raidEndEventClass,
                raidEndObservable = raidEndObservable,
                subscribeRaidEnd = raidEndObservable.javaClass.getMethod(
                    "subscribe",
                    Priority::class.java,
                    Consumer::class.java
                ),
                raidEndPlayer = raidEndEventClass.getMethod("player"),
                raidEndIsWin = raidEndEventClass.getMethod("isWin")
            )
        }.getOrElse {
            logRaidEndCompatibilityFailure(it)
            null
        }
    }

    fun registerRaidEndListener(callback: (ServerPlayerEntity, Boolean) -> Unit) {
        val methods = raidEndMethods ?: return

        runCatching {
            val consumer = Consumer<Any> { event ->
                if (methods.raidEndEventClass.isInstance(event)) {
                    val player = runCatching {
                        methods.raidEndPlayer.invoke(event) as? ServerPlayerEntity
                    }.getOrNull()
                    val won = runCatching {
                        methods.raidEndIsWin.invoke(event) as? Boolean ?: false
                    }.getOrDefault(false)

                    if (player != null) {
                        debug(
                            "[RaidDensCompat] raid end player=${player.name.string} win=$won"
                        )
                        callback(player, won)
                    }
                }
            }

            methods.subscribeRaidEnd.invoke(
                methods.raidEndObservable,
                Priority.NORMAL,
                consumer
            )
            debug("[RaidDensCompat] raid end listener registered")
        }.onFailure(::logRaidEndCompatibilityFailure)
    }

    fun isRaidBattle(battle: PokemonBattle): Boolean {
        val methods = apiMethods ?: return false
        if (!methods.raidBattleClass.isInstance(battle)) return false
        return runCatching {
            methods.isRaidBattle.invoke(battle) as? Boolean ?: false
        }.getOrDefault(false)
    }

    fun resolveTier(
        battle: PokemonBattle,
        opposingActors: List<BattleActor>
    ): String? {
        val methods = apiMethods ?: return null

        for (actor in opposingActors) {
            val entity = (actor as? EntityBackedBattleActor<*>)?.entity as? PokemonEntity ?: continue
            resolveFromEntity(methods, entity, "actor_entity")?.let { return it }
        }

        return resolveFromBattle(methods, battle)
    }

    private fun resolveFromEntity(
        methods: ApiMethods,
        entity: PokemonEntity,
        source: String
    ): String? {
        if (!methods.raidAccessorClass.isInstance(entity)) return null

        val species = entity.pokemon.species.name
        val isRaidBoss = runCatching { methods.isRaidBoss.invoke(entity) as? Boolean ?: false }
            .getOrDefault(false)
        val raidId = runCatching { methods.getRaidId.invoke(entity) as? UUID }.getOrNull()
        val directBoss = runCatching { methods.getEntityRaidBoss.invoke(entity) }.getOrNull()

        debug(
            "[RaidDensCompat] candidate source=$source species=$species " +
                "isRaidBoss=$isRaidBoss raidId=${raidId ?: "none"} directBoss=${directBoss != null}"
        )

        directBoss?.let { boss ->
            runCatching { resolveBossTier(methods, boss, source, species) }
                .getOrNull()
                ?.let { return it }
        }

        if (raidId != null) {
            val raidInstance = runCatching {
                val activeRaids = methods.activeRaids.get(null) as? Map<*, *>
                activeRaids?.get(raidId)
            }.getOrNull()

            if (raidInstance != null) {
                val raidBoss = runCatching { methods.getInstanceRaidBoss.invoke(raidInstance) }.getOrNull()
                if (raidBoss != null) {
                    runCatching {
                        resolveBossTier(methods, raidBoss, "${source}_active_raids", species)
                    }.getOrNull()?.let { return it }
                }
            }
        }

        return null
    }

    private fun resolveFromBattle(methods: ApiMethods, battle: PokemonBattle): String? {
        if (!methods.raidBattleClass.isInstance(battle)) return null

        return runCatching {
            val isRaid = methods.isRaidBattle.invoke(battle) as? Boolean ?: false
            debug("[RaidDensCompat] candidate source=battle isRaidBattle=$isRaid")
            if (!isRaid) return@runCatching null

            val raidInstance = methods.getRaidBattle.invoke(battle) ?: return@runCatching null
            val raidBoss = methods.getInstanceRaidBoss.invoke(raidInstance) ?: return@runCatching null
            resolveBossTier(methods, raidBoss, "battle", "unknown")
        }.getOrElse {
            debug("[RaidDensCompat] candidate source=battle failed=${messageOf(it)}")
            null
        }
    }

    private fun resolveBossTier(
        methods: ApiMethods,
        raidBoss: Any,
        source: String,
        species: String
    ): String? {
        val tier = methods.getTier.invoke(raidBoss) ?: return null
        val stars = runCatching { methods.getStars.invoke(tier) as? String }.getOrNull().orEmpty()
        val starCount = stars.length.takeIf { it in 1..7 }
        val mapped = starCount?.let(::mapStars)
            ?: mapTier((tier as? Enum<*>)?.name ?: tier.toString())
            ?: return null

        debug(
            "[RaidDensCompat] resolved source=$source stars=${stars.ifEmpty { "unknown" }} " +
                "tier=$mapped species=$species"
        )
        return mapped
    }

    private fun mapStars(stars: Int): String? = when (stars) {
        1 -> "UNCOMMON"
        2 -> "RARE"
        3, 4 -> "EPIC"
        5, 6 -> "LEGENDARY"
        7 -> "MYTHIC"
        else -> null
    }

    private fun mapTier(rawTier: String): String? = when (rawTier.uppercase()) {
        "TIER_ONE" -> "UNCOMMON"
        "TIER_TWO" -> "RARE"
        "TIER_THREE", "TIER_FOUR" -> "EPIC"
        "TIER_FIVE", "TIER_SIX" -> "LEGENDARY"
        "TIER_SEVEN" -> "MYTHIC"
        else -> null
    }

    private fun debug(message: String) {
        if (CobbleTunesServerConfig.current.debugLogging) {
            LOGGER.info("[cobbletunes] $message")
        }
    }

    private fun logCompatibilityFailure(error: Throwable) {
        if (compatibilityFailureLogged) return
        compatibilityFailureLogged = true
        LOGGER.warn(
            "[cobbletunes] Cobblemon Raid Dens integration disabled: {}",
            messageOf(error)
        )
    }

    private fun logRaidEndCompatibilityFailure(error: Throwable) {
        if (raidEndCompatibilityFailureLogged) return
        raidEndCompatibilityFailureLogged = true
        LOGGER.warn(
            "[cobbletunes] Cobblemon Raid Dens raid-end integration disabled: {}",
            messageOf(error)
        )
    }

    private fun messageOf(error: Throwable): String =
        error.cause?.message ?: error.message ?: error.javaClass.simpleName
}
