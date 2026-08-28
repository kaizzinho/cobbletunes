package com.kaizzinho.cobbletunes.client.compat.standalone

import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattleActor
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.kaizzinho.cobbletunes.compat.rct.RawRctTrainer
import com.kaizzinho.cobbletunes.compat.rct.RctTrainerClassifier
import com.kaizzinho.cobbletunes.compat.rct.RctTrainerOverrides
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.ClientBridgeProbePayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.SoundInstanceListener
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.UUID
import kotlin.math.pow

class ClientStandaloneBridge(
    private val debugLog: (String) -> Unit,
    private val onBattleStart: (BattleMusicStartPayload) -> Unit,
    private val onBattleEnd: () -> Unit,
    private val onBattleVictory: () -> Unit,
    private val shouldUseEarlyFaintVictory: () -> Boolean,
    private val onCapture: (Int, String) -> Unit
) {
    companion object {
        private const val BATTLE_START_DELAY_TICKS = 6
        private const val POKEMON_SNAPSHOT_INTERVAL_TICKS = 5
        private const val POKEMON_SNAPSHOT_LIFETIME_MILLIS = 5_000L

        private val RAID_SOUND_TIERS = mapOf(
            "cobblemonraiddens:battle.raid.tier_one" to "uncommon",
            "cobblemonraiddens:battle.raid.tier_two" to "rare",
            "cobblemonraiddens:battle.raid.tier_three" to "epic",
            "cobblemonraiddens:battle.raid.tier_four" to "epic",
            "cobblemonraiddens:battle.raid.tier_five" to "legendary",
            "cobblemonraiddens:battle.raid.tier_six" to "legendary",
            "cobblemonraiddens:battle.raid.tier_seven" to "mythic"
        )

        private val SPECIAL_DISPLAY_IDS = linkedMapOf(
            "barry" to "pokemon_trainer_barry",
            "silver" to "pokemon_trainer_silver",
            "steven" to "pokemon_trainer_steven",
            "morimoto" to "pokemon_trainer_morimoto",
            "professor oak" to "pokemon_trainer_oak",
            "oak" to "pokemon_trainer_oak",
            "pokemon trainer red" to "pokemon_trainer_red",
            "trainer red" to "pokemon_trainer_red",
            "pokemon trainer gold" to "pokemon_trainer_gold",
            "trainer gold" to "pokemon_trainer_gold",
            "pokemon trainer green" to "pokemon_trainer_green",
            "trainer green" to "pokemon_trainer_green",
            "pokemon trainer kris" to "pokemon_trainer_kris",
            "trainer kris" to "pokemon_trainer_kris",
            "pokemon trainer may" to "pokemon_trainer_may",
            "trainer may" to "pokemon_trainer_may"
        )
    }

    private data class PokemonSnapshot(
        val uuid: UUID?,
        val dexNumber: Int,
        val formName: String,
        val aspects: Set<String>,
        val legendary: Boolean,
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val seenAtMillis: Long = System.currentTimeMillis()
    ) {
        val regionalVariant: String
            get() = resolveRegionalVariant(dexNumber, formName, aspects)
    }

    private var currentBattleId: UUID? = null
    private var observedBattleId: UUID? = null
    private var earlyVictoryTriggered = false
    private var battleStartDelay = 0
    private var currentPayload: BattleMusicStartPayload? = null
    private var pendingRaidTier: String? = null
    private var opponentFaintedAtLastTick = false
    private var playerFaintedAtLastTick = false
    private var raidVictoryTriggered = false
    private var recentCaptureAtMillis = 0L
    private var pokemonSnapshotCounter = 0
    private var lastBridgeAvailable = false
    private var soundListenerRegistered = false
    private val nearbyPokemon = mutableListOf<PokemonSnapshot>()
    private val handledCaptureBalls = mutableSetOf<UUID>()
    private val raidSoundListener = SoundInstanceListener { sound, _, _ ->
        val client = MinecraftClient.getInstance()
        client.execute {
            handleSound(
                sound.id.toString(),
                sound.x,
                sound.y,
                sound.z
            )
        }
    }

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { tickClient ->
            registerSoundListenerIfReady(tickClient)
            tick(tickClient)
        }
    }

    private fun registerSoundListenerIfReady(client: MinecraftClient) {
        if (soundListenerRegistered) return

        val registered = runCatching {
            client.soundManager.registerListener(raidSoundListener)
            true
        }.getOrDefault(false)

        if (registered) {
            soundListenerRegistered = true
            debugLog("client standalone sound listener registered")
        }
    }

    fun serverBridgeAvailable(): Boolean = runCatching {
        ClientPlayNetworking.canSend(ClientBridgeProbePayload.ID)
    }.getOrDefault(false)

    private fun tick(client: MinecraftClient) {
        val bridgeAvailable = serverBridgeAvailable()
        if (bridgeAvailable != lastBridgeAvailable) {
            lastBridgeAvailable = bridgeAvailable
            debugLog(
                "[Standalone] mode=${if (bridgeAvailable) "server-bridge" else "client-only"}"
            )
            if (bridgeAvailable) {
                resetLocalState()
            }
        }

        val world = client.world
        val player = client.player
        if (world == null || player == null) {
            resetLocalState()
            return
        }

        observeBattleOutcome(client, bridgeAvailable)
        if (bridgeAvailable) return

        pokemonSnapshotCounter++
        if (pokemonSnapshotCounter >= POKEMON_SNAPSHOT_INTERVAL_TICKS) {
            pokemonSnapshotCounter = 0
            refreshNearbyPokemon(client)
        }

        updateBattle(client)
        updateCaptureState(client)

    }

    private fun observeBattleOutcome(client: MinecraftClient, bridgeAvailable: Boolean) {
        // When the server bridge is present, the server has the complete hidden rosters
        // and owns decisive-faint Victory. Never compete with that authoritative path.
        if (bridgeAvailable) {
            observedBattleId = null
            earlyVictoryTriggered = false
            return
        }

        val battle = CobblemonClient.battle
        if (battle == null) {
            observedBattleId = null
            earlyVictoryTriggered = false
            return
        }

        if (observedBattleId == battle.battleId) return

        observedBattleId = battle.battleId
        earlyVictoryTriggered = false

        // Cobblemon intentionally does not synchronize an opponent trainer/PvP player's
        // hidden reserve roster to the client. The battle-log queue is still useful as a
        // synchronization point for the currently active Pokémon, but only a normal wild
        // side is safe to resolve early from those visible actives. Trainer/PvP Victory
        // waits for the actual battle end in client-only mode.
        battle.messages.subscribe {
            client.execute {
                observeVisibleFaintState(client, battle.battleId, allowEarlyWildVictory = true)
            }
        }

        debugLog("[Victory] armed client battle outcome observer battle=${battle.battleId}")
    }

    private fun observeVisibleFaintState(
        client: MinecraftClient,
        battleId: UUID,
        allowEarlyWildVictory: Boolean
    ) {
        if (observedBattleId != battleId) return

        val battle = CobblemonClient.battle ?: return
        if (battle.battleId != battleId) return

        val playerUuid = client.player?.uuid ?: return
        val playerSide = battle.sides.firstOrNull { side ->
            side.actors.any { it.uuid == playerUuid }
        } ?: return
        val opposingSide = battle.sides.firstOrNull { it !== playerSide } ?: return

        // Keep the latest visible faint state for the client-only battle-end fallback.
        // This also catches the final faint even when BattleEndPacket clears the battle
        // before the next regular client tick.
        opponentFaintedAtLastTick = sideIsFainted(opposingSide.actors)
        playerFaintedAtLastTick = sideIsFainted(playerSide.actors)

        if (!allowEarlyWildVictory || earlyVictoryTriggered) return
        if (!shouldUseEarlyFaintVictory()) return
        if (pendingRaidTier != null || currentPayload?.trainerTier?.startsWith("raid|") == true) return

        val ordinaryWildSide = opposingSide.actors.isNotEmpty() &&
            opposingSide.actors.all { it.type == ActorType.WILD }
        if (!ordinaryWildSide) return
        if (!opponentFaintedAtLastTick || playerFaintedAtLastTick) return

        earlyVictoryTriggered = true
        debugLog("[Victory] all visible wild opponents fainted; starting client-only Victory")
        onBattleVictory()
    }

    private fun updateBattle(client: MinecraftClient) {
        val battle = CobblemonClient.battle
        if (battle == null) {
            if (currentBattleId != null) finishBattle()
            return
        }

        val playerUuid = client.player?.uuid ?: return
        val playerSide = battle.sides.firstOrNull { side ->
            side.actors.any { it.uuid == playerUuid }
        } ?: return
        val opposingSide = battle.sides.firstOrNull { it !== playerSide } ?: return

        val battleId = battle.battleId
        if (battleId != currentBattleId) {
            currentBattleId = battleId
            battleStartDelay = BATTLE_START_DELAY_TICKS
            currentPayload = null
            pendingRaidTier = null
            opponentFaintedAtLastTick = false
            playerFaintedAtLastTick = false
            raidVictoryTriggered = false
            debugLog("[Standalone battle] detected battle=$battleId waiting for synced roster")
        }

        opponentFaintedAtLastTick = sideIsFainted(opposingSide.actors)
        playerFaintedAtLastTick = sideIsFainted(playerSide.actors)

        if (
            !raidVictoryTriggered &&
            currentPayload?.trainerTier?.startsWith("raid|") == true &&
            opponentFaintedAtLastTick &&
            !playerFaintedAtLastTick
        ) {
            raidVictoryTriggered = true
            debugLog("[Standalone raid] boss fainted triggering victory")
            onBattleVictory()
        }

        if (currentPayload == null) {
            if (battleStartDelay > 0) {
                battleStartDelay--
                return
            }

            buildBattlePayload(client, opposingSide.actors)?.let { payload ->
                currentPayload = payload
                onBattleStart(payload)
            }
            return
        }

        // late metadata can promote a normal route without restarting battle state
        if (pendingRaidTier != null && currentPayload?.trainerTier?.startsWith("raid|") != true) {
            promoteRaidRoute()
            return
        }

        if (currentPayload?.trainerTier.isNullOrBlank()) {
            val wildBossTier = resolveWildBossTier(client, opposingSide.actors)
            if (wildBossTier != null) {
                val promoted = currentPayload!!.copy(
                    trainerTier = "boss|${wildBossTier.lowercase(Locale.ROOT)}"
                )
                currentPayload = promoted
                debugLog("[Standalone battle] promoted route='${promoted.trainerTier}'")
                onBattleStart(promoted)
            }
        }
    }

    private fun buildBattlePayload(
        client: MinecraftClient,
        opposingActors: List<ClientBattleActor>
    ): BattleMusicStartPayload? {
        val snapshots = opposingActors.flatMap(::pokemonSnapshots)
        if (snapshots.isEmpty()) {
            battleStartDelay = 2
            return null
        }

        val isWild = opposingActors.isNotEmpty() && opposingActors.all { it.type == ActorType.WILD }
        val isTrainer = opposingActors.any { it.type == ActorType.NPC }
        val isPvp = opposingActors.any { it.type == ActorType.PLAYER }
        val legendary = snapshots.firstOrNull { it.legendary }
        val primary = legendary ?: snapshots.first()

        val raidTier = pendingRaidTier
        val bossTier = if (raidTier == null) resolveWildBossTier(client, opposingActors) else null
        val trainerRoute = when {
            raidTier != null -> "raid|$raidTier"
            bossTier != null -> "boss|${bossTier.lowercase(Locale.ROOT)}"
            isTrainer -> resolveTrainerRoute(client, opposingActors, snapshots)
            else -> ""
        }

        val payload = BattleMusicStartPayload(
            isWild = isWild,
            isTrainer = isTrainer,
            isLegendary = legendary != null,
            dexNumber = primary.dexNumber,
            opposingDexNumbers = snapshots.map { it.dexNumber },
            opposingRegionalVariants = snapshots.map { it.regionalVariant },
            primaryRegionalVariant = primary.regionalVariant,
            legendaryForm = legendary?.formName.orEmpty(),
            trainerTier = trainerRoute
        )

        debugLog(
            "[Standalone battle] wild=$isWild trainer=$isTrainer pvp=$isPvp " +
                "route='${trainerRoute}' opposingDex=${payload.opposingDexNumbers}"
        )
        return payload
    }

    private fun finishBattle() {
        val wasRaid = currentPayload?.trainerTier?.startsWith("raid|") == true
        val capturedRecently = System.currentTimeMillis() - recentCaptureAtMillis < 2_500L
        val won = opponentFaintedAtLastTick && !playerFaintedAtLastTick

        debugLog(
            "[Standalone battle] ended raid=$wasRaid won=$won captured=$capturedRecently"
        )

        when {
            capturedRecently -> Unit
            raidVictoryTriggered -> Unit
            won -> onBattleVictory()
            else -> onBattleEnd()
        }

        currentBattleId = null
        battleStartDelay = 0
        currentPayload = null
        pendingRaidTier = null
        opponentFaintedAtLastTick = false
        playerFaintedAtLastTick = false
        raidVictoryTriggered = false
    }

    private fun sideIsFainted(actors: List<ClientBattleActor>): Boolean {
        val active = actors.flatMap { actor -> actor.activePokemon.mapNotNull { it.battlePokemon } }
        if (active.isEmpty()) return false
        return active.all { pokemon -> hpRatio(pokemon) <= 0.0001f }
    }

    private fun hpRatio(pokemon: ClientBattlePokemon): Float =
        if (pokemon.isHpFlat) {
            if (pokemon.maxHp > 0f) pokemon.hpValue / pokemon.maxHp else 1f
        } else {
            pokemon.hpValue
        }

    private fun pokemonSnapshots(actor: ClientBattleActor): List<PokemonSnapshot> {
        if (actor.pokemon.isNotEmpty()) {
            return actor.pokemon.map(::snapshotOf)
        }

        return actor.activePokemon.mapNotNull { active ->
            active.battlePokemon?.let(::snapshotOf)
        }
    }

    private fun snapshotOf(pokemon: Pokemon): PokemonSnapshot = PokemonSnapshot(
        uuid = pokemon.uuid,
        dexNumber = pokemon.species.nationalPokedexNumber,
        formName = pokemon.form.name,
        aspects = pokemon.aspects,
        legendary = "legendary" in pokemon.species.labels || "mythical" in pokemon.species.labels
    )

    private fun snapshotOf(pokemon: ClientBattlePokemon): PokemonSnapshot {
        val aspects = pokemon.properties.aspects + pokemon.state.currentAspects
        return PokemonSnapshot(
            uuid = pokemon.uuid,
            dexNumber = pokemon.species.nationalPokedexNumber,
            formName = pokemon.properties.form.orEmpty(),
            aspects = aspects,
            legendary = "legendary" in pokemon.species.labels || "mythical" in pokemon.species.labels
        )
    }

    private fun resolveWildBossTier(
        client: MinecraftClient,
        opposingActors: List<ClientBattleActor>
    ): String? {
        if (!FabricLoader.getInstance().isModLoaded("wildbosses")) return null

        return runCatching {
            val api = Class.forName("com.kaizzinho.wildbosses.api.WildBossIntegrationApi")
            val byUuid = api.methods.firstOrNull {
                it.name == "getTierNameByPokemonUuid" && it.parameterCount == 1
            }
            val byEntity = api.methods.firstOrNull {
                it.name == "getTierName" && it.parameterCount == 1
            }

            val uuids = opposingActors
                .flatMap { actor -> actor.activePokemon.mapNotNull { it.battlePokemon?.uuid } }

            for (uuid in uuids) {
                val tier = byUuid?.invoke(null, uuid) as? String
                if (!tier.isNullOrBlank()) return@runCatching tier.uppercase(Locale.ROOT)
            }

            val player = client.player ?: return@runCatching null
            val world = client.world ?: return@runCatching null
            val entities = world.getEntitiesByClass(
                PokemonEntity::class.java,
                player.boundingBox.expand(96.0)
            ) { true }
            for (entity in entities) {
                if (entity.pokemon.uuid !in uuids) continue
                val tier = byEntity?.invoke(null, entity) as? String
                if (!tier.isNullOrBlank()) return@runCatching tier.uppercase(Locale.ROOT)
            }
            null
        }.getOrNull()
    }

    private fun resolveTrainerRoute(
        client: MinecraftClient,
        actors: List<ClientBattleActor>,
        snapshots: List<PokemonSnapshot>
    ): String {
        val exact = findRctTrainer(client, actors)?.let { (trainerId, typeId) ->
            val normalizedId = trainerId.substringAfter(':').lowercase(Locale.ROOT)
            RctTrainerClassifier.classify(
                RawRctTrainer(trainerId, typeId, optional = false),
                exactOverrides = RctTrainerOverrides.exact
            ).route()
        }
        if (!exact.isNullOrBlank()) return exact

        val displayName = actors.firstOrNull { it.type == ActorType.NPC }
            ?.displayName?.string.orEmpty()
        val normalized = normalize(displayName)

        val specialId = SPECIAL_DISPLAY_IDS.entries.firstOrNull { (name, _) ->
            normalized.contains(name)
        }?.value
        if (specialId != null) {
            return RctTrainerClassifier.classify(
                RawRctTrainer(specialId, "", optional = false),
                exactOverrides = RctTrainerOverrides.exact
            ).route()
        }

        val inferredRole = when {
            "champion" in normalized -> "champ"
            "elite four" in normalized || "elite 4" in normalized -> "e4"
            "gym leader" in normalized || normalized.startsWith("leader ") -> "leader"
            "rival" in normalized -> "rival"
            "frontier brain" in normalized -> "frontier"
            else -> ""
        }
        if (inferredRole.isNotBlank()) return inferredRole

        val faction = when {
            "team rocket" in normalized -> "team_rocket"
            "team aqua" in normalized -> "team_aqua"
            "team magma" in normalized -> "team_magma"
            "team galactic" in normalized -> "team_galactic"
            "team plasma" in normalized -> "team_plasma"
            "team flare" in normalized -> "team_flare"
            "team skull" in normalized -> "team_skull"
            "aether" in normalized -> "aether_foundation"
            "ultra recon" in normalized -> "ultra_recon_squad"
            else -> ""
        }
        if (faction.isNotBlank()) return "faction:$faction"

        // empty route keeps normal trainer music with roster region voting
        return ""
    }

    private fun findRctTrainer(
        client: MinecraftClient,
        actors: List<ClientBattleActor>
    ): Pair<String, String>? {
        if (!FabricLoader.getInstance().isModLoaded("rctmod")) return null
        val player = client.player ?: return null
        val world = client.world ?: return null
        val npcUuids = actors
            .filter { it.type == ActorType.NPC }
            .map { it.uuid }
            .toSet()

        val candidates = world.getOtherEntities(
            player,
            player.boundingBox.expand(64.0)
        ) { entity ->
            invokeNoArg(entity, "getTrainerId") is String
        }

        for (entity in candidates.sortedBy {
            val actorPenalty = if (it.uuid in npcUuids) 0.0 else 1_000_000.0
            actorPenalty + it.squaredDistanceTo(player)
        }) {
            val trainerId = (invokeNoArg(entity, "getTrainerId") as? String)
                ?.takeIf { it.isNotBlank() }
                ?: continue
            return trainerId to readRctType(entity, trainerId)
        }
        return null
    }

    private fun readRctType(entity: Any, trainerId: String): String = runCatching {
        val rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod", false, entity.javaClass.classLoader)
        val getInstance = rctModClass.methods.firstOrNull { method ->
            method.name == "getInstance" && method.parameterCount == 0 && Modifier.isStatic(method.modifiers)
        } ?: return@runCatching ""
        val rctMod = getInstance.invoke(null) ?: return@runCatching ""
        val trainerManager = invokeNoArg(rctMod, "getTrainerManager") ?: return@runCatching ""
        val trainerData = invokeOneArg(trainerManager, "getData", trainerId)
            ?: invokeOneArg(trainerManager, "getData", entity)
            ?: return@runCatching ""
        val type = invokeNoArg(trainerData, "getType") ?: return@runCatching ""
        when (type) {
            is String -> type
            else -> (invokeNoArg(type, "id") ?: invokeNoArg(type, "getId"))?.toString().orEmpty()
        }
    }.getOrDefault("")

    private fun handleSound(soundId: String, x: Double, y: Double, z: Double) {
        if (serverBridgeAvailable()) return

        val raidTier = RAID_SOUND_TIERS[soundId]
        if (raidTier != null) {
            pendingRaidTier = raidTier
            debugLog("[Standalone raid] native sound=$soundId tier=$raidTier")
            promoteRaidRoute()
        }
    }

    private fun promoteRaidRoute() {
        val tier = pendingRaidTier ?: return
        val payload = currentPayload ?: return
        if (payload.trainerTier == "raid|$tier") return

        val promoted = payload.copy(trainerTier = "raid|$tier")
        currentPayload = promoted
        debugLog("[Standalone raid] promoted route='${promoted.trainerTier}'")
        onBattleStart(promoted)
    }

    private fun updateCaptureState(client: MinecraftClient) {
        val player = client.player ?: return
        val world = client.world ?: return
        val balls = world.getEntitiesByClass(
            EmptyPokeBallEntity::class.java,
            player.boundingBox.expand(72.0)
        ) { ball ->
            ball.captureState == EmptyPokeBallEntity.CaptureState.CAPTURED &&
                ball.owner?.uuid == player.uuid
        }

        for (ball in balls) {
            if (!handledCaptureBalls.add(ball.uuid)) continue
            handleLocalCapture(ball.x, ball.y, ball.z)
        }
    }

    private fun handleLocalCapture(x: Double, y: Double, z: Double) {
        val battleSnapshot = currentPayload?.let { payload ->
            val dex = payload.dexNumber.takeIf { it >= 0 } ?: return@let null
            PokemonSnapshot(
                uuid = null,
                dexNumber = dex,
                formName = payload.legendaryForm,
                aspects = emptySet(),
                legendary = payload.isLegendary
            )
        }

        val snapshot = battleSnapshot ?: nearbyPokemon
            .filter { System.currentTimeMillis() - it.seenAtMillis <= POKEMON_SNAPSHOT_LIFETIME_MILLIS }
            .minByOrNull { pokemon ->
                (pokemon.x - x).pow(2) + (pokemon.y - y).pow(2) + (pokemon.z - z).pow(2)
            }

        if (snapshot == null) {
            debugLog("[Standalone capture] local captured ball found but species snapshot was unavailable")
            return
        }

        recentCaptureAtMillis = System.currentTimeMillis()
        val regional = currentPayload?.primaryRegionalVariant
            ?.takeIf { it.isNotBlank() }
            ?: snapshot.regionalVariant
        debugLog("[Standalone capture] dex=${snapshot.dexNumber} regional='${regional.ifEmpty { "standard" }}'")
        onCapture(snapshot.dexNumber, regional)
    }

    private fun refreshNearbyPokemon(client: MinecraftClient) {
        val player = client.player ?: return
        val world = client.world ?: return
        val now = System.currentTimeMillis()
        nearbyPokemon.removeIf { now - it.seenAtMillis > POKEMON_SNAPSHOT_LIFETIME_MILLIS }

        val entities = world.getEntitiesByClass(
            PokemonEntity::class.java,
            player.boundingBox.expand(48.0)
        ) { true }
        for (entity in entities) {
            val pokemon = entity.pokemon
            nearbyPokemon.removeIf { it.uuid == pokemon.uuid }
            nearbyPokemon += snapshotOf(pokemon).copy(
                x = entity.x,
                y = entity.y,
                z = entity.z,
                seenAtMillis = now
            )
        }
    }

    private fun resetLocalState() {
        currentBattleId = null
        observedBattleId = null
        earlyVictoryTriggered = false
        battleStartDelay = 0
        currentPayload = null
        pendingRaidTier = null
        opponentFaintedAtLastTick = false
        playerFaintedAtLastTick = false
        raidVictoryTriggered = false
        recentCaptureAtMillis = 0L
        nearbyPokemon.clear()
        handledCaptureBalls.clear()
    }

    private fun invokeNoArg(target: Any, methodName: String): Any? = runCatching {
        target.javaClass.methods.firstOrNull {
            it.name == methodName && it.parameterCount == 0
        }?.invoke(target)
    }.getOrNull()

    private fun invokeOneArg(target: Any, methodName: String, argument: Any): Any? = runCatching {
        val candidates = target.javaClass.methods.filter {
            it.name == methodName && it.parameterCount == 1
        }
        val method = candidates.firstOrNull {
            it.parameterTypes[0] == argument.javaClass
        } ?: candidates.firstOrNull {
            it.parameterTypes[0].isAssignableFrom(argument.javaClass)
        } ?: return@runCatching null
        method.invoke(target, argument)
    }.getOrNull()

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
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
