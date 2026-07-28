# CobbleTunes — Project Summary

**Author:** kaizzinho
**Package:** `com.kaizzinho.cobbletunes`
**Platform:** Fabric, Minecraft 1.21.1
**Dependencies:** Cobblemon 1.7.3+, Fabric API, Fabric Language Kotlin
**Soft dependencies:** Radical Cobblemon Trainers (RCT) — enhances trainer tier detection if present, fully optional

## What it does
CobbleTunes is a Cobblemon addon that replaces all of Minecraft's ambient and battle music with original Pokémon OST tracks. Music is fully context-aware — the mod classifies what the player is doing and where they are, then selects the appropriate regional or situation-specific theme automatically. It supports both singleplayer and dedicated servers via a client/server packet architecture.

## Architecture
The mod is split across two source sets:

* **Server-side (main/kotlin)** — `CobblemonBattleListener` subscribes to Cobblemon's real battle events, classifies each battle (wild/trainer/legendary/PvP, plus RCT tier if available), and sends typed network payloads to the relevant players. `StructureZoneDetector` runs a slow server tick that detects which Cobbleverse worldgen structure or `MusicTriggerBlock` the player is near, and sends zone-change packets when it changes.
* **Client-side (client/kotlin)** — `CobbleTunesClient` receives all server packets and routes them to `ClientMusicPlayer`. `ClientMusicPlayer` owns all playback state: it drives `FadingSoundInstance` for fade-in/fade-out crossfades, suppresses vanilla's `MusicTracker`, manages per-biome ambience memory and rotation, and handles all silence gating logic. `TrackRegistry` bootstraps every registered track on client init.

## Music systems

### Battle music (Pillars 1–6, 8)
Every battle resolves to exactly one `MusicContext` at the moment it starts. The classifier runs server-side and sends raw facts to the client, which applies its own config on top:

* **Wild battles** — resolved by the opposing Pokémon's National Dex number → RegionOfOrigin → regional wild theme. Every region from Kanto to Paldea (including Hisui as its own entry) has a dedicated track.
* **Legendary/mythical encounters** — wild-encounter-only (matching the mainline games). Species-specific overrides take priority over regional defaults; shared multi-species tracks (Regis, weather trio, Tapus, etc.) are supported. A trainer's legendary ace plays the normal trainer theme, never the legendary theme.
* **Trainer battles** — full opposing roster sent server-side; client majority-votes the team's region and picks that region's trainer theme. Ties re-roll randomly each battle.
* **Gym Leader / Elite Four / Champion** — detected via RCT's `TrainerMobData.getType().id()` soft dependency. Routes to dedicated `GYM_LEADER_BATTLE`, `ELITE_FOUR_BATTLE`, and `CHAMPION_BATTLE` contexts, each with per-region tracks. Degrades gracefully to `TRAINER_BATTLE` if RCT is absent.
* **PvP and rivals** — RCT rivals and real player-vs-player battles both route to a shared `PVP_BATTLE` flat pool (rival and champion themes from all regions).

### Ambience (Pillar 7)
Biome-driven, region-blind. A client-tick watcher (throttled to ~1s) reads the player's current biome ID and feeds it to `updateAmbienceBiome()`. Key behaviours:

