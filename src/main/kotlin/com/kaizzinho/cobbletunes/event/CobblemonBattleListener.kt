package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.compat.rct.RawRctTrainer
import com.kaizzinho.cobbletunes.compat.rct.RctTrainerClassifier
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.PlayerDeathPayload
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import java.lang.reflect.Modifier
import java.util.Locale

object CobblemonBattleListener {

    fun register() {
        CobblemonEvents.BATTLE_STARTED_POST.subscribe { event ->
            val battle = event.battle

            for (player in battle.players) {
                val playerActor = battle.getActor(player)
                if (playerActor == null) {
                    LOGGER.warn("[$MOD_ID] Could not resolve battle actor for ${player.name.string}, skipping")
                    continue
                }

                // side isn't exposed by these mappings, so grab every actor except us
                val opposingActors: List<BattleActor> = battle.actors
                    .filter { it != playerActor }
                    .toList()

                val opposingSpecies = opposingActors
                    .flatMap { it.pokemonList }
                    .map { it.effectedPokemon.species }

                val legendarySpecies = opposingSpecies.firstOrNull { species ->
                    "legendary" in species.labels || "mythical" in species.labels
                }
                val isLegendary = legendarySpecies != null
                val primarySpecies = legendarySpecies ?: opposingSpecies.firstOrNull()
                val dexNumber = primarySpecies?.nationalPokedexNumber ?: -1
                val opposingDexNumbers = opposingSpecies.map { it.nationalPokedexNumber }

                val trainerRoute = if (battle.isPvN) {
                    RctBridge.resolveTrainerRoute(battle, opposingActors)
                } else {
                    ""
                }

                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info(
                        "[$MOD_ID] [Debug] [Battle payload] player=${player.name.string} " +
                            "battle=${battle.battleId} isWild=${battle.isPvW} " +
                            "isTrainer=${battle.isPvN} route='$trainerRoute' " +
                            "opposingDex=$opposingDexNumbers"
                    )
                }

                ServerPlayNetworking.send(
                    player,
                    BattleMusicStartPayload(
                        isWild = battle.isPvW,
                        isTrainer = battle.isPvN,
                        isLegendary = isLegendary,
                        dexNumber = dexNumber,
                        opposingDexNumbers = opposingDexNumbers,
                        trainerTier = trainerRoute
                    )
                )
            }
        }

        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            for (player in event.battle.players) {
                ServerPlayNetworking.send(player, BattleMusicEndPayload)
            }
        }

        CobblemonEvents.BATTLE_FLED.subscribe { event ->
            for (player in event.battle.players) {
                ServerPlayNetworking.send(player, BattleMusicEndPayload)
            }
        }

        // cobblemon has no defeat event, so use the real respawn flag instead
        ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer, alive ->
            if (!alive) {
                ServerPlayNetworking.send(newPlayer, PlayerDeathPayload)
                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info("[$MOD_ID] [Debug] Player ${newPlayer.name.string} died — sending PlayerDeathPayload")
                }
            }
        }

        LOGGER.info("[$MOD_ID] CobblemonBattleListener registered (server-side battle classification).")
    }

    private object RctBridge {
        private const val RCT_MOD_ID = "rctmod"
        private const val RCT_MOD_CLASS = "com.gitlab.srcmc.rctmod.api.RCTMod"

        private val rctAvailable by lazy {
            FabricLoader.getInstance().isModLoaded(RCT_MOD_ID)
        }

        private var compatibilityFailureLogged = false

        private data class TrainerProbe(
            val raw: RawRctTrainer,
            val dataFound: Boolean
        )

        fun resolveTrainerRoute(
            battle: PokemonBattle,
            opposingActors: List<BattleActor>
        ): String {
            if (!rctAvailable) return ""

            return try {
                for (actor in opposingActors) {
                    val entity = (actor as? EntityBackedBattleActor<*>)?.entity ?: continue
                    val trainerId = invokeNoArg(entity, "getTrainerId") as? String ?: continue
                    if (trainerId.isBlank()) continue

                    val probe = readTrainerData(entity, trainerId)
                    val result = RctTrainerClassifier.classify(probe.raw)
                    val route = result.route()

                    if (CobbleTunesServerConfig.current.debugLogging) {
                        val source = result.source.name.lowercase(Locale.ROOT).replace('_', '-')
                        LOGGER.info(
                            "[$MOD_ID] [Debug] [RCT] battle=${battle.battleId} " +
                                "actor=${actor.javaClass.name} entity=${entity.javaClass.name} " +
                                "trainerId='${result.trainerId}' dataFound=${probe.dataFound} " +
                                "rawType='${result.rawType.ifEmpty { "missing" }}' " +
                                "optional=${result.optional} role='${result.role.routeId}' " +
                                "faction='${result.faction?.id.orEmpty()}' " +
                                "rank='${result.factionRank?.name?.lowercase(Locale.ROOT).orEmpty()}' " +
                                "theme='${result.factionTheme.orEmpty()}' " +
                                "region='${result.region ?: "unknown"}' source=$source route='$route'"
                        )
                    }
                    return route
                }

                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info(
                        "[$MOD_ID] [Debug] [RCT] No RCT trainer actor in battle " +
                            "${battle.battleId}; using regular trainer music"
                    )
                }
                ""
            } catch (e: Exception) {
                logCompatibilityFailure(e)
                ""
            } catch (e: LinkageError) {
                logCompatibilityFailure(e)
                ""
            }
        }

        private fun readTrainerData(entity: Any, trainerId: String): TrainerProbe {
            val missing = TrainerProbe(
                raw = RawRctTrainer(trainerId, "", optional = false),
                dataFound = false
            )

            val rctModClass = Class.forName(RCT_MOD_CLASS, false, entity.javaClass.classLoader)
            val getInstance = rctModClass.methods.firstOrNull { method ->
                method.name == "getInstance" &&
                    method.parameterCount == 0 &&
                    Modifier.isStatic(method.modifiers)
            } ?: return missing

            val rctMod = getInstance.invoke(null) ?: return missing
            val trainerManager = invokeNoArg(rctMod, "getTrainerManager") ?: return missing

            // string lookup is the clean path in rct 0.18.1; entity lookup is a fallback
            val trainerData = invokeOneArg(trainerManager, "getData", trainerId)
                ?: invokeOneArg(trainerManager, "getData", entity)
                ?: return missing

            val type = invokeNoArg(trainerData, "getType")
            val typeId = when (type) {
                is String -> type
                null -> ""
                else -> (invokeNoArg(type, "id") ?: invokeNoArg(type, "getId"))?.toString().orEmpty()
            }.trim().lowercase(Locale.ROOT)

            val optional = invokeNoArg(trainerData, "isOptional") as? Boolean ?: false

            return TrainerProbe(
                raw = RawRctTrainer(
                    trainerId = trainerId,
                    typeId = typeId,
                    optional = optional
                ),
                dataFound = true
            )
        }

        private fun invokeNoArg(target: Any, methodName: String): Any? {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterCount == 0
            } ?: return null
            return method.invoke(target)
        }

        private fun invokeOneArg(target: Any, methodName: String, argument: Any): Any? {
            val candidates = target.javaClass.methods.filter {
                it.name == methodName && it.parameterCount == 1
            }

            val method = candidates.firstOrNull {
                it.parameterTypes[0] == argument.javaClass
            } ?: candidates.firstOrNull {
                it.parameterTypes[0].isAssignableFrom(argument.javaClass)
            } ?: return null

            return method.invoke(target, argument)
        }

        private fun logCompatibilityFailure(error: Throwable) {
            if (compatibilityFailureLogged) return
            compatibilityFailureLogged = true
            LOGGER.warn(
                "[$MOD_ID] [RCT] Optional integration is incompatible or unavailable; " +
                    "RCT battles will fall back to ordinary trainer music",
                error
            )
        }
    }
}
