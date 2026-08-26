# CobbleTunes — Music Framework 🎵🎮

![Version](https://img.shields.io/badge/version-1.0-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-Loader%200.17.2%2B-DBB69B?logo=minecraft&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Fabric%20Language%20Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.3-3E8E41)
![License](https://img.shields.io/badge/license-MIT-blue)
![Audio](https://img.shields.io/badge/audio-not%20included-lightgrey)

*Read this in [English](#english) | Leia em [Português](#português)*

---

## English

### Overview

**CobbleTunes** is a dynamic music framework for Cobblemon on Fabric. It replaces Minecraft music with context-aware battle themes, regional ambience, structure music, Victory themes, Battle Tower music, Game Corner zones, title-screen tracks, low-HP cues, and client-observed spatial evolution music.

The mod ships the routing and playback system only. It does **not** include, download, or generate Pokémon OST files. Music is supplied by a normal Minecraft resource pack under `assets/cobbletunes/sounds/`.

The current source defines **441 sound events** across battle, Victory, Battle Tower, Game Corner, evolution, ambience, menu, and effects. The complete list is in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### What it does

CobbleTunes is client-first. On a public Cobblemon server without CobbleTunes, the client reads the battle state it already receives, resolves the opponent role and region locally, and plays the matching registered `SoundEvent`. When the server also has CobbleTunes, its packets become the authoritative source for exact server-only metadata such as worldgen structure identity and manual trigger zones.

```text
Cobblemon battle
→ classify wild trainer pvp boss faction or facility battle
→ resolve role and region
→ apply species form and regional variant overrides
→ use local client data or an authoritative server route
→ play the matching music context
```

World music uses a similar priority model:

```text
battle
↓
victory and loot menu
↓
structure trigger and battle tower zone
↓
biome ambience
```

Lower-priority world detection keeps running while battle or Victory music owns the audio slot, so the current zone or biome is ready when the higher-priority music ends.

### Client-only and server-enhanced modes

CobbleTunes can be installed on the **client only** and used on public Cobblemon servers that do not run the mod. In that mode it infers standard wild, trainer, PvP, Legendary/Mythical, regional-form, Victory, capture, WildBosses, Raid Dens, and observable RCT routes from client-visible Cobblemon/mod state. Raid Dens can also be identified from its native tier battle sound, and successful client-only raids trigger the same five-second regional Victory cue when the raid boss faints, followed by a two-second full-volume tail before fade-out begins.

World biome ambience, menu music, low-HP handling, spatial evolution music, volume suspension, and other purely client-side systems work normally. Evolution never needs a CobbleTunes server packet: visible world evolutions are observed from Cobblemon's synchronized `PokemonEntity.isEvolving` state, while an evolution completed directly from Cobblemon's Summary UI with no live world entity is detected from Cobblemon's synchronized client party storage and plays only the regional congratulation cue from the player's position. The Summary watch remains armed briefly after the screen closes so the final party sync is not missed. For structures, standalone mode uses conservative fingerprints for locations the client can recognize reliably, currently including Gimmighoul towers, Poké Centers, Ruined Portals, and villages. Exact `StructureStart` identities for the full Cobbleverse/Terralith/BCA/Repurposed/Legendary Monuments mapping and manual marker zones remain server-enhanced features because vanilla chunk networking does not provide those authoritative structure identities to a normal remote client.

When a CobbleTunes server bridge is present, the client automatically stops its fallback classifier and prefers the existing server-authoritative packets. Server packets are sent only to clients that advertise the matching CobbleTunes payload channel, so the fallback and enhanced paths do not compete. The server side registers no CobbleTunes block, item, or entity content, so installing the same JAR on a server does not make CobbleTunes mandatory for other players.

### Key Features

- [x] **Regional battle music** for Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, and Paldea.
- [x] **Wild, trainer, Gym Leader, Elite Four, Champion, rival, PvP, faction, Frontier Brain, Battle Tower, and Legendary/Mythical contexts.**
- [x] **Regional form routing** so Alolan, Galarian, Hisuian, and Paldean forms use their form region instead of the base species region.
- [x] **Form-aware Legendary routing** for encounters such as Kyurem, Necrozma, Eternatus, Calyrex, Terapagos, and the Galarian birds.
- [x] **Dynamic RCT classification** with role, region, faction, rank, trainer ID, progression-aware routing, and exact themes for named custom trainers.
- [x] **Villain faction themes** for Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine, and Ultra Recon Squad routes.
- [x] **Frontier Brain music** with a dedicated battle context.
- [x] **WildBosses and Cobblemon Raid Dens integrations** with tier-weighted regional PvP, generic Legendary, and BW World Tournament pools, actor-entity Raid Dens detection, and a dedicated 5-second regional Victory cue plus a 2-second full-volume tail before fade-out when a raid is cleared.
- [x] **Species-safe Boss pools** that keep unique Legendary encounter themes out of unrelated Boss fights.
- [x] **Victory + Cobblemon Loot Menu integration** with Victory starting on the decisive opponent faint, a normal short cue when no loot screen opens, extension while the loot screen is open, and current-zone resume afterward.
- [x] **Capture Victory themes** for successful Pokémon captures, including captures made outside battle, using the captured Pokémon region and the existing wild Victory pool.
- [x] **Battle Tower floor pools** with low, mid, high, and final tiers plus dedicated Battle Tower battle music.
- [x] **Biome ambience memory and rotation** with silence windows and biome-transition debounce.
- [x] **Underground ambience detection** using sky light and player height.
- [x] **Cobbleverse exact structures**, **vanilla structures**, **Terralith structures**, **CobblemonAdditions/BCA villages**, **Repurposed Structures**, selected **Legendary Monuments**, and **Cobblemon Gimmighoul towers** using existing music.
- [x] **Hand-placed music zones** for Poké Centers, Poké Marts, Gyms, Game Corners/Casinos, special locations, and Battle Tower floors.
- [x] **Game Corner pool** with 15 FRLG, Emerald, HGSS, and Platinum tracks played as a shuffled no-repeat playlist.
- [x] **Title-screen music** that stays active across submenus and stops when a world loads.
- [x] **Low-HP cue** for the active battle Pokémon with a single 80%-volume alert.
- [x] **Spatial evolution music** emitted from visible evolving Pokémon, plus a Summary-screen fallback that plays only the local regional completion cue when no world entity exists.
- [x] **Player-death handling** and safe world/zone reset.
- [x] **Client and server debug logging**, disabled by default.
- [x] **Client-only public-server mode** with automatic server-bridge detection and server-authoritative upgrades when available.
- [x] **Mod Menu integration** with a native CobbleTunes config screen for client music settings.

### Requirements

#### Required

- Minecraft `1.21.1`
- Fabric Loader `0.17.2+`
- Fabric API
- Fabric Language Kotlin `1.13.3+`
- Cobblemon `1.7.3`
- Java `21`

For multiplayer, installing CobbleTunes on the **client is enough** for the standalone experience on a public Cobblemon server. Installing the same CobbleTunes JAR on the server is optional and enables exact server-only structure/manual-zone routing and authoritative compatibility metadata for CobbleTunes clients. Players without CobbleTunes can still join because the server bridge registers no custom gameplay content and only sends CobbleTunes payloads to clients that advertise support.

#### Optional integrations

- **Mod Menu** — opens a native CobbleTunes configuration screen for client music settings.
- **Radical Cobblemon Trainers** — trainer role, region, faction, progression-aware routing, and exact theme overrides for supported named custom trainers.
- **WildBosses** — Boss-specific weighted regional battle pools.
- **Cobblemon Raid Dens** — raid battles reuse the same regional weighted pools. Client-only mode can recognize the native tier battle sound; server-enhanced mode keeps the actor-backed Raid Dens metadata, raid ID/active raid map, and battle-marker fallbacks.
- **Cobblemon Loot Menu** — post-battle Victory music while the loot screen is active.
- **CobblemonAdditions / BCA structures** — maps the current `4.1.6` dark, default, and fighting village variants into the existing small/mid/large pools, with the BCA Witch Hut reusing the Swamp Hut pool. Legacy generic BCA village IDs remain supported.
- **Terralith datapack** — maps all 26 structures referenced by the supplied Terralith structure sets into existing vanilla/BCA music pools.
- **Repurposed Structures** — reuses the existing vanilla/BCA music pools for all 107 worldgen structure IDs present in `7.5.21+1.21.1`.
- **Legendary Monuments** — reuses existing Sinnoh and structure tracks for Distortion Portal, Giratina Island, Turnback Cave, the three Sinnoh lakes, and Stark Mountain.

Mod Menu, RCT, WildBosses, Cobblemon Raid Dens, Cobblemon Loot Menu, CobblemonAdditions, Repurposed Structures, and Legendary Monuments are soft mod integrations. Terralith support is registry-driven and only activates when its datapack structures exist.

### Battle routing

#### Standard regional contexts

CobbleTunes keeps the battle role separate from the region. A Kanto Gym Leader route looks like:

```text
leader|kanto
```

A Sinnoh Frontier Brain route looks like:

```text
frontier|sinnoh
```

A faction route keeps the exact theme ID:

```text
faction:team_galactic_commander|sinnoh
```

When trainer metadata contains a region, that region wins. If it does not, the opposing roster votes by region.

#### Regional variants

National Dex numbers alone are not enough for regional forms. CobbleTunes also sends regional-form metadata with the battle packet.

Examples:

```text
alolan exeggutor → alola
alolan grimer and muk → alola
galarian weezing → galar
hisuian growlithe → hisui
paldean wooper → paldea
```

The regional variant affects wild themes, trainer roster voting, WildBosses and Raid Dens regional pools, Legendary fallback routing, and Victory region selection.

#### RCT trainer roles and factions

RCT integration is reflection-based. CobbleTunes reads the trainer ID and type when available, then classifies the encounter without making RCT a hard dependency.

Recognized role routes include:

```text
normal
leader
e4
champ
rival
frontier
```

Recognized faction families include Team Rocket, Team Aqua, Team Magma, Team Galactic, Team Plasma, Team Flare, Team Skull, Aether Foundation, and Ultra Recon Squad.

Faction matching runs before broad role matching. This keeps cases such as Rocket Giovanni separate from a normal regional Gym Leader Giovanni route.

The supplied RCT Tower datapack also has exact battle-theme overrides for its ten named `pokemon_trainer_*` encounters. Floor trainers such as `f1_trainer1` through `f9_trainer10` are intentionally untouched and continue using the normal Battle Tower trainer route.

| Trainer | Exact battle theme |
|---|---|
| Barry | Sinnoh rival |
| Gold | Johto BW World Tournament Champion remix |
| Green | Kanto BW World Tournament Champion remix |
| Kris | Johto BW World Tournament Champion remix |
| May | Hoenn BW World Tournament Champion remix |
| Morimoto | B2W2 PWT Final |
| Oak | Kanto BW World Tournament Champion remix |
| Red | Kanto BW World Tournament Champion remix |
| Silver | Johto rival |
| Steven | Hoenn Champion |

The override matches the exact trainer ID, including the equivalent `rctmod:`-namespaced form when RCT exposes it that way. Barry and Silver keep rival Victory routing, Steven keeps Champion Victory routing, and the other named trainers keep normal regional trainer Victory routing.

### Legendary and Mythical routing

Dedicated encounter tracks are selected by species and, where needed, form. Generic regional Legendary tracks remain available as fallbacks.

Current form-sensitive cases include:

- Galarian Articuno, Zapdos, and Moltres;
- base vs Black/White Kyurem;
- base, fused, and Ultra Necrozma;
- Eternatus vs Eternamax Eternatus;
- Calyrex vs Ice Rider / Shadow Rider Calyrex;
- Terapagos vs Stellar Terapagos.

The resource pack also contains dedicated encounter themes for major Legendary/Mythical groups across the supported regions. See the manifest for every mapped file.

### WildBosses and Cobblemon Raid Dens integration

WildBosses and Cobblemon Raid Dens are optional. Actual WildBoss encounters and Raid Dens battles use the same regional weighted music system. Raid Dens remains reflection-only, but detection checks the server-side actor-backed Pokémon first because its raid metadata is already authoritative when Cobblemon fires the battle-start event. If needed, CobbleTunes resolves the raid through `crd_getRaidId()` and Raid Dens' active raid map before falling back to the battle-level marker. Raid completion is also read from Raid Dens' own `RAID_END` event, because Raid Dens does not follow Cobblemon's normal Victory lifecycle for this encounter type.

| Music tier | Regional rival/PvP | Generic Legendary | BW World Tournament | Raid Dens tier |
|---|---:|---:|---:|---|
| Uncommon | 80% | 15% | 5% | 1 star |
| Rare | 70% | 20% | 10% | 2 stars |
| Epic | 60% | 30% | 10% | 3–4 stars |
| Legendary | 50% | 35% | 15% | 5–6 stars |
| Mythic | 45% | 40% | 15% | 7 stars |

The raid Pokémon roster determines the regional pool in the same way as WildBosses. Species-specific Legendary themes are excluded from the generic Legendary pool, and immediate track repeats are avoided when another valid choice exists.

With server debug logging enabled, Raid Dens probes emit compact `[RaidDensCompat] candidate` and `resolved` lines showing whether the actor entity, active raid map, or battle fallback supplied the tier. A successful `RAID_END` sends the normal regional wild Victory resolver to the client for a fixed **5-second** base cue, keeps it at full volume for another **2 seconds**, then starts the fade back to the latest valid world ambience. The cue survives the Raid Dens dimension transition back to the overworld, so world-join silence cannot cut it short. Failed raids stop the battle theme without playing Victory.

### Victory, captures, and Cobblemon Loot Menu

Battle Victory music keeps its soft integration with `cobblemon_loot_menu`. Successful Pokémon captures also trigger the regional wild Victory theme even when the capture happens outside battle.

When a supported battle is won and Cobblemon Loot Menu is installed:

```text
battle victory
→ final opposing Pokémon faints
→ Victory starts immediately
→ no loot screen: Victory uses the normal brief cue
→ loot screen appears: the same Victory keeps playing
→ Victory continues until the loot screen closes
```

CobbleTunes no longer delays Battle Victory while waiting to discover whether loot exists. The client watches Cobblemon's battle-log updates and synchronized roster HP, so the regional Victory cue starts as soon as the decisive opponent faint is reported. The normal cue has a three-second base duration, then stays at full volume for another two seconds before fade-out begins. If `LootSelectionScreen` appears during the post-battle transition, the current Victory is promoted into a held cue and continues until that screen closes. After the loot screen closes, Victory also remains at full volume for two seconds before fade-out begins. If the brief cue already ended before the loot screen appears, CobbleTunes restarts the same Victory immediately for the loot screen. If Cobblemon Loot Menu is not installed, the same decisive-faint trigger is used, with Cobblemon's later battle Victory event retained as a fallback.

A successful capture uses the same regional wild Victory resolver with a three-second base cue followed by a two-second full-volume tail before fade-out. This applies to captures that end a wild battle and to direct overworld captures. Regional forms still override the base National Dex region. Capture Victory never waits for the Loot Menu. Successful Raid Dens clears also reuse the regional wild Victory resolver with a **five-second** base cue followed by the same **two-second** full-volume tail before fade-out. With the server bridge, Raid Victory uses Raid Dens' `RAID_END` event. In client-only mode, the synchronized raid boss reaching 0 HP is the clear point, so the cue still starts immediately without waiting for Raid Dens to close the battle or return the player to the overworld.

Biome and structure detection continue while Victory owns playback. When Victory ends, CobbleTunes restores the **latest** valid structure, Battle Tower floor, or biome instead of returning to stale pre-battle ambience.

Hisui intentionally has no traditional Victory theme in this pack.

### Battle Tower

Battle Tower ambience uses four pools:

```text
battle_tower_low
battle_tower_mid
battle_tower_high
battle_tower_final
```

Floor trigger IDs such as `cobbletunes:battle_tower_floor_1` through `cobbletunes:battle_tower_floor_10` resolve into those pools. Nearby floor triggers are checked nearest-first so vertically overlapping floors do not steal each other's music.

A trainer battle that begins while the current zone is `BATTLE_TOWER` is routed to the dedicated Galar Battle Tower battle theme. In client-only mode, RCT floor trainer IDs such as `f1_trainer1` through the tower floor pattern also resolve directly to that same Battle Tower battle theme without turning them into named special-trainer overrides.

### Game Corner and Casino zones

A manual server marker can turn a custom build into a Game Corner or Casino without registering a custom CobbleTunes block. Summon a vanilla `minecraft:marker` with a `cobbletunes_zone:<zoneId>` tag and use either of these zone IDs:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Entering the zone starts a shuffled Game Corner playlist. The first track is random, and when it finishes CobbleTunes automatically advances to another track without looping the same file. Every track is played once before the pool reshuffles, and the reshuffle avoids immediately repeating the track that just finished. Battle and Victory music still have priority; when they end, the active Game Corner playlist continues while the zone remains active.

The pool has 15 expected files under `assets/cobbletunes/sounds/gamecorner/`, grouped across FRLG, Emerald, HGSS, and Platinum. The resource pack audio is still supplied separately; the source only registers the events and routing. See [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) for the exact filenames and source-theme suggestions.

Manual server zones use invisible vanilla marker entities within a 12-block radius. For example, `/summon minecraft:marker 300 64 300 {Tags:["cobbletunes_zone:cobbletunes:game_corner"]}` creates a Game Corner anchor. The nearest tagged marker wins when zones overlap. The old `cobbletunes:music_trigger` custom block was removed so a server-side CobbleTunes install stays optional for clients; worlds that used that old block should convert those anchors to markers before updating. See [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) for the full marker setup examples.

### Ambience and structures

Regional biome ambience is currently defined for Kanto, Johto, Hoenn, Sinnoh, and Unova. Tracks are mapped to vanilla and Terralith biome groups such as plains, forests, caves, oceans, mountains, snow, deserts, and volcanic areas.

Biome tracks use memory, rotation budgets, silence ranges, and transition debounce. Crossing a tiny biome does not immediately force a new track if the biome changes again during the debounce window.

Structure music has higher priority than biome ambience. With the optional server bridge, supported exact sources include:

- all 71 registered Cobbleverse structures across the main, Johto, Hoenn, and Sinnoh datapacks, including the current nested `legendary/` and `mythical/` registry paths;
- vanilla structures such as Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals, and more;
- all 26 Terralith structures referenced by its active structure sets, aliased into the closest existing vanilla/BCA pools;
- CobblemonAdditions `4.1.6` dark, default, and fighting villages mapped into the existing BCA small/mid/large pools, plus its Witch Hut mapped to the Swamp Hut pool;
- Repurposed Structures variants mapped back into the closest existing vanilla/BCA pool;
- selected Legendary Monuments structures mapped to existing Sinnoh/structure tracks;
- all six Cobblemon Gimmighoul tower worldgen structures (`deserted`, `frozen`, `lush`, `rooted`, `sunscorched`, and `temperate`) mapped to a creepy three-track pool built from the existing Pokémon Tower, Lavender Town, and Pokégear Unown themes;
- manual zones anchored by vanilla `minecraft:marker` entities tagged with `cobbletunes_zone:<zoneId>`.

In server-enhanced mode, structure detection resolves the real `StructureStart` from nearby chunk references instead of asking only whether the player is already inside the structure. Compact structures up to 16×16 blocks now receive a 4-block margin on every horizontal side and 4 blocks vertically; larger structures keep a 1-block horizontal and 2-block vertical margin. This gives Ruined Portals, small ruins, huts, and similar landmarks a stable music zone around the visible build instead of dropping back to biome ambience when the player takes one or two steps past the exact structure box.

Most fixed zone music loops until the player leaves the zone. Villages are intentionally different: the selected village theme plays once, waits a random **5–60 seconds**, then replays the same theme while the player remains inside. Leaving and re-entering selects another theme when the pool has an alternative. Game Corner and Casino zones also remain non-looping and advance through their shuffled 15-track playlist. Battle and Victory music temporarily take priority without discarding the current zone state. Current Cobbleverse legendary and mythical registry IDs are normalized back to the existing CobbleTunes zone IDs, while the older flattened IDs remain valid as compatibility aliases. Terralith adds no new sound events: villages, huts, rubble, Mage structures, Spire, and underground landmarks reuse existing Village, Igloo, Trail Ruins, Mansion, End City, Stronghold, Jungle Pyramid, Mineshaft, Ocean Ruin, and related pools. Repurposed Structures likewise reuses existing pools. Legendary Monuments adds no new audio: Distortion Portal, Giratina Island, and Turnback Cave reuse the Sinnoh Distortion World theme, Lake Acuity, Lake Valor, and Lake Verity reuse Lake Caverns, and Stark Mountain reuses the existing Stark Mountain structure track.

### Low HP cue

The active battle Pokémon is checked periodically. When a living active Pokémon reaches 25% HP or lower, CobbleTunes plays the low-HP alert once.

The effect uses `0.80x` the configured music volume, so the low-HP cue plays at 80% of the current CobbleTunes music volume.

### Spatial evolution music

Evolution music is fully client-side. For visible world evolutions, every CobbleTunes client watches nearby synchronized `PokemonEntity` instances and reacts to `isEvolving` without a custom server packet. The suspense cue begins after a 20-tick delay so it lines up with Cobblemon's visible evolution animation, then stops when the synchronized evolution state returns to false and the regional completion sting plays from the same Pokémon position.

If an evolution is completed from Cobblemon's Summary UI while that Pokémon has no live world entity, CobbleTunes snapshots `CobblemonClient.storage.party` when the Summary opens and watches that synchronized party store for a same-UUID species change. It does not depend on the Summary screen's own Pokémon objects, which can remain stale during evolution. The watcher stays armed for 10 seconds after the Summary closes so a slightly delayed party sync is still caught. In that fallback path it skips suspense entirely and plays only the regional completion cue from the player's position. A live nearby world entity or already tracked spatial evolution suppresses the fallback to avoid duplicate completion audio.

World evolution sounds are positional under Minecraft's **Records/Jukebox** category instead of the global Music category. Suspense uses `0.62x` and completion uses `0.78x` the configured CobbleTunes music volume, with linear attenuation out to about 32 blocks. Multiple nearby evolutions can play independently from different directions. While any audible evolution cue is playing, including the completion sting, world ambience/structure music smoothly ducks to `0.20x` and stays there for the whole evolution sequence. Only after the final congratulations cue ends does the world soundtrack fade back to normal over 2 seconds. Battle, Victory, and menu music are not ducked. The Summary fallback completion is emitted from the player's position at the same `0.78x` completion volume, ducks world ambience to `0.20x` for the duration of the cue, and then uses the same 2-second restore fade. If a visible evolving state ends without the Pokémon species changing, CobbleTunes treats it as an interrupted evolution and does not play the congratulation sting.

Regional routing uses the evolving Pokémon's National Dex region, with Alolan, Galarian, Hisuian, and Paldean forms overriding the base species region. Alola randomly chooses `um_evo.ogg` or `um_evo2.ogg`, and Galar randomly chooses `swsh_evo.ogg` or `swsh_evo2.ogg`. The completion cue always uses that region's flat `<prefix>_congrat.ogg` file. Evolution assets are expected directly under `assets/cobbletunes/sounds/evolution/`, matching the resource pack filenames such as `fr_evo.ogg`, `bl_congrat.ogg`, and `sv_evo.ogg`. Missing evolution files are detected directly in the active resource packs, so a missing cue stays silent and does not duck the world soundtrack.

### Configuration

CobbleTunes creates two JSON files in `config/`. When Mod Menu is installed, the Configure button opens a native CobbleTunes screen for all client-side options below. The server debug option remains in the server JSON.

#### `cobbletunes-client.json`

| Option | Default |
|---|---:|
| `replaceAmbience` | `true` |
| `replaceMenuMusic` | `true` |
| `replaceBattleMusic` | `true` |
| `enableEvolutionMusic` | `true` |
| `musicVolume` | `1.0` |
| `crossfadeSeconds` | `2.5` |
| `shuffleAmbienceTracks` | `true` |
| `worldJoinSilenceSeconds` | `10` |
| `trackEndSilenceMinSeconds` | `90` |
| `trackEndSilenceMaxSeconds` | `180` |
| `biomeTransitionSilenceMinSeconds` | `4` |
| `biomeTransitionSilenceMaxSeconds` | `8` |
| `debugLogging` | `false` |

Setting either CobbleTunes `musicVolume` or Minecraft's **Music** volume slider to `0%` suspends CobbleTunes playback. Biome, structure, battle, and Victory targets continue updating, but ambience silence windows, biome debounce, village cooldowns, and track-rotation timers do not advance. Once both volume controls are above zero again, CobbleTunes resumes the latest valid context immediately without applying a leftover cooldown or world-join delay.

#### `cobbletunes-server.json`

| Option | Default |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

CobbleTunes does not include soundtrack files. Put your `.ogg` files under:

```text
assets/cobbletunes/sounds/
```

The included `sounds.json` defines all 441 expected sound events. [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) mirrors those entries and explains their routing.

Missing audio is handled as silence instead of crashing the music system. Evolution files are expected directly under `assets/cobbletunes/sounds/evolution/` using the flat filenames listed in the sound manifest, while `gamecorner/` entries remain empty slots until the matching OGG files are added to the resource pack.

### Project layout

- **`src/main/kotlin`** — common/server entrypoint, battle events, RCT classification, WildBosses and Raid Dens bridges, structure detection including Cobbleverse compatibility aliases, Terralith, CobblemonAdditions, Repurposed Structures, and Legendary Monuments mappings, manual zone markers, configs, and networking.
- **`src/client/kotlin`** — packet routing plus the client-only fallback classifier, region selection, Victory/Loot Menu bridge, local Raid Dens/WildBosses/RCT inference, the client-observed spatial evolution watcher, conservative structure fingerprints, ambience watching, music state, fades, menu music, low-HP handling, and the optional Mod Menu config screen.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — all sound keys and resource-pack paths.

### Building

Java 21 is required. From the complete project root:

```powershell
.\gradlew clean build
```

If your project uses the audio-exclusion build property:

```powershell
.\gradlew clean build -PexcludeAudio
```

The built JAR is placed in `build/libs/`.

### Audio and licensing

No Pokémon OST audio is distributed with CobbleTunes. Soundtrack files belong to their respective rights holders and must be supplied separately by the resource-pack user or pack maintainer.

### License

CobbleTunes source code is available under the MIT license.

---

## Português

### Visão geral

**CobbleTunes** é um framework de música dinâmica para Cobblemon em Fabric. Ele substitui a música do Minecraft por temas de batalha, ambientação regional, músicas de estruturas, temas de vitória, Battle Tower, Game Corner, menu, alerta de HP baixo e música espacial de evolução observada pelo cliente.

O mod contém apenas a lógica de roteamento e reprodução. Ele **não** inclui, baixa ou gera arquivos de OST de Pokémon. As músicas são fornecidas por um resource pack normal dentro de `assets/cobbletunes/sounds/`.

O código atual define **441 eventos de som** entre batalhas, vitória, Battle Tower, Game Corner, evolução, ambientação, menu e efeitos. A lista completa está em [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### O que ele faz

O CobbleTunes funciona primeiro pelo cliente. Em um servidor público de Cobblemon sem CobbleTunes, o cliente usa o estado da batalha que já recebe, resolve localmente a função e a região do adversário e toca o `SoundEvent` correspondente. Quando o servidor também possui CobbleTunes, os pacotes do servidor passam a ser a fonte autoritativa para dados que só existem no servidor, como a identidade exata de estruturas worldgen e zonas manuais.

```text
batalha do Cobblemon
→ classifica selvagem treinador pvp boss facção ou facility
→ resolve função e região
→ aplica forma e variante regional
→ usa dados locais do cliente ou uma rota autoritativa do servidor
→ toca o contexto musical correspondente
```

A prioridade da música no mundo funciona assim:

```text
batalha
↓
vitória e loot menu
↓
estrutura trigger e battle tower
↓
ambientação de bioma
```

As detecções de prioridade menor continuam atualizando em segundo plano enquanto batalha ou vitória controlam o áudio. Assim o CobbleTunes já sabe qual zona ou bioma deve voltar quando a música prioritária termina.

### Modos somente cliente e com servidor

O CobbleTunes pode ser instalado **somente no cliente** e usado em servidores públicos de Cobblemon que não possuem o mod. Nesse modo ele infere batalhas selvagens, treinadores, PvP, lendários/míticos, formas regionais, Victory, capturas, WildBosses, Raid Dens e dados observáveis do RCT a partir do estado que o cliente já recebe. Raid Dens também pode ser reconhecido pelo som nativo do tier da raid, e uma raid concluída no modo somente cliente dispara a mesma Victory regional de cinco segundos quando o boss desmaia, seguida por dois segundos em volume cheio antes do início do fade-out.

Ambientação de bioma, menu, HP baixo, música espacial de evolução, suspensão por volume e outros sistemas puramente locais continuam funcionando normalmente. A evolução nunca exige pacote do servidor do CobbleTunes: evoluções visíveis no mundo são observadas pelo estado sincronizado `PokemonEntity.isEvolving`, enquanto uma evolução concluída diretamente pela tela Summary do Cobblemon sem uma entidade viva próxima no mundo é detectada pelo armazenamento sincronizado da party no cliente e toca somente o cue regional de congratulação na posição do jogador. O watcher da Summary continua armado por alguns segundos depois que a tela fecha para não perder a sincronização final. Para estruturas, o modo standalone usa fingerprints conservadores para locais que o cliente consegue reconhecer com segurança, atualmente torres de Gimmighoul, Poké Centers, Ruined Portals e vilas. Os IDs exatos de `StructureStart` usados por Cobbleverse/Terralith/BCA/Repurposed/Legendary Monuments e as zonas manuais com markers continuam como recursos melhorados pelo servidor, pois o cliente remoto normal não recebe essas identidades autoritativas de estrutura.

Quando uma ponte de servidor do CobbleTunes está disponível, o cliente desativa automaticamente o classificador fallback e prefere os pacotes autoritativos já existentes. O servidor só envia esses pacotes para clientes que anunciam o canal correspondente do CobbleTunes, evitando disputa entre os dois caminhos. O lado servidor não registra blocos, itens ou entidades do CobbleTunes, então instalar o mesmo JAR no servidor não torna o mod obrigatório para os outros jogadores.

### Principais recursos

- [x] **Música regional de batalha** para Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui e Paldea.
- [x] **Contextos de selvagem, treinador, Líder de Ginásio, Elite Four, Campeão, rival, PvP, facção, Frontier Brain, Battle Tower e Lendário/Mítico.**
- [x] **Roteamento de formas regionais** para que formas de Alola, Galar, Hisui e Paldea usem a região da forma em vez da região da espécie base.
- [x] **Roteamento de lendários por forma** para casos como Kyurem, Necrozma, Eternatus, Calyrex, Terapagos e as aves de Galar.
- [x] **Classificação dinâmica do RCT** usando função, região, facção, rank, ID do treinador, progressão e temas exatos para treinadores personalizados nomeados.
- [x] **Temas de facções** para Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine e Ultra Recon Squad.
- [x] **Música de Frontier Brain** com contexto próprio.
- [x] **Integrações com WildBosses e Cobblemon Raid Dens** usando pools regionais ponderados por tier, detecção pelo Pokémon original do actor e Victory regional dedicado por 5 segundos, seguido por 2 segundos em volume cheio antes do fade-out ao concluir uma raid.
- [x] **Pools seguros para Bosses** sem usar temas lendários específicos em encontros aleatórios.
- [x] **Integração de vitória com Cobblemon Loot Menu** iniciando a Victory no desmaio decisivo do oponente, usando o cue curto normal quando não existe tela de loot, mantendo a música durante o menu e retornando para a zona atual depois.
- [x] **Temas de vitória ao capturar Pokémon** em capturas dentro ou fora de batalha, usando a região do Pokémon capturado e o pool de vitória selvagem já existente.
- [x] **Pools de Battle Tower** para andares baixos, médios, altos e finais com tema de batalha dedicado.
- [x] **Memória e rotação de ambientação por bioma** com intervalos de silêncio e debounce.
- [x] **Detecção subterrânea** usando luz do céu e altura do jogador.
- [x] **Estruturas exatas do Cobbleverse**, **estruturas vanilla**, **estruturas do Terralith**, **vilas do CobblemonAdditions/BCA**, **Repurposed Structures**, estruturas selecionadas do **Legendary Monuments** e **torres de Gimmighoul do Cobblemon** usando músicas já existentes.
- [x] **Zonas manuais de música** para Centros Pokémon, Poké Marts, Ginásios, Game Corners/Cassinos, locais especiais e andares da Battle Tower.
- [x] **Pool de Game Corner** com 15 faixas de FRLG, Emerald, HGSS e Platinum tocadas como uma playlist embaralhada sem repetição imediata.
- [x] **Música de menu** contínua entre os submenus da tela inicial.
- [x] **Alerta de HP baixo** tocado uma vez a 80% do volume configurado.
- [x] **Música espacial de evolução** emitida por Pokémon visíveis, com fallback da tela Summary que toca apenas a conclusão regional local quando não existe entidade no mundo.
- [x] **Tratamento de morte do jogador** e limpeza segura de estado do mundo.
- [x] **Logs de debug no cliente e servidor**, desligados por padrão.
- [x] **Modo somente cliente para servidores públicos** com detecção automática da ponte do servidor e dados autoritativos quando ela está disponível.
- [x] **Integração com Mod Menu** com tela nativa do CobbleTunes para as configurações de música do cliente.

### Requisitos

#### Obrigatórios

- Minecraft `1.21.1`
- Fabric Loader `0.17.2+`
- Fabric API
- Fabric Language Kotlin `1.13.3+`
- Cobblemon `1.7.3`
- Java `21`

Em multiplayer, instalar o CobbleTunes **somente no cliente já é suficiente** para a experiência standalone em um servidor público de Cobblemon. Instalar o mesmo JAR do CobbleTunes no servidor é opcional e habilita roteamento exato de estruturas/zonas manuais e metadados autoritativos para clientes que também possuem o mod. Jogadores sem CobbleTunes continuam podendo entrar porque a ponte não registra conteúdo próprio de gameplay e só envia payloads para clientes que anunciam suporte.

#### Integrações opcionais

- **Mod Menu** — abre uma tela nativa do CobbleTunes para as configurações de música do cliente.
- **Radical Cobblemon Trainers** — melhora a detecção de função, região, facção e progressão e permite temas exatos para treinadores personalizados suportados.
- **WildBosses** — ativa pools musicais próprios para Bosses.
- **Cobblemon Raid Dens** — batalhas de raid reutilizam os mesmos pools regionais ponderados. O modo somente cliente pode reconhecer o som nativo do tier da raid; o modo com servidor mantém os fallbacks pelos metadados do Pokémon ligado ao actor, ID/mapa de raids ativas e marcador da batalha.
- **Cobblemon Loot Menu** — ativa temas de vitória enquanto a tela de loot está aberta.
- **CobblemonAdditions / estruturas BCA** — mapeia as variantes atuais `4.1.6` dark, default e fighting para os pools pequenos, médios e grandes já existentes, e reutiliza o pool de Swamp Hut para a Witch Hut do BCA. Os IDs genéricos antigos continuam suportados.
- **Datapack Terralith** — mapeia todas as 26 estruturas referenciadas pelos structure sets fornecidos do Terralith para pools vanilla/BCA já existentes.
- **Repurposed Structures** — reaproveita os pools vanilla/BCA existentes para todos os 107 IDs de estruturas de worldgen presentes na versão `7.5.21+1.21.1`.
- **Legendary Monuments** — reutiliza músicas já existentes de Sinnoh e de estruturas para Distortion Portal, Giratina Island, Turnback Cave, os três lagos de Sinnoh e Stark Mountain.

Mod Menu, RCT, WildBosses, Cobblemon Raid Dens, Cobblemon Loot Menu, CobblemonAdditions, Repurposed Structures e Legendary Monuments são integrações opcionais de mods. O suporte ao Terralith é baseado no registro e só é ativado quando as estruturas do datapack existem.

### Roteamento de batalha

#### Contextos regionais

O CobbleTunes mantém a função separada da região. Um Líder de Ginásio de Kanto usa:

```text
leader|kanto
```

Um Frontier Brain de Sinnoh usa:

```text
frontier|sinnoh
```

Uma facção mantém o ID exato do tema:

```text
faction:team_galactic_commander|sinnoh
```

Quando os metadados do treinador informam uma região, ela tem prioridade. Caso contrário, a equipe adversária vota pela região.

#### Variantes regionais

O número da Pokédex Nacional não é suficiente para formas regionais. O pacote de batalha também envia os dados da variante regional.

Exemplos:

```text
exeggutor de alola → alola
grimer e muk de alola → alola
weezing de galar → galar
growlithe de hisui → hisui
wooper de paldea → paldea
```

A variante regional afeta temas selvagens, votação da equipe de treinadores, pools do WildBosses e Raid Dens, fallback de lendários e seleção da região da vitória.

#### Funções e facções do RCT

A integração com RCT usa reflexão. O CobbleTunes lê ID e tipo do treinador quando disponíveis sem transformar o RCT em dependência obrigatória.

Rotas reconhecidas incluem:

```text
normal
leader
e4
champ
rival
frontier
```

As famílias de facção reconhecidas incluem Team Rocket, Team Aqua, Team Magma, Team Galactic, Team Plasma, Team Flare, Team Skull, Aether Foundation e Ultra Recon Squad.

A detecção de facção acontece antes das regras amplas de função. Isso mantém Giovanni da Rocket separado de uma rota normal de Líder de Ginásio regional.

O datapack RCT Tower fornecido também possui overrides exatos de tema de batalha para os dez encontros nomeados `pokemon_trainer_*`. Os treinadores de andar como `f1_trainer1` até `f9_trainer10` ficam intencionalmente intactos e continuam usando a rota normal de treinador da Battle Tower.

| Treinador | Tema de batalha exato |
|---|---|
| Barry | Rival de Sinnoh |
| Gold | Remix de Campeão de Johto do BW World Tournament |
| Green | Remix de Campeão de Kanto do BW World Tournament |
| Kris | Remix de Campeão de Johto do BW World Tournament |
| May | Remix de Campeão de Hoenn do BW World Tournament |
| Morimoto | B2W2 PWT Final |
| Oak | Remix de Campeão de Kanto do BW World Tournament |
| Red | Remix de Campeão de Kanto do BW World Tournament |
| Silver | Rival de Johto |
| Steven | Campeão de Hoenn |

O override usa o ID exato do treinador e também aceita a forma equivalente com namespace `rctmod:` quando o RCT expõe o ID dessa maneira. Barry e Silver mantêm a rota de vitória de rival, Steven mantém a rota de vitória de Campeão e os outros treinadores nomeados mantêm a rota regional normal de vitória de treinador.

### Lendários e míticos

Temas dedicados são escolhidos por espécie e por forma quando necessário. Temas lendários regionais genéricos continuam disponíveis como fallback.

Os casos sensíveis à forma incluem:

- Articuno, Zapdos e Moltres de Galar;
- Kyurem base e Black/White Kyurem;
- Necrozma base, fundido e Ultra Necrozma;
- Eternatus e Eternamax Eternatus;
- Calyrex e suas formas Ice Rider / Shadow Rider;
- Terapagos e Stellar Terapagos.

O resource pack também possui temas dedicados para vários grupos lendários e míticos das regiões suportadas. O manifest contém o mapeamento completo.

### Integração com WildBosses e Cobblemon Raid Dens

WildBosses e Cobblemon Raid Dens são opcionais. Encontros reais do WildBosses e batalhas de Raid Dens usam o mesmo sistema regional de música ponderada. A integração continua apenas por reflexão e verifica primeiro o Pokémon original ligado ao actor no servidor, onde os metadados da raid já estão disponíveis quando o Cobblemon dispara o início da batalha. Se necessário, o CobbleTunes usa `crd_getRaidId()` e o mapa de raids ativas antes de recorrer ao marcador da própria batalha. O fim da raid também é lido pelo evento `RAID_END` do próprio Raid Dens, pois esse tipo de encontro não segue o ciclo normal de Victory do Cobblemon.

| Tier musical | Rival/PvP regional | Lendário genérico | BW World Tournament | Tier do Raid Dens |
|---|---:|---:|---:|---|
| Uncommon | 80% | 15% | 5% | 1 estrela |
| Rare | 70% | 20% | 10% | 2 estrelas |
| Epic | 60% | 30% | 10% | 3–4 estrelas |
| Legendary | 50% | 35% | 15% | 5–6 estrelas |
| Mythic | 45% | 40% | 15% | 7 estrelas |

A equipe do Pokémon da raid determina o pool regional da mesma forma que no WildBosses. Temas lendários específicos ficam fora do pool genérico de Lendários e o mod evita repetição imediata quando existe outra faixa válida.

Com o debug do servidor ativado, a integração escreve linhas compactas `[RaidDensCompat] candidate` e `resolved` indicando se o tier veio da entidade ligada ao actor, do mapa de raids ativas ou do fallback da batalha. Um `RAID_END` bem-sucedido usa o resolvedor regional de vitória selvagem por **5 segundos**, mantém o tema em volume cheio por mais **2 segundos** e então inicia o fade de volta para a ambientação válida mais recente. O tema continua durante a transição da dimensão da raid de volta ao overworld, sem ser cortado pelo silêncio de entrada no mundo. Raids perdidas apenas liberam a música de batalha, sem tocar Victory.

### Vitória, capturas e Cobblemon Loot Menu

A música de vitória de batalha mantém a integração leve com `cobblemon_loot_menu`. Capturas bem-sucedidas também ativam o tema regional de vitória selvagem mesmo quando a captura acontece fora de batalha.

Quando uma batalha suportada é vencida e o Cobblemon Loot Menu está instalado:

```text
vitória da batalha
→ último Pokémon oponente desmaia
→ Victory começa imediatamente
→ sem tela de loot: Victory usa o cue curto normal
→ tela de loot aparece: a mesma Victory continua tocando
→ Victory continua até a tela de loot fechar
```

O CobbleTunes não atrasa mais a Victory esperando descobrir se existe loot. O cliente observa as mensagens de batalha do Cobblemon e o HP sincronizado da equipe, então o cue regional de Victory começa assim que o desmaio decisivo do oponente é reportado. O cue normal tem duração base de cerca de três segundos e depois permanece em volume cheio por mais dois segundos antes do início do fade-out. Se a `LootSelectionScreen` aparecer durante a transição pós-batalha, a Victory atual passa a ser mantida até a tela fechar. Depois que a tela de loot fecha, a Victory também permanece em volume cheio por dois segundos antes do início do fade-out. Se o cue curto já tiver terminado antes de a tela de loot aparecer, o CobbleTunes reinicia imediatamente a mesma Victory para o menu. Sem o Cobblemon Loot Menu, o mesmo gatilho de desmaio decisivo é usado, mantendo o evento posterior de vitória do Cobblemon como fallback.

Uma captura bem-sucedida usa o mesmo roteamento regional de vitória selvagem com um cue base de cerca de três segundos, seguido por dois segundos em volume cheio antes do fade-out. Isso vale tanto para capturas que encerram uma batalha selvagem quanto para capturas diretas no mundo. Formas regionais continuam sobrescrevendo a região baseada na National Dex. A vitória de captura nunca espera pelo Loot Menu. Uma raid concluída com sucesso também usa o tema regional de vitória selvagem com um cue base de **cinco segundos**, seguido pelos mesmos **dois segundos** em volume cheio antes do fade-out. Com a ponte do servidor, a Victory da raid usa o `RAID_END` do Raid Dens. No modo somente cliente, o boss sincronizado chegando a 0 HP marca a conclusão, então o tema começa imediatamente sem esperar o fechamento da batalha ou o retorno ao overworld.

A detecção de bioma e estrutura continua funcionando durante a vitória. Quando a vitória termina, o CobbleTunes volta para a **última** estrutura, andar da Battle Tower ou bioma válido em vez de usar uma ambientação antiga salva antes da batalha.

Hisui não possui tema tradicional de vitória neste pack.

### Battle Tower

A ambientação da Battle Tower usa quatro pools:

```text
battle_tower_low
battle_tower_mid
battle_tower_high
battle_tower_final
```

IDs de trigger como `cobbletunes:battle_tower_floor_1` até `cobbletunes:battle_tower_floor_10` são convertidos nesses pools. Triggers próximos são verificados do mais próximo para o mais distante para evitar conflito vertical entre andares.

Uma batalha de treinador iniciada enquanto a zona atual é `BATTLE_TOWER` usa o tema de batalha da Battle Tower de Galar. No modo somente cliente, IDs de treinadores de andar do RCT como `f1_trainer1` e o restante do padrão da torre também usam diretamente esse mesmo tema sem serem tratados como overrides de treinadores especiais nomeados.

### Game Corner e zonas de Cassino

Um marker manual do servidor pode transformar uma construção própria em Game Corner ou Cassino sem registrar um bloco próprio do CobbleTunes. Invoque um `minecraft:marker` vanilla com a tag `cobbletunes_zone:<zoneId>` e use um destes IDs de zona:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Ao entrar na zona o mod inicia uma playlist embaralhada do Game Corner. A primeira faixa é aleatória e quando ela termina o CobbleTunes avança automaticamente para outra sem manter o mesmo arquivo em loop. Todas as faixas passam uma vez antes do pool ser embaralhado novamente e o novo ciclo evita repetir imediatamente a música que acabou de tocar. Batalha e vitória continuam com prioridade e depois delas a playlist do Game Corner continua enquanto a zona permanecer ativa.

O pool possui 15 arquivos esperados dentro de `assets/cobbletunes/sounds/gamecorner/`, divididos entre FRLG, Emerald, HGSS e Platinum. O áudio continua sendo fornecido separadamente pelo resource pack. Veja [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) para os nomes exatos e as sugestões de temas de origem.

As zonas manuais do servidor usam entidades vanilla `minecraft:marker` invisíveis em um raio de 12 blocos. Por exemplo, `/summon minecraft:marker 300 64 300 {Tags:["cobbletunes_zone:cobbletunes:game_corner"]}` cria uma âncora de Game Corner. Quando zonas se sobrepõem, o marker mais próximo vence. O antigo bloco customizado `cobbletunes:music_trigger` foi removido para que a instalação do CobbleTunes no servidor continue opcional para os clientes; mundos que usavam esse bloco devem converter as âncoras para markers antes de atualizar. Veja [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) para os exemplos completos.

### Ambientação e estruturas

A ambientação regional por bioma está definida para Kanto, Johto, Hoenn, Sinnoh e Unova. As faixas são agrupadas entre biomas vanilla e Terralith como planícies, florestas, cavernas, oceanos, montanhas, neve, desertos e regiões vulcânicas.

As músicas de bioma usam memória, orçamento de rotação, intervalos de silêncio e debounce. Cruzar um bioma muito pequeno não força imediatamente uma nova música caso o bioma mude novamente durante a janela de debounce.

Música de estrutura tem prioridade sobre ambientação de bioma. Com a ponte opcional do servidor, as fontes exatas suportadas incluem:

- todas as 71 estruturas registradas do Cobbleverse entre os datapacks principal, Johto, Hoenn e Sinnoh, incluindo os caminhos atuais `legendary/` e `mythical/`;
- estruturas vanilla como Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals e outras;
- todas as 26 estruturas do Terralith referenciadas pelos structure sets ativos, redirecionadas para os pools vanilla/BCA mais próximos;
- vilas dark, default e fighting do CobblemonAdditions `4.1.6` redirecionadas para os pools BCA pequenos, médios e grandes, além da Witch Hut reutilizando o pool de Swamp Hut;
- variantes do Repurposed Structures redirecionadas para o pool vanilla/BCA mais próximo;
- estruturas selecionadas do Legendary Monuments redirecionadas para músicas de Sinnoh/estruturas já existentes;
- as seis estruturas worldgen das torres de Gimmighoul do Cobblemon (`deserted`, `frozen`, `lush`, `rooted`, `sunscorched` e `temperate`) redirecionadas para um pool sombrio com Pokémon Tower, Lavender Town e Pokégear Unown já existentes;
- zonas manuais ancoradas por entidades vanilla `minecraft:marker` com a tag `cobbletunes_zone:<zoneId>`.

No modo com servidor, a detecção de estruturas resolve o `StructureStart` real pelas referências dos chunks próximos em vez de depender de o jogador já estar dentro da estrutura. Estruturas compactas de até 16×16 blocos agora recebem uma margem de 4 blocos em cada lado horizontal e 4 blocos na vertical; estruturas maiores mantêm 1 bloco horizontal e 2 blocos verticais. Assim, Ruined Portals, ruínas pequenas, cabanas e estruturas parecidas mantêm uma zona musical estável ao redor da construção visível em vez de voltar para a música do bioma depois de apenas um ou dois passos além da bounding box exata.

A maioria das músicas fixas de zona fica em loop até o jogador sair. Vilas funcionam de forma diferente: o tema escolhido toca uma vez, espera entre **5 e 60 segundos** e repete a mesma faixa enquanto o jogador continuar dentro da vila. Ao sair e entrar novamente o mod escolhe outra faixa quando o pool possui uma alternativa. Game Corner e Cassino também continuam sem loop individual e avançam pela playlist embaralhada de 15 faixas. Batalha e vitória assumem temporariamente o áudio sem apagar o estado atual da zona. Os IDs atuais de lendários e míticos do Cobbleverse são normalizados para os IDs de zona já existentes no CobbleTunes, enquanto os IDs achatados antigos continuam válidos como aliases de compatibilidade. Terralith não adiciona novos eventos de som: vilas, huts, rubble, estruturas Mage, Spire e locais subterrâneos reutilizam pools já existentes de Village, Igloo, Trail Ruins, Mansion, End City, Stronghold, Jungle Pyramid, Mineshaft, Ocean Ruin e outros. Repurposed Structures também reutiliza pools existentes. Legendary Monuments não adiciona novos áudios: Distortion Portal, Giratina Island e Turnback Cave reutilizam o tema Distortion World de Sinnoh, Lake Acuity, Lake Valor e Lake Verity reutilizam Lake Caverns, e Stark Mountain usa a faixa de estrutura Stark Mountain já existente.

### Alerta de HP baixo

O Pokémon ativo é verificado periodicamente durante a batalha. Quando um Pokémon ativo e vivo chega a 25% de HP ou menos, o CobbleTunes toca o alerta de HP baixo uma única vez.

O efeito usa `0.80x` o volume configurado para música, fazendo o alerta tocar a 80% do volume atual de música do CobbleTunes.

### Música espacial de evolução

A música de evolução é totalmente do cliente. Para evoluções visíveis no mundo, cada cliente com CobbleTunes observa os `PokemonEntity` sincronizados próximos e reage ao estado `isEvolving` sem pacote customizado do servidor. O suspense começa após 20 ticks para alinhar com o início da animação visual de evolução do Cobblemon, para quando o estado sincronizado volta para false e então toca o sting regional de conclusão na mesma posição do Pokémon.

Se uma evolução for concluída pela tela Summary do Cobblemon enquanto esse Pokémon não possui uma entidade viva no mundo, o CobbleTunes cria um snapshot de `CobblemonClient.storage.party` quando a Summary abre e observa esse armazenamento sincronizado da party por uma mudança de espécie no mesmo UUID. Ele não depende dos objetos de Pokémon guardados pela própria tela Summary, que podem continuar desatualizados durante a evolução. O watcher permanece armado por 10 segundos após a Summary fechar para capturar uma sincronização final atrasada. Nesse fallback ele não toca suspense e reproduz somente o cue regional de conclusão na posição do jogador. Uma entidade viva próxima ou uma evolução espacial já rastreada bloqueia o fallback para evitar áudio duplicado.

Os sons de evolução no mundo são posicionais e usam a categoria **Discos/Jukebox** do Minecraft em vez da categoria global de Música. O suspense usa `0.62x` e a conclusão `0.78x` do volume de música configurado no CobbleTunes, com atenuação linear até aproximadamente 32 blocos. Várias evoluções próximas podem tocar ao mesmo tempo em direções diferentes. Enquanto qualquer cue de evolução audível estiver tocando, incluindo a congratulação, a música de ambiente/estrutura do mundo reduz suavemente para `0.20x` e permanece nesse nível durante toda a sequência. Somente depois que o cue final de congratulação termina a música do mundo retorna ao volume normal com um fade de 2 segundos. Batalha, vitória e menu não recebem esse ducking. A conclusão fallback da Summary é emitida na posição do jogador com o mesmo volume de `0.78x`, reduz o ambiente para `0.20x` durante o cue e usa o mesmo fade de retorno de 2 segundos. Se o estado de evolução visível terminar sem a espécie do Pokémon mudar, o CobbleTunes trata como uma evolução interrompida e não toca o sting de congratulação.

O roteamento regional usa a região da Pokédex Nacional do Pokémon em evolução, com formas de Alola, Galar, Hisui e Paldea substituindo a região da espécie base. Alola escolhe aleatoriamente entre `um_evo.ogg` e `um_evo2.ogg`, e Galar entre `swsh_evo.ogg` e `swsh_evo2.ogg`. A conclusão sempre usa o arquivo plano `<prefix>_congrat.ogg` da mesma região. Os arquivos de evolução são esperados diretamente em `assets/cobbletunes/sounds/evolution/`, seguindo nomes como `fr_evo.ogg`, `bl_congrat.ogg` e `sv_evo.ogg`. Arquivos ausentes ficam em silêncio e não reduzem a música do mundo.

### Configuração

O CobbleTunes cria dois JSONs dentro de `config/`. Quando o Mod Menu está instalado, o botão Configure abre uma tela nativa do CobbleTunes com todas as opções do cliente abaixo. O debug do servidor continua no JSON do servidor.

#### `cobbletunes-client.json`

| Opção | Padrão |
|---|---:|
| `replaceAmbience` | `true` |
| `replaceMenuMusic` | `true` |
| `replaceBattleMusic` | `true` |
| `enableEvolutionMusic` | `true` |
| `musicVolume` | `1.0` |
| `crossfadeSeconds` | `2.5` |
| `shuffleAmbienceTracks` | `true` |
| `worldJoinSilenceSeconds` | `10` |
| `trackEndSilenceMinSeconds` | `90` |
| `trackEndSilenceMaxSeconds` | `180` |
| `biomeTransitionSilenceMinSeconds` | `4` |
| `biomeTransitionSilenceMaxSeconds` | `8` |
| `debugLogging` | `false` |

Definir `musicVolume` do CobbleTunes ou o controle **Música** do Minecraft em `0%` suspende a reprodução do CobbleTunes. Os alvos atuais de bioma, estrutura, batalha e vitória continuam sendo atualizados, mas intervalos de silêncio, debounce de bioma, cooldown de vila e timers de rotação não avançam. Quando os dois controles de volume voltam a ficar acima de zero, o contexto válido mais recente retorna imediatamente sem reaplicar cooldown antigo ou espera de entrada no mundo.

#### `cobbletunes-server.json`

| Opção | Padrão |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

O CobbleTunes não inclui arquivos de soundtrack. Coloque os `.ogg` dentro de:

```text
assets/cobbletunes/sounds/
```

O `sounds.json` incluído define todos os 441 eventos esperados. [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) espelha essas entradas e explica o roteamento.

Áudio ausente vira silêncio sem derrubar o sistema de música. Os arquivos de evolução são esperados diretamente em `assets/cobbletunes/sounds/evolution/` com os nomes planos listados no sound manifest, enquanto as entradas de `gamecorner/` continuam como espaços vazios até os OGGs correspondentes serem adicionados ao resource pack.

### Organização do projeto

- **`src/main/kotlin`** — inicialização comum/servidor, eventos de batalha, classificação do RCT, pontes com WildBosses e Raid Dens, detecção de estruturas incluindo aliases de compatibilidade do Cobbleverse e mapeamentos do Terralith, CobblemonAdditions, Repurposed Structures e Legendary Monuments, markers de zonas manuais, configs e rede.
- **`src/client/kotlin`** — roteamento dos pacotes e classificador fallback somente cliente, região, ponte de Victory/Loot Menu, inferência local de Raid Dens/WildBosses/RCT, watcher espacial de evolução observado pelo cliente, fingerprints conservadores de estruturas, observação de biomas, estado musical, fades, menu, HP baixo e a tela opcional de configuração do Mod Menu.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — todas as chaves e caminhos do resource pack.

### Compilação

Java 21 é obrigatório. Na raiz do projeto completo:

```powershell
.\gradlew clean build
```

Se o projeto estiver usando a opção de exclusão de áudio:

```powershell
.\gradlew clean build -PexcludeAudio
```

O JAR compilado fica em `build/libs/`.

### Áudio e licença

Nenhuma OST de Pokémon é distribuída com o CobbleTunes. As faixas pertencem aos respectivos detentores de direitos e devem ser fornecidas separadamente pelo usuário ou mantenedor do resource pack.

### Licença

O código-fonte do CobbleTunes está disponível sob a licença MIT.
