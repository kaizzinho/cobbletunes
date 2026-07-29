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
    var replaceBattleMusic: Boolean = true,
    var musicVolume: Float = 1.0f,
    var crossfadeSeconds: Float = 2.5f,
    var shuffleAmbienceTracks: Boolean = true,
    // Silence gaps around ambience, matching vanilla Minecraft's feel of quiet
    // before/between tracks rather than continuous back-to-back music.
    // Vanilla itself (net.minecraft.sound.MusicSound's minDelay/maxDelay, in
    // ticks) rolls a random silence length between two bounds rather than a
    // fixed one — Survival Overworld defaults to 10-20 real minutes. Track-end
    // and biome-transition below follow that same random-range shape, just
    // with much shorter windows appropriate to this mod's pace. Ambience
    // rotation itself is no longer a fixed/random window (see
    // ClientMusicPlayer) — each track now plays through its own ACTUAL length
    // once, detected via the sound engine, then one of the ranges below
    // applies before the next track starts.
    //
    // worldJoinSilenceSeconds: right after connecting/loading a world OR
    //   changing dimension — no ambience plays at all until this elapses.
    //   Single fixed value, not a range (not asked to vary this one). Does NOT
    //   reset if the biome changes during the wait (see ClientMusicPlayer's
    //   "pending silence" mechanism).
    // trackEndSilenceMinSeconds/MaxSeconds: once a track finishes playing
    //   through on its own, before the next one starts. Same "does NOT reset
    //   on biome changes" behavior as worldJoinSilenceSeconds above — if the
    //   player wanders to a different biome DURING the wait, the countdown
    //   keeps running regardless, and whichever biome they're in when it
    //   elapses is what starts.
    // biomeTransitionSilenceMinSeconds/MaxSeconds: walking into a new biome
    //   while the old track was still playing (hadn't finished on its own).
    //   Opposite behavior from the two above — this is a DEBOUNCE: audio cuts
    //   to silence the instant the biome changes, and if the observed biome
    //   changes AGAIN before this window elapses, the countdown restarts
    //   against the newest biome. Only commits (and fades in) once a biome has
    //   read the same for the full window — this is what keeps a thin biome
    //   like a river from starting its own theme if the player's already
    //   walked past it. Skipped entirely whenever a worldJoin/trackEnd
    //   countdown is already active.
    var worldJoinSilenceSeconds: Float = 10f,
    var trackEndSilenceMinSeconds: Float = 90f,
    var trackEndSilenceMaxSeconds: Float = 180f,
    var biomeTransitionSilenceMinSeconds: Float = 4f,
    var biomeTransitionSilenceMaxSeconds: Float = 8f,
    var replaceMenuMusic: Boolean = true
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