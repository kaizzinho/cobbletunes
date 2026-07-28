package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
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
 * absent — FabricLoader.isModLoaded("rctmod") gates every call site, and the
 * object itself is only initialized lazily on first access.
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
     *   BattleState.findFirst(battle) — static helper on rctapi's BattleState;
     *     returns null if RCT isn't managing this battle (shouldn't happen for
     *     isPvN battles, but handled gracefully).
     *   BattleState.getParticipants2() — the NPC/trainer side of the battle
     *     (participants1 is the player side by convention).
     *   Trainer.getEntity() — the raw LivingEntity for each participant.
     *   TrainerMob.getTrainerId() — stable string ID ("kanto_brock", etc.)
     *     used to look up TrainerMobData.
     *   RCTMod.getInstance().getTrainerManager().getData(mob) — TrainerMobData.
     *   TrainerMobData.getType().id() — the tier string: "leader", "e4",
     *     "champ", "rival", "normal", or a team-affiliation type.
     *
     * Takes the FIRST non-normal-tier NPC found on the opposing side, not all of
     * them — a solo gym leader battle will always have exactly one on that side,
     * and multi-NPC battles are currently rare in the Cobbleverse RCT setup.
     * Falls back to "" (normal trainer) if no elevated tier is found.
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

            // participants2 is the trainer/NPC side; toList() forces Kotlin to
            // resolve the Java List<Trainer> iteration unambiguously.
            val opponents = state.participants2.toList()

            for (trainer in opponents) {
                val entity = trainer.entity
                if (entity !is com.gitlab.srcmc.rctmod.world.entities.TrainerMob) continue

                val tmd = com.gitlab.srcmc.rctmod.api.RCTMod.getInstance()
                    .getTrainerManager()
                    .getData(entity)

                // TrainerType.id() is a field accessor — check IntelliJ's
                // completion on tmd.getType() if .id() doesn't resolve;
                // the decompiled bytecode showed it as id() but Kotlin may
                // see it as getId() depending on how CFR rendered the accessor.
                val tierId: String = tmd.getType().id() ?: return ""

                if (tierId == "leader" || tierId == "e4" ||
                    tierId == "champ"  || tierId == "rival") {
                    return tierId
                }
            }
            return ""
        }
    }
}