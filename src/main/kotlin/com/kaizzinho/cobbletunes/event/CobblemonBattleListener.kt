package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.battles.BattleSide
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.compat.rct.RawRctTrainer
import com.kaizzinho.cobbletunes.compat.rct.RctTrainerClassifier
import com.kaizzinho.cobbletunes.compat.rct.RctTrainerOverrides
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.BattleVictoryPayload
import com.kaizzinho.cobbletunes.network.PlayerDeathPayload
import com.kaizzinho.cobbletunes.network.PokemonCapturedPayload
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import com.kaizzinho.cobbletunes.network.CobbleTunesNetworking
import net.fabricmc.loader.api.FabricLoader
import java.lang.reflect.Modifier
import java.util.Locale

object CobblemonBattleListener {

    // Cobblemon emits one BATTLE_FAINTED event per faint. Use it only to arm a
    // short outcome watch: early Victory is sent after Showdown has declared the
    // winner and the complete server-side opposing roster is actually defeated.
    private data class PendingDecisiveFaintCheck(
        val battle: PokemonBattle,
        var ticksRemaining: Int = 40
    )

    private val pendingDecisiveFaintChecks = linkedMapOf<java.util.UUID, PendingDecisiveFaintCheck>()
    private val earlyVictorySentBattles = mutableSetOf<java.util.UUID>()

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register {
            val iterator = pendingDecisiveFaintChecks.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()
                val finished = sendEarlyVictoryIfDecided(pending.battle)
                pending.ticksRemaining--
                if (finished || pending.ticksRemaining <= 0) {
                    iterator.remove()
                }
            }
        }
        RaidDensBridge.registerRaidEndListener { player, won ->
            CobbleTunesNetworking.sendIfSupported(
                player,
                if (won) BattleVictoryPayload else BattleMusicEndPayload
            )

            if (CobbleTunesServerConfig.current.debugLogging) {
                LOGGER.info(
                    "[$MOD_ID] [Debug] [Raid end] player=${player.name.string} win=$won " +
                        "payload=${if (won) "victory" else "end"}"
                )
            }
        }

        CobblemonEvents.BATTLE_STARTED_POST.subscribe { event ->
            val battle = event.battle

            for (player in battle.players) {
                val playerActor = battle.getActor(player)
                if (playerActor == null) {
                    LOGGER.warn("[$MOD_ID] Could not resolve battle actor for ${player.name.string}, skipping")
                    continue
                }

                // In doubles/2v2/team battles, only actors on the opposite BattleSide
                // contribute to the opponent roster and regional/theme vote.
                val playerSide = sideForActor(battle, playerActor)
                if (playerSide == null) {
                    LOGGER.warn(
                        "[$MOD_ID] Could not resolve BattleSide for ${player.name.string}, skipping"
                    )
                    continue
                }
                val opposingSide = if (playerSide === battle.side1) battle.side2 else battle.side1
                val opposingActors: List<BattleActor> = opposingSide.actors.toList()

                val opposingPokemon = opposingActors
                    .flatMap { it.pokemonList }
                    .map { it.effectedPokemon }
                val opposingSpecies = opposingPokemon.map { it.species }

                val legendaryPokemon = opposingPokemon.firstOrNull { pokemon ->
                    "legendary" in pokemon.species.labels || "mythical" in pokemon.species.labels
                }
                val isLegendary = legendaryPokemon != null
                val primaryPokemon = legendaryPokemon ?: opposingPokemon.firstOrNull()
                val dexNumber = primaryPokemon?.species?.nationalPokedexNumber ?: -1
                val legendaryForm = legendaryPokemon?.form?.name.orEmpty()
                val opposingDexNumbers = opposingSpecies.map { it.nationalPokedexNumber }
                val opposingRegionalVariants = opposingPokemon.map { pokemon ->
                    resolveRegionalVariant(
                        pokemon.species.nationalPokedexNumber,
                        pokemon.form.name,
                        pokemon.aspects
                    )
                }
                val primaryRegionalVariant = primaryPokemon?.let { pokemon ->
                    resolveRegionalVariant(
                        pokemon.species.nationalPokedexNumber,
                        pokemon.form.name,
                        pokemon.aspects
                    )
                }.orEmpty()

                val raidTier = RaidDensBridge.resolveTier(battle, opposingActors)
                val bossTier = if (raidTier == null) WildBossesBridge.resolveTier(opposingActors) else null
                val trainerRoute = when {
                    raidTier != null -> "raid|${raidTier.lowercase()}"
                    bossTier != null -> "boss|${bossTier.lowercase()}"
                    battle.isPvN -> RctBridge.resolveTrainerRoute(battle, opposingActors)
                    else -> ""
                }

                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info(
                        "[$MOD_ID] [Debug] [Battle payload] player=${player.name.string} " +
                            "battle=${battle.battleId} isWild=${battle.isPvW} " +
                            "isTrainer=${battle.isPvN} raidTier=${raidTier ?: "none"} " +
                            "bossTier=${bossTier ?: "none"} route='$trainerRoute' " +
                            "form='${legendaryForm.ifEmpty { "base" }}' " +
                            "regional='${primaryRegionalVariant.ifEmpty { "standard" }}' " +
                            "opposingDex=$opposingDexNumbers regionalVariants=$opposingRegionalVariants"
                    )
                }

                CobbleTunesNetworking.sendIfSupported(
                    player,
                    BattleMusicStartPayload(
                        isWild = battle.isPvW,
                        isTrainer = battle.isPvN,
                        isLegendary = isLegendary,
                        dexNumber = dexNumber,
                        opposingDexNumbers = opposingDexNumbers,
                        opposingRegionalVariants = opposingRegionalVariants,
                        primaryRegionalVariant = primaryRegionalVariant,
                        legendaryForm = legendaryForm,
                        trainerTier = trainerRoute
                    )
                )
            }
        }

        CobblemonEvents.BATTLE_FAINTED.subscribe { event ->
            if (!RaidDensBridge.isRaidBattle(event.battle) && event.battle.battleId !in earlyVictorySentBattles) {
                pendingDecisiveFaintChecks[event.battle.battleId] =
                    PendingDecisiveFaintCheck(event.battle)
            }
        }

        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            val pokemon = event.pokemon
            val dexNumber = pokemon.species.nationalPokedexNumber
            val regionalVariant = resolveRegionalVariant(
                dexNumber,
                pokemon.form.name,
                pokemon.aspects
            )

            CobbleTunesNetworking.sendIfSupported(
                event.player,
                PokemonCapturedPayload(
                    dexNumber = dexNumber,
                    regionalVariant = regionalVariant
                )
            )

            if (CobbleTunesServerConfig.current.debugLogging) {
                LOGGER.info(
                    "[$MOD_ID] [Debug] [Capture victory] player=${event.player.name.string} " +
                        "dex=$dexNumber regional='${regionalVariant.ifEmpty { "standard" }}'"
                )
            }
        }

        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            pendingDecisiveFaintChecks.remove(event.battle.battleId)
            val earlyVictoryAlreadySent = earlyVictorySentBattles.remove(event.battle.battleId)

            if (RaidDensBridge.isRaidBattle(event.battle)) {
                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info(
                        "[$MOD_ID] [Debug] [Raid end] Ignoring Cobblemon BATTLE_VICTORY " +
                            "because Raid Dens RAID_END owns completion"
                    )
                }
            } else if (!earlyVictoryAlreadySent) {
                val winnerActors = event.winners.toSet()
                for (player in event.battle.players) {
                    val playerActor = event.battle.getActor(player)
                    val actuallyWon = !event.wasWildCapture && playerActor in winnerActors
                    CobbleTunesNetworking.sendIfSupported(
                        player,
                        if (actuallyWon) BattleVictoryPayload else BattleMusicEndPayload
                    )
                }
            } else if (CobbleTunesServerConfig.current.debugLogging) {
                LOGGER.info(
                    "[$MOD_ID] [Debug] [Victory] Ignoring later Cobblemon BATTLE_VICTORY " +
                        "because decisive-faint Victory was already sent for battle=${event.battle.battleId}"
                )
            }
        }

        CobblemonEvents.BATTLE_FLED.subscribe { event ->
            pendingDecisiveFaintChecks.remove(event.battle.battleId)
            earlyVictorySentBattles.remove(event.battle.battleId)
            for (player in event.battle.players) {
                CobbleTunesNetworking.sendIfSupported(player, BattleMusicEndPayload)
            }
        }

        // defeat uses the real respawn flag
        ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer, alive ->
            if (!alive) {
                CobbleTunesNetworking.sendIfSupported(newPlayer, PlayerDeathPayload)
                if (CobbleTunesServerConfig.current.debugLogging) {
                    LOGGER.info("[$MOD_ID] [Debug] Player ${newPlayer.name.string} died — sending PlayerDeathPayload")
                }
            }
        }

        LOGGER.info("[$MOD_ID] CobblemonBattleListener registered (server-side battle classification).")
    }

    private fun sendEarlyVictoryIfDecided(battle: PokemonBattle): Boolean {
        if (battle.battleId in earlyVictorySentBattles) return true
        if (RaidDensBridge.isRaidBattle(battle)) return true

        // The Showdown interpreter receives the authoritative `win` instruction before
        // Cobblemon finishes its visual dispatch queue. Requiring that declaration keeps
        // early Victory safe for spread moves, recoil, Explosion/double-KO resolutions,
        // PvP, doubles and 2v2 battles. If `win` is not available yet, do nothing and let
        // Cobblemon's normal BATTLE_VICTORY event remain the final authority.
        val declaredWinners = showdownWinnerActorUuids(battle) ?: return false

        val declaredWinningPlayers = battle.players.filter { player ->
            battle.getActor(player)?.uuid?.let { it in declaredWinners } == true
        }
        if (declaredWinningPlayers.isEmpty()) return true

        val winningPlayers = declaredWinningPlayers.filter { player ->
            val actor = battle.getActor(player) ?: return@filter false

            // Also verify the complete server-side opposing roster. A forfeit may produce
            // a winner while healthy opponents remain; that case is intentionally left to
            // the official BATTLE_VICTORY event instead of pretending it was a final faint.
            val playerSide = sideForActor(battle, actor) ?: return@filter false
            val opposingSide = if (playerSide === battle.side1) battle.side2 else battle.side1
            sideIsCompletelyDefeated(opposingSide)
        }
        if (winningPlayers.isEmpty()) return false

        earlyVictorySentBattles += battle.battleId
        for (player in battle.players) {
            CobbleTunesNetworking.sendIfSupported(
                player,
                if (player in winningPlayers) BattleVictoryPayload else BattleMusicEndPayload
            )
        }

        if (CobbleTunesServerConfig.current.debugLogging) {
            LOGGER.info(
                "[$MOD_ID] [Debug] [Victory] decisive side defeat battle=${battle.battleId} " +
                    "winnerPlayers=${winningPlayers.joinToString { it.name.string }} " +
                    "source=server-showdown-win+full-roster"
            )
        }
        return true
    }

    private fun showdownWinnerActorUuids(battle: PokemonBattle): Set<java.util.UUID>? {
        for (rawUpdate in battle.showdownMessages.asReversed()) {
            for (line in rawUpdate.lineSequence().toList().asReversed()) {
                if (line.isBlank()) continue
                val message = BattleMessage(line)
                if (message.id.replace("|", "").trim() != "win") continue

                val rawWinners = message.argumentAt(0) ?: return emptySet()
                return rawWinners.split('&')
                    .mapNotNull { value ->
                        runCatching { java.util.UUID.fromString(value.trim()) }.getOrNull()
                    }
                    .toSet()
            }
        }
        return null
    }

    private fun sideForActor(battle: PokemonBattle, actor: BattleActor): BattleSide? {
        return when {
            battle.side1.actors.any { it.uuid == actor.uuid } -> battle.side1
            battle.side2.actors.any { it.uuid == actor.uuid } -> battle.side2
            else -> null
        }
    }

    private fun sideIsCompletelyDefeated(side: BattleSide): Boolean {
        val roster = side.actors.flatMap { it.pokemonList }
        return roster.isNotEmpty() && roster.all { it.health <= 0 }
    }

    private fun resolveRegionalVariant(
        dexNumber: Int,
        formName: String,
        aspects: Set<String>
    ): String {
        val candidates = aspects + formName
        for (candidate in candidates) {
            val normalized = candidate.trim().lowercase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')

            when {
                "alola" in normalized || "alolan" in normalized -> return "alola"
                "galar" in normalized || "galarian" in normalized -> return "galar"
                "hisui" in normalized || "hisuian" in normalized -> return "hisui"
                "paldea" in normalized || "paldean" in normalized -> return "paldea"
                dexNumber == 550 && "white-striped" in normalized -> return "hisui"
            }
        }
        return ""
    }

    private object RctBridge {
        private const val RCT_MOD_ID = "rctmod"
        private const val RCT_MOD_CLASS = "com.gitlab.srcmc.rctmod.api.RCTMod"

        private val specialTrainerOverrides = RctTrainerOverrides.exact

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
                    val result = RctTrainerClassifier.classify(
                        probe.raw,
                        exactOverrides = specialTrainerOverrides
                    )
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
                                "battleTrack='${result.battleTrackId.orEmpty()}' " +
                                "battlePool='${result.battlePoolId.orEmpty()}' " +
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

            // rct string lookup first entity lookup fallback
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