* **Per-biome memory** — re-entering a biome resumes the same track that was playing before, no re-roll.
* **Real-length rotation** — tracks play non-looping; rotation is triggered by `SoundManager.isPlaying()` returning false (the track's actual end), not an artificial timer.
* **Silence gating** — three distinct silence behaviours: world-join/dimension-change (fixed 10s, ignores biome changes during the wait); track-end (random 1:30–3:00, ignores biome changes); biome-transition debounce (random 4–8s, resets on every biome flicker, cuts audio immediately — prevents thin biomes like rivers from ever starting a theme the player has already walked past).
* **Unmapped biomes** — fully transparent; the previous biome's track continues until the player settles somewhere with registered coverage.
* **Battle interaction** — battle music takes over instantly, ambience resumes instantly on battle end (no silence gap on battle-end by design).
* All timers respect `MinecraftClient.isPaused()` — no music starts during the pause menu.

### Structure / proximity music (Pillar 9)
A server-side tick (~5 seconds) detects two types of zones in priority order:

* **MusicTriggerBlock (higher priority)** — a custom block with a block entity carrying a `zoneId` string. Place one inside any hand-built structure (Poké Center, Poké Mart). Scanned within a 24-block radius. Place with `/setblock ~ ~ ~ cobbletunes:music_trigger{ZoneId:"cobbletunes:pokecenter"}`.
* **Worldgen structures** — all Cobbleverse gym and special structures detected via `ServerWorld.locateStructure()` within a 5-chunk radius. No manual block placement required.

Three proximity `MusicContext` values:

* **GYM_AMBIENCE** — one track per region (Kanto→Sinnoh), resolved by which gym structure the player is near.
* **POKECENTER** — flat pool of 5 regional center themes, random pick.
* **POKEMART** — flat pool of 3 mart themes, random pick.
* **SPECIAL_STRUCTURE** — 29 dedicated 1:1 tracks for legendary shrines, villain bases, and notable named locations across all four regions (Articuno/Zapdos/Moltres caves, Ash's house, Crown Cemetery, Bell Tower, Burned Tower, Celebi Shrine, Whirl Island, Sky Pillar, Regi caves, Deoxys, Jirachi, Spear Pillar, Snowpoint Temple, lake trio temples, Eterna Building, and more).

Zone music plays immediately on detection, resumes normal biome ambience immediately on leaving (no silence gap on zone-exit by design).

## Config (cobbletunes-client.json)
Generated on first run at `.minecraft/config/cobbletunes-client.json`. All values are per-client:

| Key | Default | Description |
| :--- | :--- | :--- |
| `replaceAmbience` | `true` | Enable biome/structure ambience replacement |
| `replaceBattleMusic` | `true` | Enable battle music replacement |
| `musicVolume` | `1.0` | Master volume for all CobbleTunes tracks |
| `crossfadeSeconds` | `2.5` | Fade-in and fade-out duration for all transitions |
| `shuffleAmbienceTracks` | `true` | Random pick vs. first-registered for ambience pools |
| `worldJoinSilenceSeconds` | `10.0` | Silence after joining a world or changing dimension |
| `trackEndSilenceMinSeconds` | `90.0` | Minimum silence after a track finishes naturally |
| `trackEndSilenceMaxSeconds` | `180.0` | Maximum silence after a track finishes naturally |
| `biomeTransitionSilenceMinSeconds` | `4.0` | Minimum debounce window on biome change |
| `biomeTransitionSilenceMaxSeconds` | `8.0` | Maximum debounce window on biome change |

## Sound asset layout
assets/cobbletunes/sounds/
  ambience/
    <biome_tracks_per_region>/  ← wild ambience, tagged by biome key sets
    gym/                        ← one per region: kanto_gym, johto_gym, etc.
    pokecenter/                 ← five regional center themes
    pokemart/                   ← three mart themes
    structure/                  ← 29 dedicated legendary/special structure themes
  battle/
    wild/                       ← per-region wild battle themes
    trainer/                    ← per-region trainer themes
    gym_leader/                 ← per-region gym leader themes
    elite_four/                 ← per-region E4 themes
    champion/                   ← per-region champion themes
    pvp/                        ← rival + champion tracks for the PvP pool
    legendary/                  ← species overrides + regional defaults

## Covered regions
* **All battle contexts:** Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, Paldea
* **Ambience:** Kanto, Johto, Hoenn, Sinnoh, Unova (by design — no ambience gathered for post-Unova regions)
* **Gym/structure proximity:** Kanto, Johto, Hoenn, Sinnoh (all regions currently in the Cobbleverse datapacks)

## Known limitations / future work
* Pillar 4 (Gym Leader/E4/Champion) requires RCT to be installed — without it, all NPC trainer battles play the regional trainer theme.
* Biome resume on re-entry restarts the track from the beginning of the file rather than the exact playback position — Minecraft's sound API offers no seek capability.
* Pokémon Center/Mart trigger blocks must be manually placed inside each WorldEdit-pasted schematic via `/setblock`.
* Post-Sinnoh gyms (Unova, Kalos, Alola, Galar, Paldea) are not yet in the Cobbleverse datapacks — the architecture supports them, just add entries to `STRUCTURE_TO_REGION` and `GYM_STRUCTURES` when those datapacks arrive.
* Post-Unova ambience was intentionally not gathered — the biome coverage system supports adding it, no code changes needed, just new track registrations and OGG files.