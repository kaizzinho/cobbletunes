package com.kaizzinho.cobbletunes.client.evolution

import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import com.kaizzinho.cobbletunes.client.sound.ClientMusicPlayer
import com.kaizzinho.cobbletunes.client.sound.EvolutionCue
import com.kaizzinho.cobbletunes.client.sound.EvolutionSoundInstance
import com.kaizzinho.cobbletunes.client.sound.RegionOfOrigin
import com.kaizzinho.cobbletunes.client.sound.TrackRegistry
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import java.util.UUID

class ClientEvolutionWatcher(
    private val config: CobbleTunesClientConfig,
    private val musicPlayer: ClientMusicPlayer,
    private val debugLog: (String) -> Unit
) {
    private data class ActiveEvolution(
        var entity: PokemonEntity,
        val region: RegionOfOrigin,
        val startingSpeciesName: String,
        val suspenseCue: EvolutionCue?,
        var startDelayTicks: Int = 20,
        var suspenseStarted: Boolean = false,
        var suspenseSound: EvolutionSoundInstance? = null
    )

    private data class SummarySnapshot(
        val speciesName: String,
        val region: RegionOfOrigin?
    )

    private val active = mutableMapOf<UUID, ActiveEvolution>()
    private val playedSounds = mutableSetOf<EvolutionSoundInstance>()
    private val summarySnapshots = mutableMapOf<UUID, SummarySnapshot>()
    private val summaryEligible = mutableSetOf<UUID>()
    private var lastWorld: ClientWorld? = null
    private var summaryScreenActive = false
    private var summaryGraceTicks = 0
    private var ducking = false

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun tick(client: MinecraftClient) {
        val world = client.world
        if (world !== lastWorld) {
            reset(client)
            lastWorld = world
        }

        if (!config.enableEvolutionMusic || world == null || client.player == null) {
            reset(client)
            return
        }

        cleanupFinishedSounds(client)
        playedSounds.forEach { it.setVolumeScale(config.musicVolume) }

        val player = client.player ?: return
        val nearby = world.getEntitiesByClass(
            PokemonEntity::class.java,
            player.boundingBox.expand(MAX_DISTANCE)
        ) { entity -> entity.squaredDistanceTo(player) <= MAX_DISTANCE_SQUARED }

        for (entity in nearby) {
            val tracked = active[entity.uuid]
            if (tracked != null) {
                tracked.entity = entity
            } else if (entity.isEvolving) {
                beginEvolution(client, entity)
            }
        }

        tickSummaryFallback(client, nearby)

        val iterator = active.iterator()
        while (iterator.hasNext()) {
            val (_, tracked) = iterator.next()
            when {
                tracked.entity.isRemoved -> {
                    stopSuspense(client, tracked)
                    iterator.remove()
                }
                !tracked.entity.isEvolving -> {
                    finishEvolution(client, tracked)
                    iterator.remove()
                }
                else -> tickSuspenseStart(client, tracked)
            }
        }

        syncDucking(client)
    }

    private fun beginEvolution(client: MinecraftClient, entity: PokemonEntity) {
        val region = resolveRegion(entity) ?: return
        val theme = TrackRegistry.evolutionThemeFor(region) ?: return
        val availableSuspense = theme.suspense.filter { resourceExists(client, it) }
        if (availableSuspense.isEmpty()) {
            debugLog(
                "[Evolution] no suspense resource for ${entity.pokemon.species.name} " +
                    "expected=${theme.suspense.joinToString { it.assetPath }}"
            )
        }
        val tracked = ActiveEvolution(
            entity = entity,
            region = region,
            startingSpeciesName = entity.pokemon.species.name,
            suspenseCue = availableSuspense.randomOrNull()
        )
        active[entity.uuid] = tracked
        debugLog(
            "[Evolution] observed ${entity.pokemon.species.name} region=${region.name.lowercase()} " +
                "suspense=${tracked.suspenseCue?.id ?: "missing"}"
        )
    }

    private fun tickSuspenseStart(client: MinecraftClient, tracked: ActiveEvolution) {
        if (tracked.suspenseStarted) return
        if (tracked.startDelayTicks > 0) {
            tracked.startDelayTicks--
            return
        }

        val cue = tracked.suspenseCue ?: return
        if (!canHearEvolution(client)) return

        val sound = EvolutionSoundInstance(
            soundEvent = cue.soundEvent,
            sourceEntity = tracked.entity,
            baseVolume = SUSPENSE_VOLUME
        )
        sound.setVolumeScale(config.musicVolume)
        client.soundManager.play(sound)
        tracked.suspenseStarted = true
        tracked.suspenseSound = sound
        playedSounds += sound
        debugLog("[Evolution] playing ${cue.id} from ${tracked.entity.uuid}")
    }

    private fun finishEvolution(client: MinecraftClient, tracked: ActiveEvolution) {
        stopSuspense(client, tracked)

        val currentSpeciesName = tracked.entity.pokemon.species.name
        if (currentSpeciesName.equals(tracked.startingSpeciesName, ignoreCase = true)) {
            debugLog("[Evolution] ended without species change ${tracked.entity.uuid}")
            return
        }

        val cue = TrackRegistry.evolutionThemeFor(tracked.region)?.complete ?: return
        if (!resourceExists(client, cue)) {
            debugLog("[Evolution] completion resource missing ${cue.assetPath}")
            return
        }
        if (!canHearEvolution(client)) return

        val sound = EvolutionSoundInstance(
            soundEvent = cue.soundEvent,
            sourceEntity = tracked.entity,
            baseVolume = COMPLETE_VOLUME
        )
        sound.setVolumeScale(config.musicVolume)
        client.soundManager.play(sound)
        playedSounds += sound
        debugLog("[Evolution] complete ${cue.id} from ${tracked.entity.uuid}")
    }

    private fun tickSummaryFallback(
        client: MinecraftClient,
        nearby: List<PokemonEntity>
    ) {
        val screen = client.currentScreen
        val isSummaryScreen = screen != null &&
            screen.javaClass.name.startsWith(SUMMARY_SCREEN_PACKAGE)
        val observed = collectClientPartyPokemon()

        if (isSummaryScreen) {
            summaryGraceTicks = SUMMARY_GRACE_TICKS
            if (!summaryScreenActive) {
                summaryScreenActive = true
                summarySnapshots.clear()
                summaryEligible.clear()
                observed.forEach { (uuid, pokemon) ->
                    summarySnapshots[uuid] = snapshot(pokemon)
                    summaryEligible += uuid
                }
                debugLog(
                    "[Evolution] summary fallback armed ${observed.size} party pokemon " +
                        "for direct storage sync"
                )
                return
            }

            observed.keys.forEach(summaryEligible::add)
        } else {
            if (summaryScreenActive) {
                summaryScreenActive = false
                debugLog(
                    "[Evolution] summary screen closed, keeping party watch for " +
                        "${SUMMARY_GRACE_TICKS / 20}s"
                )
            }

            if (summaryGraceTicks <= 0) {
                summarySnapshots.clear()
                summaryEligible.clear()
                return
            }
            summaryGraceTicks--
        }

        for ((pokemonUuid, pokemon) in observed) {
            val previous = summarySnapshots[pokemonUuid]
            val current = snapshot(pokemon)

            if (
                previous != null &&
                !previous.speciesName.equals(current.speciesName, ignoreCase = true) &&
                pokemonUuid in summaryEligible
            ) {
                val hasWorldEntity = nearby.any { entity ->
                    entity.pokemon.uuid == pokemonUuid && !entity.isRemoved
                } || active.values.any { tracked ->
                    tracked.entity.pokemon.uuid == pokemonUuid && !tracked.entity.isRemoved
                }

                if (!hasWorldEntity) {
                    debugLog(
                        "[Evolution] summary party transition ${previous.speciesName} -> " +
                            "${current.speciesName} using player position"
                    )
                    playSummaryCompletion(
                        client = client,
                        pokemonUuid = pokemonUuid,
                        previous = previous,
                        previousSpeciesName = previous.speciesName,
                        currentSpeciesName = current.speciesName
                    )
                } else {
                    debugLog(
                        "[Evolution] summary party transition suppressed by live world entity " +
                            "${previous.speciesName} -> ${current.speciesName}"
                    )
                }

                summaryEligible.remove(pokemonUuid)
            }
            summarySnapshots[pokemonUuid] = current
        }
    }

    private fun playSummaryCompletion(
        client: MinecraftClient,
        pokemonUuid: UUID,
        previous: SummarySnapshot,
        previousSpeciesName: String,
        currentSpeciesName: String
    ) {
        val region = previous.region ?: return
        val cue = TrackRegistry.evolutionThemeFor(region)?.complete ?: return
        if (!resourceExists(client, cue)) {
            debugLog("[Evolution] summary completion resource missing ${cue.assetPath}")
            return
        }
        if (!canHearEvolution(client)) {
            debugLog("[Evolution] summary completion muted by CobbleTunes/Records/Master volume")
            return
        }

        val player = client.player ?: return
        val sound = EvolutionSoundInstance(
            soundEvent = cue.soundEvent,
            sourceEntity = player,
            baseVolume = COMPLETE_VOLUME
        )
        sound.setVolumeScale(config.musicVolume)
        client.soundManager.play(sound)
        playedSounds += sound
        debugLog(
            "[Evolution] summary complete ${cue.id} for $pokemonUuid " +
                "$previousSpeciesName -> $currentSpeciesName at player position"
        )
    }

    private fun collectClientPartyPokemon(): Map<UUID, Pokemon> {
        val found = linkedMapOf<UUID, Pokemon>()
        for (pokemon in CobblemonClient.storage.party) {
            if (pokemon != null) {
                found[pokemon.uuid] = pokemon
            }
        }
        return found
    }

    private fun snapshot(pokemon: Pokemon): SummarySnapshot =
        SummarySnapshot(
            speciesName = pokemon.species.name,
            region = resolveRegion(pokemon)
        )

    private fun stopSuspense(client: MinecraftClient, tracked: ActiveEvolution) {
        val sound = tracked.suspenseSound ?: return
        sound.finish()
        client.soundManager.stop(sound)
        tracked.suspenseSound = null
        playedSounds.remove(sound)
    }

    private fun syncDucking(client: MinecraftClient) {
        val player = client.player
        val shouldDuck = player != null && canHearEvolution(client) && playedSounds.any { sound ->
            client.soundManager.isPlaying(sound) && sound.isAudibleTo(player, MAX_DISTANCE_SQUARED)
        }

        if (shouldDuck) {
            if (!ducking) {
                ducking = true
                musicPlayer.setEvolutionDucking(true)
            }
            return
        }

        if (!ducking) return

        ducking = false
        musicPlayer.setEvolutionDucking(false, DUCK_RESTORE_FADE_SECONDS)
    }

    private fun cleanupFinishedSounds(client: MinecraftClient) {
        playedSounds.removeIf { sound -> !client.soundManager.isPlaying(sound) }
    }

    private fun reset(client: MinecraftClient) {
        active.values.forEach { stopSuspense(client, it) }
        active.clear()
        summarySnapshots.clear()
        summaryEligible.clear()
        summaryScreenActive = false
        summaryGraceTicks = 0
        playedSounds.forEach { sound ->
            sound.finish()
            client.soundManager.stop(sound)
        }
        playedSounds.clear()
        if (ducking) {
            ducking = false
            musicPlayer.setEvolutionDucking(false, transitionSeconds = 0f)
        }
    }

    private fun resolveRegion(entity: PokemonEntity): RegionOfOrigin? =
        resolveRegion(entity.pokemon)

    private fun resolveRegion(pokemon: Pokemon): RegionOfOrigin? {
        val dexNumber = pokemon.species.nationalPokedexNumber
        val formName = pokemon.form.name
        val regional = (pokemon.aspects + formName).firstNotNullOfOrNull { value ->
            RegionOfOrigin.fromRegionalVariant(value)
        }
        if (regional != null) return regional

        if (dexNumber == 550) {
            val whiteStriped = (pokemon.aspects + formName).any { value ->
                value.lowercase().replace('_', '-').replace(' ', '-').contains("white-striped")
            }
            if (whiteStriped) return RegionOfOrigin.HISUI
        }

        return RegionOfOrigin.fromDexNumber(dexNumber)
    }

    private fun resourceExists(client: MinecraftClient, cue: EvolutionCue): Boolean =
        client.resourceManager.getResource(Identifier.of(MOD_ID, cue.assetPath)).isPresent

    private fun canHearEvolution(client: MinecraftClient): Boolean {
        if (config.musicVolume <= 0.0001f) return false
        val recordsVolume = client.options
            .getSoundVolumeOption(SoundCategory.RECORDS)
            .value
            .toFloat()
        val masterVolume = client.options
            .getSoundVolumeOption(SoundCategory.MASTER)
            .value
            .toFloat()
        return recordsVolume > 0.0001f && masterVolume > 0.0001f
    }

    companion object {
        private const val SUMMARY_SCREEN_PACKAGE = "com.cobblemon.mod.common.client.gui.summary."
        private const val SUMMARY_GRACE_TICKS = 200
        private const val MAX_DISTANCE = 32.0
        private const val MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE
        private const val DUCK_RESTORE_FADE_SECONDS = 2.0f
        private const val SUSPENSE_VOLUME = 0.62f
        private const val COMPLETE_VOLUME = 0.78f
    }
}
