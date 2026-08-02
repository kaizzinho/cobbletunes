package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.PlayerDeathPayload
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader

/**
 * Common/server-side code — lives in main/kotlin, NOT client/kotlin, because
 * PokemonBattle and its real ServerPlayerEntity list only exist server-side.
 *
 * Verified end-to-end against Cobblemon 1.7.3 via genSources:
 *   PokemonBattle.players, .getActor(), .isPvW/.isPvN/.isPvP, .actors
 *   BattleActor.pokemonList → BattlePokemon.effectedPokemon → Pokemon.species
 *   Species.labels (HashSet<String>), .nationalPokedexNumber: Int
 *
 * Pillar 4: RCT integration via RctBridge (below). All RCT class references are
 * isolated inside that object so the JVM never attempts to load them when RCT is
 * absent — FabricLoader.isModLoaded("rctmod") gates every call site.
 */
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

                val opposingSpecies = battle.actors
                    .filter { it != playerActor }
                    .flatMap { it.pokemonList }
                    .map { it.effectedPokemon.species }

                val legendarySpecies = opposingSpecies.firstOrNull { species ->
                    "legendary" in species.labels || "mythical" in species.labels
                }
                val isLegendary = legendarySpecies != null
                val primarySpecies = legendarySpecies ?: opposingSpecies.firstOrNull()
                val dexNumber = primarySpecies?.nationalPokedexNumber ?: -1
                val opposingDexNumbers = opposingSpecies.map { it.nationalPokedexNumber }

                // Pillar 4: resolve RCT tier if the mod is loaded and this is an
                // NPC battle — empty string means "not an RCT NPC" on the client.
                val trainerTier = if (battle.isPvN) RctBridge.resolveTrainerTier(battle) else ""

                val payload = BattleMusicStartPayload(
                    isWild = battle.isPvW,
                    isTrainer = battle.isPvN,
                    isLegendary = isLegendary,
                    dexNumber = dexNumber,
                    opposingDexNumbers = opposingDexNumbers,
                    trainerTier = trainerTier
                )
                ServerPlayNetworking.send(player, payload)
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

        // Death during a battle: Cobblemon has no BATTLE_DEFEAT event, so we
        // use Fabric's AFTER_RESPAWN instead. This fires after the player has
        // already respawned and the battle has ended server-side. We send
        // PlayerDeathPayload rather than BattleMusicEndPayload because death
        // needs a silence window before resuming ambience — the player may
        // have respawned in a completely different biome, so we should not
        // resume the pre-battle ambience track but instead let the biome
        // watcher re-detect and debounce normally after the silence.
        //
        // !! VERIFY ServerPlayerEvents.AFTER_RESPAWN's exact signature !!
        // Standard Fabric API shape: (oldPlayer, newPlayer, alive) where
        // alive=false means the player actually died (vs dimension change).
        // alive=true means this was a dimension-change "respawn", not death.
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

    /**
     * Pillar 4: soft-dependency bridge to RCT. Every RCT class reference lives
     * inside this object so the JVM class-loader never touches them unless this
     * object is actually accessed. The isModLoaded guard at every call site
     * ensures that — if RCT is absent, resolveTrainerTier() returns "" immediately
     * without ever triggering a class-load of anything from rctmod or rctapi.
     *
     * Chain used (all confirmed via decompilation of the real jars):
     *   BattleState.findFirst(battle) — static helper on rctapi's BattleState.
     *   BattleState.getParticipants2() — the NPC/trainer side of the battle.
     *   Trainer.getEntity() — the raw LivingEntity for each participant.
     *   TrainerMob.getTrainerId() — stable string ID ("kanto_brock", etc.)
     *   RCTMod.getInstance().getTrainerManager().getData(mob) — TrainerMobData.
     *   TrainerMobData.getType().id() — tier string: "leader", "e4", "champ",
     *     "rival", "normal", or a team-affiliation type.
     */
    private object RctBridge {
        private val rctAvailable by lazy {
            FabricLoader.getInstance().isModLoaded("rctmod")
        }

        fun resolveTrainerTier(battle: PokemonBattle): String {
            if (!rctAvailable) return ""
            return try {
                resolveInternal(battle)
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] RCT tier resolution failed for battle ${battle.battleId}: ${e.message}")
                ""
            }
        }

        private fun resolveInternal(battle: PokemonBattle): String {
            val state = com.gitlab.srcmc.rctapi.api.battle.BattleState.findFirst(battle)
                ?: return ""

            val opponents = state.participants2.toList()

            for (trainer in opponents) {
                val entity = trainer.entity
                if (entity !is com.gitlab.srcmc.rctmod.world.entities.TrainerMob) continue

                val tmd = com.gitlab.srcmc.rctmod.api.RCTMod.getInstance()
                    .getTrainerManager()
                    .getData(entity)

                val tierId: String = tmd.getType().id() ?: return ""

                if (tierId == "leader" || tierId == "e4" ||
                    tierId == "champ" || tierId == "rival") {
                    return tierId
                }
            }
            return ""
        }
    }
}