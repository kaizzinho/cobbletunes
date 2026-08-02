package com.kaizzinho.cobbletunes.client.config

import com.google.gson.GsonBuilder
import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import java.io.File

/**
 * Serialized to config/cobbletunes-client.json.
 * Kept intentionally flat — one toggle/slider per concept — so it maps 1:1 onto
 * a future in-game options screen or Mod Menu integration.
 *
 * legendaryMusicOverridesTrainer was removed: Pillar 2 decision is that the
 * legendary theme is wild-encounter-only, matching the mainline games — a
 * trainer's legendary ace always just plays the normal trainer theme, no toggle
 * needed since there's no longer a choice to make there.
 */
data class CobbleTunesClientConfig(
    var replaceAmbience: Boolean = true,
    var replaceMenuMusic: Boolean = true,
    var replaceBattleMusic: Boolean = true,
    var musicVolume: Float = 1.0f,
    var crossfadeSeconds: Float = 2.5f,
    var shuffleAmbienceTracks: Boolean = true,
    // Pillar 7 refinement: instead of a fixed per-track duration (which would've
    // meant hand-curating a real length for every ambience file), each ambience
    // selection rolls a random rotation target somewhere in this window before
    // it's eligible to be swapped out. Defaults to 3:30–4:00, roughly the median
    // length of a vanilla Minecraft ambience track. Lower these if a track still
    // feels like it's overstaying its welcome; crossfadeSeconds above already
    // handles the fade in/out on every rotation, no separate fade config needed.

    // Silence gaps around ambience, matching vanilla Minecraft's feel of quiet
    // before/between tracks rather than continuous back-to-back music.
    // Vanilla itself (net.minecraft.sound.MusicSound's minDelay/maxDelay, in
    // ticks) rolls a random silence length between two bounds rather than a
    // fixed one — Survival Overworld defaults to 10-20 real minutes. That ratio
    // works for vanilla because vanilla tracks are much longer; against this
    // mod's 3:30-4:00 rotation window, a vanilla-scale silence would mean
    // standing in silence roughly 3x as long as the music plays. 1:30-3:00
    // below keeps silence noticeably shorter than the average track instead,
    // so music still dominates but the pause still reads as intentional.
    //
    // worldJoinSilenceSeconds: right after connecting/loading a world OR
    //   changing dimension — no ambience plays at all until this elapses.
    //   Single fixed value, not a range (not asked to vary this one). Does NOT
    //   reset if the biome changes during the wait (see ClientMusicPlayer's
    //   "pending silence" mechanism).
    // trackEndSilenceMinSeconds/MaxSeconds: once a track's rotation budget
    //   is used up, before the
    //   next one starts. Same "does NOT reset on biome changes" behavior as
    //   worldJoinSilenceSeconds above — if the player wanders to a different
    //   biome DURING the wait, the countdown keeps running regardless, and
    //   whichever biome they're in when it elapses is what starts.
    // biomeTransitionSilenceMinSeconds/MaxSeconds: walking into a new biome
    //   while the old track still had budget left. Opposite behavior from the
    //   two above — this is a DEBOUNCE: audio cuts to silence the instant the
    //   biome changes, and if the observed biome changes AGAIN before this
    //   window elapses, the countdown restarts against the newest biome. Only
    //   commits (and fades in) once a biome has read the same for the full
    //   window — this is what keeps a thin biome like a river from starting
    //   its own theme if the player's already walked past it. Kept short
    //   (4-8s) since it's meant to feel like a brief, deliberate pause, not a
    //   long gap. Skipped entirely whenever a worldJoin/trackEnd countdown is
    //   already active — that one already guarantees silence on its own.
    var worldJoinSilenceSeconds: Float = 10f,
    var trackEndSilenceMinSeconds: Float = 90f,
    var trackEndSilenceMaxSeconds: Float = 180f,
    var biomeTransitionSilenceMinSeconds: Float = 4f,
    var biomeTransitionSilenceMaxSeconds: Float = 8f,
    // When true, all routine state-transition logs (silence countdowns,
    // rotation, zone enter/exit, battle resume, etc.) print to the log —
    // otherwise only warnings about real problems are shown. Meant for bug
    // reports: ask the reporter to enable this, reproduce the issue, and
    // paste the log. Server-side files (CobblemonBattleListener,
    // StructureZoneDetector) use the separate cobbletunes-server.json's
    // matching debugLogging field, since they can't see this client config.
    var debugLogging: Boolean = false
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val configFile: File
            get() = File(FabricLoader.getInstance().configDir.toFile(), "$MOD_ID-client.json")

        fun load(): CobbleTunesClientConfig {
            val file = configFile
            if (!file.exists()) {
                val default = CobbleTunesClientConfig()
                default.save()
                return default
            }
            return try {
                file.reader().use { gson.fromJson(it, CobbleTunesClientConfig::class.java) }
                    ?: CobbleTunesClientConfig()
            } catch (e: Exception) {
                LOGGER.warn("[$MOD_ID] Failed to read client config, falling back to defaults", e)
                CobbleTunesClientConfig()
            }
        }
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writer().use { gson.toJson(this, it) }
        } catch (e: Exception) {
            LOGGER.warn("[$MOD_ID] Failed to write client config", e)
        }
    }
}