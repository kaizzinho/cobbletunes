# CobbleTunes — Music Framework 🎵🎮

![Version](https://img.shields.io/badge/version-1.0-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-Loader%200.17.2%2B-DBB69B?logo=minecraft&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Fabric%20Language%20Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Cobblemon](https://img.shields.io/badge/Cobblemon-1.8.1-3E8E41)
![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red)
![Audio](https://img.shields.io/badge/audio-not%20included-lightgrey)

*Read this in [English](#english) | Leia em [Português](#português)*

*If you're here for the resource pack, click [here](#resource-pack)* |
*Se você está aqui pelo resource pack, clique [aqui](#resource-pack-1)*
---

## English

### Overview

**CobbleTunes** is a dynamic music framework for Cobblemon on Fabric. It replaces Minecraft music with context-aware battle themes, regional ambience, structure music, Victory themes, Battle Tower music, Game Corner zones, title-screen tracks, low-HP cues, and client-observed spatial evolution music.

The mod ships the routing and playback system only. It does **not** include, download, or generate Pokémon OST files. Music is supplied by a normal Minecraft resource pack under `assets/cobbletunes/sounds/`.

The current source defines **441 sound events** across battle, Victory, Battle Tower, Game Corner, evolution, ambience, menu, and effects. The complete list is in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

This source targets **Cobblemon 1.8.1 on Minecraft 1.21.1**. The 1.8.1 patch does not change the battle-side, Victory, capture, Alpha metadata, client battle synchronization, evolution, or `StructureStart` APIs used by CobbleTunes, so no routing code rewrite is required. Cobblemon 1.8 Alpha Pokémon are now detected from their synchronized `isAlpha` metadata and use a boss-like regional weighted soundtrack route. The complete Cobblemon 1.8 worldgen structure registry is also mapped: 32 registered Habitat Structure IDs, 29 ruin IDs, three Shipwreck Coves, and three fishing boats. These routes reuse existing CobbleTunes structure pools, except the six Gimmighoul towers which retain their dedicated spooky pool.

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

CobbleTunes can be installed on the **client only** and used on public Cobblemon servers that do not run the mod. In that mode it infers standard wild, trainer, PvP, Legendary/Mythical, Alpha, regional-form, Victory, capture, WildBosses, Raid Dens, and observable RCT routes from client-visible Cobblemon/mod state. Raid Dens can also be identified from its native tier battle sound, and successful client-only raids trigger the same five-second regional Victory cue when the raid boss faints, followed by a two-second full-volume tail before fade-out begins.

World biome ambience, menu music, low-HP handling, spatial evolution music, volume suspension, and other purely client-side systems work normally. Evolution never needs a CobbleTunes server packet: visible world evolutions are observed from Cobblemon's synchronized `PokemonEntity.isEvolving` state, while an evolution completed directly from Cobblemon's Summary UI with no live world entity is detected from Cobblemon's synchronized client party storage and plays only the regional congratulation cue from the player's position. The Summary watch remains armed briefly after the screen closes so the final party sync is not missed. Client-only structure routing deliberately does **not** infer worldgen structures from ordinary player-placeable blocks. Bells do not identify villages, healing machines plus PCs do not identify Poké Centers, Gilded/Gimmighoul chests do not identify Gimmighoul towers, and portal-like obsidian/netherrack builds do not identify Ruined Portals. Exact `StructureStart` identities and manual marker zones remain server-enhanced features because vanilla chunk networking does not provide those authoritative structure identities to a normal remote client. This favors a safe biome fallback on public servers over false structure music inside player bases.

When a CobbleTunes server bridge is present, the client automatically stops its fallback classifier and prefers the existing server-authoritative packets. Server packets are sent only to clients that advertise the matching CobbleTunes payload channel, so the fallback and enhanced paths do not compete. The server side registers no CobbleTunes block, item, or entity content, so installing the same JAR on a server does not make CobbleTunes mandatory for other players.

### Key Features

- [x] **Regional battle music** for Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, and Paldea.
- [x] **Wild, trainer, Gym Leader, Elite Four, Champion, rival, PvP, faction, Frontier Brain, Battle Tower, and Legendary/Mythical contexts.**
- [x] **Regional form routing** so Alolan, Galarian, Hisuian, and Paldean forms use their form region instead of the base species region.
- [x] **Form-aware Legendary routing** for encounters such as Kyurem, Necrozma, Eternatus, Calyrex, Terapagos, and the Galarian birds.
- [x] **Dynamic RCT classification** with role, region, faction, rank, trainer ID, progression-aware routing, roster-majority routing for generic Battle Tower floor trainers, and lore-specific weighted pools for named custom trainers.
- [x] **Villain faction themes** for Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine, and Ultra Recon Squad routes.
- [x] **Frontier Brain music** with a dedicated battle context.
- [x] **WildBosses and Cobblemon Raid Dens integrations** with tier-weighted regional PvP, generic Legendary, and BW World Tournament pools, actor-entity Raid Dens detection, and a dedicated 5-second regional Victory cue plus a 2-second full-volume tail before fade-out when a raid is cleared.
- [x] **Cobblemon 1.8 Alpha Pokémon routing** using authoritative/synchronized Alpha metadata on both server-enhanced and client-only paths. Wild Alphas reuse the Boss selector with a Rare-like 70/20/10 regional PvP / generic Legendary / BW World Tournament mix; Raid Dens and WildBosses retain higher routing priority.
- [x] **Species-safe Boss pools** that keep unique Legendary encounter themes out of unrelated Boss fights.
- [x] **Victory + Cobblemon Loot Menu integration** with server-enhanced Victory starting only when the complete opposing battle side is defeated, safe client-only fallbacks, extension while the loot screen is open, and current-zone resume afterward.
- [x] **Capture Victory themes** for successful Pokémon captures, including captures made outside battle, using the captured Pokémon region and the existing wild Victory pool.
- [x] **Battle Tower floor pools** with low, mid, high, and final marker-driven ambience tiers; generic floor-trainer battles keep normal roster-based regional routing.
- [x] **Biome ambience memory and rotation** with silence windows, biome-transition debounce, and visit memory that expires after a random 3–6 minutes or 3 meaningful biome transitions.
- [x] **Underground ambience detection** using sky light and player height.
- [x] **Cobbleverse exact structures**, **vanilla structures**, **Terralith structures**, **CobblemonAdditions/BCA villages**, **Repurposed Structures**, all structures from the **LegendaryMonuments-Cobbleverse light build**, and the complete **Cobblemon 1.8 worldgen structure registry** using existing music pools.
- [x] **Hand-placed music zones** for Poké Centers, Poké Marts, Gyms, Game Corners/Casinos, special locations, and Battle Tower floors.
- [x] **Game Corner pool** with 15 FRLG, Emerald, HGSS, and Platinum tracks played as a shuffled no-repeat playlist.
- [x] **Title-screen music** that stays active across submenus and stops when a world loads.
- [x] **FancyMenu music override** that silences FancyMenu Music-channel themes while CobbleTunes owns title-screen music, without touching FancyMenu visuals or UI sounds.
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
- Fabric API `0.116.6+1.21.1`
- Fabric Language Kotlin `1.13.6+`
- Cobblemon `1.8.1`
- Java `21`

For multiplayer, installing CobbleTunes on the **client is enough** for the standalone experience on a public Cobblemon server. Installing the same CobbleTunes JAR on the server is optional and enables exact server-only structure/manual-zone routing and authoritative compatibility metadata for CobbleTunes clients. Players without CobbleTunes can still join because the server bridge registers no custom gameplay content and only sends CobbleTunes payloads to clients that advertise support.

Poké Center and Poké Mart music is **manual-zone-only**. Generated Center/Mart buildings inside villages keep the village soundtrack unless a CobbleTunes marker is deliberately placed there; player-built healers, PCs, counters, bells, chests, or portal-like builds never create automatic structure zones.

#### Optional integrations

- **Mod Menu** — opens a native CobbleTunes configuration screen for client music settings.
- **FancyMenu** — when CobbleTunes menu replacement is enabled, FancyMenu global menu tracks and Music-channel Audio elements are silenced so the two menu themes do not overlap. Visual layouts and UI sounds remain untouched.
- **Radical Cobblemon Trainers** — trainer role, region, faction, progression-aware routing, roster-majority regional themes for generic Battle Tower floor trainers, and lore-specific weighted pools for supported named custom trainers.
- **WildBosses** — Boss-specific weighted regional battle pools.
- **Cobblemon Raid Dens** — raid battles reuse the same regional weighted pools. Client-only mode can recognize the native tier battle sound; server-enhanced mode keeps the actor-backed Raid Dens metadata, raid ID/active raid map, and battle-marker fallbacks.
- **Cobblemon Loot Menu** — post-battle Victory music while the loot screen is active.
- **CobblemonAdditions / BCA structures** — maps the current `4.3.0` dark, default, fighting, fairy, and ice village variants into the existing small/mid/large pools, with the BCA Witch Hut reusing the Swamp Hut pool. Legacy generic BCA village IDs remain supported.
- **Terralith datapack** — maps all 26 structures referenced by the supplied Terralith structure sets into existing vanilla/BCA music pools.
- **Repurposed Structures** — reuses the existing vanilla/BCA music pools for all 107 worldgen structure IDs present in `7.5.21+1.21.1`.
- **Legendary Monuments (Cobbleverse/light build)** — targets the attached `LegendaryMonuments-Cobbleverse` build exactly: all 13 registered structures and its `distortion_world_biome` are mapped to existing CobbleTunes music. Full Legendary Monuments 8.1-only locations such as Plains of Death, Thalic structures, Dyna Plains, and the expanded monument set are intentionally not targeted until the Cobbleverse 1.8 migration.

Mod Menu, FancyMenu, RCT, WildBosses, Cobblemon Raid Dens, Cobblemon Loot Menu, CobblemonAdditions, Repurposed Structures, and Legendary Monuments are soft mod integrations. Terralith support is registry-driven and only activates when its datapack structures exist.

### Installation

1. Download the latest `cobbletunes-*.jar` from this repository's **Releases** page.
2. Install the required versions of Fabric Loader, Fabric API, Fabric Language Kotlin, and Cobblemon listed above.
3. Place the CobbleTunes JAR in your Minecraft `mods` folder.
4. Add a compatible CobbleTunes music resource pack.
5. For server-enhanced structure and manual-zone detection, place the same CobbleTunes JAR in the server `mods` folder. Server installation is optional for the client-only experience.

Mod Menu is optional. Without it, all client settings remain editable through `config/cobbletunes/client.json`.

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

The regional variant affects wild themes, trainer roster voting, Alpha/WildBosses/Raid Dens regional pools, Legendary fallback routing, and Victory region selection. Cobblemon 1.8's new Alolan Sandshrew/Sandslash, Galarian Yamask, and Hisuian Avalugg are covered automatically by the same form/aspect logic.

#### Alpha Pokémon

Cobblemon 1.8 exposes Alpha state directly through `Pokemon.isAlpha()` on the server and synchronized `PokemonProperties.isAlpha` in client battle data. CobbleTunes uses those flags directly; it does not infer Alpha status from scale, red eyes, particles, moves, or herd behaviour.

Only **wild** Alpha opponents receive the Alpha music route. Route priority is `Raid Dens → WildBosses → Alpha → ordinary trainer/wild`, so a WildBoss that also happens to be Alpha keeps its explicit WildBoss tier. Alpha uses the existing Boss weighted selector with an `ALPHA` profile equivalent to the Rare mix: **70% regional rival/PvP, 20% generic Legendary, 10% BW World Tournament**. Species-specific Legendary encounter tracks remain excluded from the generic Legendary pool. No new OGG files or sound events are required.

Alpha victories and Alpha captures continue to use the normal regional **wild Victory** resolver; Alpha changes the encounter soundtrack, not the post-battle/capture identity.

#### RCT trainer roles and factions

RCT integration is reflection-based. CobbleTunes reads the trainer ID and type when available, then classifies the encounter without making RCT a hard dependency. For the Cobblemon 1.8.1 development/runtime test environment, the validated local pair remains **RCT Mod 0.19.0-beta + RCT API 0.16.0-beta**.

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

The supplied RCT Tower datapack has lore-specific weighted pools for its ten named `pokemon_trainer_*` encounters. Generic floor trainers matching `f1_trainer*` through `f10_trainer*` are deliberately forced back to the ordinary trainer route: their opposing roster votes by Pokémon region and the majority region selects the normal regional trainer theme. This keeps repeated XP-farming battles varied instead of forcing a Battle Tower battle song.

| Trainer | Lore pool |
|---|---|
| Barry | 80% Sinnoh Rival, 20% Sinnoh BW World Tournament Champion remix |
| Gold | 60% Johto Champion, 40% Johto BW World Tournament Champion remix |
| Green | 60% Kanto Champion/Rival, 40% Kanto BW World Tournament Champion remix |
| Kris | 60% Johto Champion, 40% Johto BW World Tournament Champion remix |
| May | 50% Hoenn Rival, 25% Hoenn Champion, 25% Hoenn BW World Tournament Champion remix |
| Morimoto | 100% B2W2 PWT Final |
| Oak | 60% Kanto Champion/Rival, 40% Kanto BW World Tournament Champion remix |
| Red | 60% Johto Champion, 40% Johto BW World Tournament Champion remix |
| Silver | 100% Johto Rival |
| Steven | 60% Hoenn Champion, 40% Hoenn BW World Tournament Champion remix |

The override matches the exact trainer ID, including the equivalent `rctmod:`-namespaced form when RCT exposes it that way. Pools avoid an immediate repeat when another valid track exists. Barry and Silver keep rival Victory routing, Steven keeps Champion Victory routing, and the other named trainers keep normal regional trainer Victory routing.

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
| Alpha (fixed profile) | 70% | 20% | 10% | — |
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
→ complete opposing battle side is defeated
→ Victory starts immediately with the server bridge
→ client-only trainer/PvP waits for confirmed battle end
→ no loot screen: Victory uses the normal brief cue
→ loot screen appears: the same Victory keeps playing
→ Victory continues until the loot screen closes
```

CobbleTunes no longer treats a single fainted active Pokémon as proof that a trainer or PvP side has lost. With the optional server bridge, Cobblemon's `BATTLE_FAINTED` event only arms a short authoritative outcome watch. CobbleTunes waits for Showdown's `win` declaration and also verifies that every Pokémon in the complete server-side opposing `BattleSide` roster is at 0 HP before starting early Victory. This naturally covers multi-Pokémon NPC teams, Gym/Elite Four/Champion battles, PvP, doubles and 2v2/team battles, while avoiding false Victory after an intermediate faint or an unresolved simultaneous KO. Cobblemon's later `BATTLE_VICTORY` event remains the final fallback, and is ignored if the decisive-faint cue was already sent. On a public server without CobbleTunes, hidden trainer/PvP reserves are intentionally not guessed; those battles wait for the actual battle end, while ordinary wild battles may still use the fast visible-faint path. The normal cue has a three-second base duration, then stays at full volume for another two seconds before fade-out begins. If `LootSelectionScreen` appears during the post-battle transition, the current Victory is promoted into a held cue and continues until that screen closes. After the loot screen closes, Victory also remains at full volume for two seconds before fade-out begins.

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

Floor trigger IDs such as `cobbletunes:battle_tower_floor_1` through `cobbletunes:battle_tower_floor_10` resolve as **1–3 low, 4–6 mid, 7–9 high, and 10 final**. Battle Tower floor markers use the same **12-block default radius** as every other manual CobbleTunes zone. Markers are still checked nearest-first, so overlapping floor zones resolve to the closest eligible marker. Use `cobbletunes_range:<blocks>` only when you intentionally want to override the standard radius for a specific custom setup.

Battle Tower floor zones control **world ambience only**. They no longer force trainer battles to use the Galar Battle Tower battle theme. Generic `f*_trainer*` RCT floor trainers use ordinary roster-majority regional trainer routing, while the ten exact named `pokemon_trainer_*` encounters use their lore-specific weighted pools.

### Game Corner and Casino zones

A manual server marker can turn a custom build into a Game Corner or Casino without registering a custom CobbleTunes block. Summon a vanilla `minecraft:marker` with a `cobbletunes_zone:<zoneId>` tag and use either of these zone IDs:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Entering the zone starts a shuffled Game Corner playlist. The first track is random, and when it finishes CobbleTunes automatically advances to another track without looping the same file. Every track is played once before the pool reshuffles, and the reshuffle avoids immediately repeating the track that just finished. Battle and Victory music still have priority; when they end, the active Game Corner playlist continues while the zone remains active.

The pool has 15 expected files under `assets/cobbletunes/sounds/gamecorner/`, grouped across FRLG, Emerald, HGSS, and Platinum. The resource pack audio is still supplied separately; the source only registers the events and routing. See [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) for the exact filenames and source-theme suggestions.

Manual server zones use invisible vanilla marker entities with a default 12-block radius. An optional `cobbletunes_range:<blocks>` tag can override that radius per marker from 1–32 blocks; invalid or missing values fall back to 12. For example, `/summon minecraft:marker 300 64 300 {Tags:["cobbletunes_zone:cobbletunes:game_corner","cobbletunes_range:12"]}` creates a Game Corner anchor. The nearest marker whose own radius actually reaches the player wins when zones overlap. Multiple markers resolving to the same zone ID do not restart or reshuffle the current zone music. The old `cobbletunes:music_trigger` custom block was removed so a server-side CobbleTunes install stays optional for clients; worlds that used that old block should convert those anchors to markers before updating. See [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) for the full marker setup examples.

### Ambience and structures

Regional biome ambience is currently defined for Kanto, Johto, Hoenn, Sinnoh, and Unova. Tracks are mapped to vanilla and Terralith biome groups such as plains, forests, caves, oceans, mountains, snow, deserts, and volcanic areas.

Biome tracks use memory, rotation budgets, silence ranges, and transition debounce. Crossing a tiny biome does not immediately force a new track if the biome changes again during the debounce window. When a resolved biome is left, its current track is remembered for a random **3–6 minutes**. Returning before that timer expires keeps the remembered track, unless the player has already committed transitions through **3 other biomes**. Once either limit is reached, the biome picks a fresh track and avoids the previous one whenever that pool has another valid option.

Structure music has higher priority than biome ambience. With the optional server bridge, supported exact sources include:

- all 71 registered Cobbleverse structures across the main, Johto, Hoenn, and Sinnoh datapacks, including the current nested `legendary/` and `mythical/` registry paths;
- vanilla structures such as Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals, and more;
- all 26 Terralith structures referenced by its active structure sets, aliased into the closest existing vanilla/BCA pools;
- CobblemonAdditions `4.3.0` dark, default, fighting, fairy, and ice villages mapped into the existing BCA small/mid/large pools, plus its Witch Hut mapped to the Swamp Hut pool;
- Repurposed Structures variants mapped back into the closest existing vanilla/BCA pool;
- all 13 structures registered by the LegendaryMonuments-Cobbleverse light build mapped to existing regional/structure tracks;
- all **32 registered Cobblemon 1.8 Habitat Structure IDs**, mapped by environment/theme into existing Trail Ruins, Jungle Pyramid, Igloo, Ocean Ruin, Desert Pyramid, Swamp Hut, Mineshaft Mesa, and related pools;
- all **29 Cobblemon ruin IDs**: the six Gimmighoul towers (`deserted`, `frozen`, `lush`, `rooted`, `sunscorched`, and `temperate`) keep the dedicated creepy Pokémon Tower/Lavender Town/Pokégear Unown pool, while the other 23 ruins reuse the closest existing structure pool;
- all three Cobblemon **Shipwreck Coves**, including 1.8's `magma_shipwreck_cove`, plus all three fishing-boat Structure IDs, reuse the existing Shipwreck pool;
- manual zones anchored by vanilla `minecraft:marker` entities tagged with `cobbletunes_zone:<zoneId>`.

Cobblemon's new **Habitat Block** is intentionally not treated as an automatic music detector. It is a placeable/spawner block, while CobbleTunes structure music remains tied to real worldgen `StructureStart` identities; custom Habitat Block areas can still use a manual `cobbletunes_zone:` marker if desired.

In server-enhanced mode, structure detection resolves the real registered `StructureStart` from nearby chunk references, matching the same worldgen identity concept used by commands such as `/locate structure`. Compact structures up to 16×16 blocks receive a 4-block margin on every horizontal side and 4 blocks vertically; larger structures keep a 1-block horizontal and 2-block vertical margin. When mapped structures overlap, special structures beat generic structures, generic structures beat villages, and equal-priority overlaps prefer the smaller structure before falling back to registry-ID order. Debug logging reports the authoritative registry ID and selected CobbleTunes zone. This gives structures stable, deterministic routing without using player-placeable blocks as evidence.

Most fixed zone music loops until the player leaves the zone. Villages are intentionally different: the selected village theme plays once, waits a random **5–60 seconds**, then replays the same theme while the player remains inside. Leaving and re-entering selects another theme when the pool has an alternative. Game Corner and Casino zones also remain non-looping and advance through their shuffled 15-track playlist. Battle and Victory music temporarily take priority without discarding the current zone state. Current Cobbleverse legendary and mythical registry IDs are normalized back to the existing CobbleTunes zone IDs, while the older flattened IDs remain valid as compatibility aliases. Terralith adds no new sound events: villages, huts, rubble, Mage structures, Spire, and underground landmarks reuse existing Village, Igloo, Trail Ruins, Mansion, End City, Stronghold, Jungle Pyramid, Mineshaft, Ocean Ruin, and related pools. Repurposed Structures likewise reuses existing pools. The LegendaryMonuments-Cobbleverse light build also adds no new audio: Distortion Portal, Giratina Island, Turnback Cave, and Eternatus Cocoon reuse the Sinnoh Distortion World theme; Lake Acuity, Lake Valor, and Lake Verity reuse Lake Caverns; Stark Mountain reuses the existing Stark Mountain structure track; Firescourge, Grasswither, Groundblight, and Icerend shrines reuse Mt. Chimney, Ecruteak, Underground Ruins, and Ice Path respectively; Outskirt Stand reuses Hoenn Route 111. Its only custom biome, `legendarymonuments:distortion_world_biome`, directly reuses the Sinnoh Distortion World ambience.

### Low HP cue

The active battle Pokémon is checked periodically. When a living active Pokémon reaches 25% HP or lower, CobbleTunes plays the low-HP alert once.

The effect uses `0.80x` the configured music volume, so the low-HP cue plays at 80% of the current CobbleTunes music volume.

### Spatial evolution music

Evolution music is fully client-side. For visible world evolutions, every CobbleTunes client watches nearby synchronized `PokemonEntity` instances and reacts to `isEvolving` without a custom server packet. The suspense cue begins after a 20-tick delay so it lines up with Cobblemon's visible evolution animation, then stops when the synchronized evolution state returns to false and the regional completion sting plays from the same Pokémon position.

If an evolution is completed from Cobblemon's Summary UI while that Pokémon has no live world entity, CobbleTunes snapshots `CobblemonClient.storage.party` when the Summary opens and watches that synchronized party store for a same-UUID species change. It does not depend on the Summary screen's own Pokémon objects, which can remain stale during evolution. The watcher stays armed for 10 seconds after the Summary closes so a slightly delayed party sync is still caught. In that fallback path it skips suspense entirely and plays only the regional completion cue from the player's position. A live nearby world entity or already tracked spatial evolution suppresses the fallback to avoid duplicate completion audio.

World evolution sounds are positional under Minecraft's **Records/Jukebox** category instead of the global Music category. Suspense uses `0.62x` and completion uses `0.78x` the configured CobbleTunes music volume, with linear attenuation out to about 32 blocks. Multiple nearby evolutions can play independently from different directions. While any audible evolution cue is playing, including the completion sting, world ambience/structure music smoothly ducks to `0.20x` and stays there for the whole evolution sequence. Only after the final congratulations cue ends does the world soundtrack fade back to normal over 2 seconds. Battle, Victory, and menu music are not ducked. The Summary fallback completion is emitted from the player's position at the same `0.78x` completion volume, ducks world ambience to `0.20x` for the duration of the cue, and then uses the same 2-second restore fade. If a visible evolving state ends without the Pokémon species changing, CobbleTunes treats it as an interrupted evolution and does not play the congratulation sting.

Regional routing uses the evolving Pokémon's National Dex region, with Alolan, Galarian, Hisuian, and Paldean forms overriding the base species region. Alola randomly chooses `um_evo.ogg` or `um_evo2.ogg`, and Galar randomly chooses `swsh_evo.ogg` or `swsh_evo2.ogg`. The completion cue always uses that region's flat `<prefix>_congrat.ogg` file. Evolution assets are expected directly under `assets/cobbletunes/sounds/evolution/`, matching the resource pack filenames such as `fr_evo.ogg`, `bl_congrat.ogg`, and `sv_evo.ogg`. Missing evolution files are detected directly in the active resource packs, so a missing cue stays silent and does not duck the world soundtrack.

### Configuration

CobbleTunes creates its JSON files inside `config/cobbletunes/`. Mod Menu is optional: when installed, its Configure button edits the same client JSON shown below. Without Mod Menu, players can edit every client option directly in `config/cobbletunes/client.json`. The server debug option remains in `config/cobbletunes/server.json`. Existing loose `cobbletunes-client.json` and `cobbletunes-server.json` files are migrated automatically on the next launch.

#### `config/cobbletunes/client.json`

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

#### `config/cobbletunes/server.json`

| Option | Default |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

[Click here to download the Cobbletunes resourcepack](https://mega.nz/file/zeIwST5L#_D9raffsmXnsZ1KKtpSYw0_-Nhak7XhcO89OG8Pf468)

### Project layout

- **`src/main/kotlin`** — common/server entrypoint, battle events, RCT classification, WildBosses and Raid Dens bridges, structure detection including Cobbleverse compatibility aliases, Terralith, CobblemonAdditions, Repurposed Structures, and Legendary Monuments mappings, manual zone markers, configs, and networking.
- **`src/client/kotlin`** — packet routing plus the client-only battle classifier, region selection, Victory/Loot Menu bridge, local Raid Dens/WildBosses/RCT inference, the client-observed spatial evolution watcher, biome ambience watching, music state, fades, menu music, low-HP handling, and the optional Mod Menu config screen. Worldgen structure identity is intentionally server-authoritative.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — all sound keys and resource-pack paths.

### License

CobbleTunes is **All Rights Reserved (ARR)**. The original, unmodified mod may be used on public or private servers and redistributed as part of public or private modpacks without asking permission. Standalone reuploads or mirrors are not allowed. Copying or reusing the project source/assets, or publicly distributing modified builds, forks, derivative works, or altered binaries, requires prior permission. See [`LICENSE`](./LICENSE) for the full terms.

---

## Português

### Visão geral

**CobbleTunes** é um framework de música dinâmica para Cobblemon em Fabric. Ele substitui a música do Minecraft por temas de batalha, ambientação regional, músicas de estruturas, temas de vitória, Battle Tower, Game Corner, menu, alerta de HP baixo e música espacial de evolução observada pelo cliente.

O mod contém apenas a lógica de roteamento e reprodução. Ele **não** inclui, baixa ou gera arquivos de OST de Pokémon. As músicas são fornecidas por um resource pack normal dentro de `assets/cobbletunes/sounds/`.

O código atual define **441 eventos de som** entre batalhas, vitória, Battle Tower, Game Corner, evolução, ambientação, menu e efeitos. A lista completa está em [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

Este código tem como alvo **Cobblemon 1.8.1 no Minecraft 1.21.1**. O patch 1.8.1 não altera as APIs de lados da batalha, Victory, captura, metadados Alpha, sincronização de batalha no cliente, evolução ou `StructureStart` usadas pelo CobbleTunes, então não é necessário reescrever o roteamento. Pokémon Alpha do Cobblemon 1.8 agora são detectados pelos metadados sincronizados `isAlpha` e usam uma rota regional ponderada semelhante à de Boss. O registro worldgen do Cobblemon 1.8 também está completamente mapeado: 32 IDs registrados de estruturas Habitat, 29 IDs de ruínas, três Shipwreck Coves e três barcos de pesca. Essas rotas reutilizam pools de estrutura já existentes, exceto pelas seis torres de Gimmighoul que mantêm seu pool sombrio dedicado.

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

O CobbleTunes pode ser instalado **somente no cliente** e usado em servidores públicos de Cobblemon que não possuem o mod. Nesse modo ele infere batalhas selvagens, treinadores, PvP, lendários/míticos, Alpha, formas regionais, Victory, capturas, WildBosses, Raid Dens e dados observáveis do RCT a partir do estado que o cliente já recebe. Raid Dens também pode ser reconhecido pelo som nativo do tier da raid, e uma raid concluída no modo somente cliente dispara a mesma Victory regional de cinco segundos quando o boss desmaia, seguida por dois segundos em volume cheio antes do início do fade-out.

Ambientação de bioma, menu, HP baixo, música espacial de evolução, suspensão por volume e outros sistemas puramente locais continuam funcionando normalmente. A evolução nunca exige pacote do servidor do CobbleTunes: evoluções visíveis no mundo são observadas pelo estado sincronizado `PokemonEntity.isEvolving`, enquanto uma evolução concluída diretamente pela tela Summary do Cobblemon sem uma entidade viva próxima no mundo é detectada pelo armazenamento sincronizado da party no cliente e toca somente o cue regional de congratulação na posição do jogador. O watcher da Summary continua armado por alguns segundos depois que a tela fecha para não perder a sincronização final. No modo somente cliente, o roteamento de estruturas deliberadamente **não** infere estruturas worldgen a partir de blocos comuns colocáveis pelo jogador. Sinos não identificam vilas, healing machines com PCs não identificam Poké Centers, Gilded/Gimmighoul chests não identificam torres de Gimmighoul e construções de obsidian/netherrack não identificam Ruined Portals. Os IDs exatos de `StructureStart` e as zonas manuais com markers continuam como recursos melhorados pelo servidor, pois um cliente remoto normal não recebe essas identidades autoritativas. Em servidores públicos, o CobbleTunes prefere cair para a música do bioma em vez de tocar uma estrutura falsa dentro da base de um jogador.

Quando uma ponte de servidor do CobbleTunes está disponível, o cliente desativa automaticamente o classificador fallback e prefere os pacotes autoritativos já existentes. O servidor só envia esses pacotes para clientes que anunciam o canal correspondente do CobbleTunes, evitando disputa entre os dois caminhos. O lado servidor não registra blocos, itens ou entidades do CobbleTunes, então instalar o mesmo JAR no servidor não torna o mod obrigatório para os outros jogadores.

### Principais recursos

- [x] **Música regional de batalha** para Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui e Paldea.
- [x] **Contextos de selvagem, treinador, Líder de Ginásio, Elite Four, Campeão, rival, PvP, facção, Frontier Brain, Battle Tower e Lendário/Mítico.**
- [x] **Roteamento de formas regionais** para que formas de Alola, Galar, Hisui e Paldea usem a região da forma em vez da região da espécie base.
- [x] **Roteamento de lendários por forma** para casos como Kyurem, Necrozma, Eternatus, Calyrex, Terapagos e as aves de Galar.
- [x] **Classificação dinâmica do RCT** usando função, região, facção, rank, ID do treinador, progressão, maioria regional do time para treinadores genéricos da Battle Tower e pools ponderados baseados no lore para treinadores personalizados nomeados.
- [x] **Temas de facções** para Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine e Ultra Recon Squad.
- [x] **Música de Frontier Brain** com contexto próprio.
- [x] **Integrações com WildBosses e Cobblemon Raid Dens** usando pools regionais ponderados por tier, detecção pelo Pokémon original do actor e Victory regional dedicado por 5 segundos, seguido por 2 segundos em volume cheio antes do fade-out ao concluir uma raid.
- [x] **Roteamento de Pokémon Alpha do Cobblemon 1.8** usando metadados Alpha autoritativos/sincronizados no servidor e no modo somente cliente. Alphas selvagens reutilizam o seletor de Boss com mistura 70/20/10 equivalente a Rare; Raid Dens e WildBosses mantêm prioridade maior.
- [x] **Pools seguros para Bosses** sem usar temas lendários específicos em encontros aleatórios.
- [x] **Integração de vitória com Cobblemon Loot Menu** iniciando a Victory antecipada no servidor somente quando todo o lado adversário foi derrotado, usando fallbacks seguros no modo somente cliente, mantendo a música durante o menu e retornando para a zona atual depois.
- [x] **Temas de vitória ao capturar Pokémon** em capturas dentro ou fora de batalha, usando a região do Pokémon capturado e o pool de vitória selvagem já existente.
- [x] **Pools de Battle Tower** para ambientação de andares baixos, médios, altos e finais controlados por markers; batalhas contra treinadores genéricos continuam usando roteamento regional pelo time adversário.
- [x] **Memória e rotação de ambientação por bioma** com intervalos de silêncio, debounce e memória de visita que expira após 3–6 minutos aleatórios ou 3 transições significativas de bioma.
- [x] **Detecção subterrânea** usando luz do céu e altura do jogador.
- [x] **Estruturas exatas do Cobbleverse**, **estruturas vanilla**, **estruturas do Terralith**, **vilas do CobblemonAdditions/BCA**, **Repurposed Structures**, todas as estruturas da versão leve **LegendaryMonuments-Cobbleverse** e o registro worldgen completo do **Cobblemon 1.8** usando pools musicais já existentes.
- [x] **Zonas manuais de música** para Centros Pokémon, Poké Marts, Ginásios, Game Corners/Cassinos, locais especiais e andares da Battle Tower.
- [x] **Pool de Game Corner** com 15 faixas de FRLG, Emerald, HGSS e Platinum tocadas como uma playlist embaralhada sem repetição imediata.
- [x] **Música de menu** contínua entre os submenus da tela inicial.
- [x] **Override de música do FancyMenu** que silencia temas do FancyMenu no canal Music enquanto o CobbleTunes controla a música da tela inicial, sem alterar visuais ou sons de interface do FancyMenu.
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
- Fabric API `0.116.6+1.21.1`
- Fabric Language Kotlin `1.13.6+`
- Cobblemon `1.8.1`
- Java `21`

Em multiplayer, instalar o CobbleTunes **somente no cliente já é suficiente** para a experiência standalone em um servidor público de Cobblemon. Instalar o mesmo JAR do CobbleTunes no servidor é opcional e habilita roteamento exato de estruturas/zonas manuais e metadados autoritativos para clientes que também possuem o mod. Jogadores sem CobbleTunes continuam podendo entrar porque a ponte não registra conteúdo próprio de gameplay e só envia payloads para clientes que anunciam suporte.

Música de Poké Center e Poké Mart é **exclusiva de zonas manuais**. Centros/Marts gerados dentro de vilas mantêm a música da vila, a menos que um marker do CobbleTunes seja colocado de propósito; healers, PCs, balcões, sinos, chests ou construções parecidas com portais feitas pelo jogador nunca criam zonas automáticas de estrutura.

#### Integrações opcionais

- **Mod Menu** — abre uma tela nativa do CobbleTunes para as configurações de música do cliente.
- **FancyMenu** — quando a substituição de música de menu do CobbleTunes está ativa, faixas globais e elementos Audio do FancyMenu no canal Music são silenciados para evitar duas músicas ao mesmo tempo. Layouts visuais e sons de interface não são alterados.
- **Radical Cobblemon Trainers** — melhora a detecção de função, região, facção e progressão, usa a maioria regional do time para treinadores genéricos da Battle Tower e pools ponderados baseados no lore para treinadores personalizados suportados.
- **WildBosses** — ativa pools musicais próprios para Bosses.
- **Cobblemon Raid Dens** — batalhas de raid reutilizam os mesmos pools regionais ponderados. O modo somente cliente pode reconhecer o som nativo do tier da raid; o modo com servidor mantém os fallbacks pelos metadados do Pokémon ligado ao actor, ID/mapa de raids ativas e marcador da batalha.
- **Cobblemon Loot Menu** — ativa temas de vitória enquanto a tela de loot está aberta.
- **CobblemonAdditions / estruturas BCA** — mapeia as variantes atuais `4.3.0` dark, default, fighting, fairy e ice para os pools pequenos, médios e grandes já existentes, e reutiliza o pool de Swamp Hut para a Witch Hut do BCA. Os IDs genéricos antigos continuam suportados.
- **Datapack Terralith** — mapeia todas as 26 estruturas referenciadas pelos structure sets fornecidos do Terralith para pools vanilla/BCA já existentes.
- **Repurposed Structures** — reaproveita os pools vanilla/BCA existentes para todos os 107 IDs de estruturas de worldgen presentes na versão `7.5.21+1.21.1`.
- **Legendary Monuments (versão Cobbleverse/light)** — suporta exatamente o `LegendaryMonuments-Cobbleverse` anexado: todas as 13 estruturas registradas e o bioma `distortion_world_biome` usam músicas já existentes. Locais exclusivos do Legendary Monuments 8.1 completo, como Plains of Death, estruturas Thalic, Dyna Plains e o conjunto expandido de monumentos, ficam intencionalmente fora desta versão até a migração do Cobbleverse para 1.8.

Mod Menu, FancyMenu, RCT, WildBosses, Cobblemon Raid Dens, Cobblemon Loot Menu, CobblemonAdditions, Repurposed Structures e Legendary Monuments são integrações opcionais de mods. O suporte ao Terralith é baseado no registro e só é ativado quando as estruturas do datapack existem.

### Instalação

1. Baixe o `cobbletunes-*.jar` mais recente na página **Releases** deste repositório.
2. Instale as versões obrigatórias do Fabric Loader, Fabric API, Fabric Language Kotlin e Cobblemon listadas acima.
3. Coloque o JAR do CobbleTunes na pasta `mods` do Minecraft.
4. Adicione separadamente um resource pack de música compatível com o CobbleTunes; as faixas não são incluídas no mod.
5. Para detecção aprimorada de estruturas e zonas manuais pelo servidor, coloque o mesmo JAR do CobbleTunes na pasta `mods` do servidor. A instalação no servidor é opcional para o modo somente cliente.

O Mod Menu é opcional. Sem ele, todas as configurações do cliente continuam disponíveis em `config/cobbletunes/client.json`.

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

A variante regional afeta temas selvagens, votação da equipe de treinadores, pools de Alpha/WildBosses/Raid Dens, fallback de lendários e seleção da região da vitória. As novas formas Sandshrew/Sandslash de Alola, Yamask de Galar e Avalugg de Hisui do Cobblemon 1.8 são cobertas automaticamente pela mesma lógica de forma/aspects.

#### Pokémon Alpha

O Cobblemon 1.8 expõe o estado Alpha diretamente por `Pokemon.isAlpha()` no servidor e por `PokemonProperties.isAlpha` sincronizado nos dados de batalha do cliente. O CobbleTunes usa esses flags diretamente; não tenta deduzir Alpha por escala, olhos vermelhos, partículas, golpes ou comportamento de manada.

Somente oponentes Alpha **selvagens** recebem essa rota. A prioridade é `Raid Dens → WildBosses → Alpha → treinador/selvagem comum`, então um WildBoss que também seja Alpha mantém seu tier explícito de WildBoss. Alpha reutiliza o seletor ponderado de Boss com perfil `ALPHA` equivalente a Rare: **70% rival/PvP regional, 20% Lendário genérico e 10% BW World Tournament**. Temas exclusivos de espécies Lendárias continuam fora do pool genérico. Nenhum OGG ou evento de som novo é necessário.

Vitórias e capturas de Alpha continuam usando o resolvedor regional normal de **Victory selvagem**; Alpha altera a música do encontro, não a identidade pós-batalha/captura.

#### Funções e facções do RCT

A integração com RCT usa reflexão. O CobbleTunes lê ID e tipo do treinador quando disponíveis sem transformar o RCT em dependência obrigatória. Para o ambiente de desenvolvimento/teste em Cobblemon 1.8.1, o par local validado continua sendo **RCT Mod 0.19.0-beta + RCT API 0.16.0-beta**.

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

O datapack RCT Tower fornecido usa pools ponderados baseados no lore para os dez encontros nomeados `pokemon_trainer_*`. Treinadores genéricos de andar que seguem `f1_trainer*` até `f10_trainer*` são forçados para a rota de treinador comum: o time adversário vota pela região de cada Pokémon e a região majoritária escolhe o tema regional normal. Isso mantém as batalhas repetidas de farm de XP variadas em vez de forçar uma música única da Battle Tower.

| Treinador | Pool baseado no lore |
|---|---|
| Barry | 80% Rival de Sinnoh, 20% remix de Campeão de Sinnoh do BW World Tournament |
| Gold | 60% Campeão de Johto, 40% remix de Campeão de Johto do BW World Tournament |
| Green | 60% Campeão/Rival de Kanto, 40% remix de Campeão de Kanto do BW World Tournament |
| Kris | 60% Campeão de Johto, 40% remix de Campeão de Johto do BW World Tournament |
| May | 50% Rival de Hoenn, 25% Campeão de Hoenn, 25% remix de Campeão de Hoenn do BW World Tournament |
| Morimoto | 100% B2W2 PWT Final |
| Oak | 60% Campeão/Rival de Kanto, 40% remix de Campeão de Kanto do BW World Tournament |
| Red | 60% Campeão de Johto, 40% remix de Campeão de Johto do BW World Tournament |
| Silver | 100% Rival de Johto |
| Steven | 60% Campeão de Hoenn, 40% remix de Campeão de Hoenn do BW World Tournament |

O override usa o ID exato do treinador e também aceita a forma equivalente com namespace `rctmod:` quando o RCT expõe o ID dessa maneira. Os pools evitam repetir imediatamente a mesma faixa quando existe outra opção válida. Barry e Silver mantêm a rota de vitória de rival, Steven mantém a rota de vitória de Campeão e os outros treinadores nomeados mantêm a rota regional normal de vitória de treinador.

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
| Alpha (perfil fixo) | 70% | 20% | 10% | — |
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
→ todo o lado adversário é derrotado
→ com a ponte do servidor, Victory começa assim que o resultado é confirmado
→ treinador/PvP somente cliente espera o fim confirmado da batalha
→ sem tela de loot: Victory usa o cue curto normal
→ tela de loot aparece: a mesma Victory continua tocando
→ Victory continua até a tela de loot fechar
```

O CobbleTunes não trata mais o desmaio de um único Pokémon ativo como prova de que um treinador ou lado PvP perdeu. Com a ponte opcional do servidor, `BATTLE_FAINTED` apenas arma uma verificação curta do resultado. O CobbleTunes espera a declaração `win` autoritativa do Showdown e também confirma que todos os Pokémon do `BattleSide` adversário completo estão com 0 HP antes de iniciar a Victory antecipada. Isso cobre equipes com vários Pokémon, Líderes de Ginásio, Elite Four, Campeões, PvP, doubles e batalhas 2v2/em equipe, sem disparar Victory após um desmaio intermediário ou antes da resolução de um KO simultâneo. Em servidores públicos sem CobbleTunes, as reservas ocultas de treinadores/PvP não são adivinhadas; essas batalhas esperam o fim confirmado, enquanto batalhas selvagens comuns ainda podem usar o último desmaio visível. O cue normal tem duração base de cerca de três segundos e depois permanece em volume cheio por mais dois segundos antes do fade-out. Se a `LootSelectionScreen` aparecer, a mesma Victory é mantida até a tela fechar e então segura por mais dois segundos antes do fade-out. O evento posterior `BATTLE_VICTORY` continua como fallback e não reinicia uma Victory antecipada já enviada.

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

IDs de trigger como `cobbletunes:battle_tower_floor_1` até `cobbletunes:battle_tower_floor_10` são convertidos em **1–3 low, 4–6 mid, 7–9 high e 10 final**. Os markers dos andares da Battle Tower usam o mesmo **raio padrão de 12 blocos** de todas as outras zonas manuais do CobbleTunes. Os markers continuam sendo avaliados do mais próximo para o mais distante, então zonas sobrepostas resolvem para o marker elegível mais próximo. Use `cobbletunes_range:<blocos>` apenas quando quiser substituir intencionalmente o raio padrão em uma configuração específica.

As zonas de andar da Battle Tower controlam **somente a ambientação do mundo**. Elas não forçam mais batalhas de treinador para o tema da Battle Tower de Galar. Treinadores genéricos `f*_trainer*` usam a rota regional comum por maioria do time, enquanto os dez encontros exatos `pokemon_trainer_*` usam seus pools especiais baseados no lore.

### Game Corner e zonas de Cassino

Um marker manual do servidor pode transformar uma construção própria em Game Corner ou Cassino sem registrar um bloco próprio do CobbleTunes. Invoque um `minecraft:marker` vanilla com a tag `cobbletunes_zone:<zoneId>` e use um destes IDs de zona:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Ao entrar na zona o mod inicia uma playlist embaralhada do Game Corner. A primeira faixa é aleatória e quando ela termina o CobbleTunes avança automaticamente para outra sem manter o mesmo arquivo em loop. Todas as faixas passam uma vez antes do pool ser embaralhado novamente e o novo ciclo evita repetir imediatamente a música que acabou de tocar. Batalha e vitória continuam com prioridade e depois delas a playlist do Game Corner continua enquanto a zona permanecer ativa.

O pool possui 15 arquivos esperados dentro de `assets/cobbletunes/sounds/gamecorner/`, divididos entre FRLG, Emerald, HGSS e Platinum. O áudio continua sendo fornecido separadamente pelo resource pack. Veja [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) para os nomes exatos e as sugestões de temas de origem.

As zonas manuais do servidor usam entidades vanilla `minecraft:marker` invisíveis com raio padrão de 12 blocos. Uma tag opcional `cobbletunes_range:<blocos>` pode substituir esse raio individualmente entre 1–32 blocos; valores ausentes ou inválidos voltam para 12. Por exemplo, `/summon minecraft:marker 300 64 300 {Tags:["cobbletunes_zone:cobbletunes:game_corner","cobbletunes_range:12"]}` cria uma âncora de Game Corner. Quando zonas se sobrepõem, vence o marker mais próximo cujo próprio raio realmente alcança o jogador. Vários markers que resolvem para a mesma zona não reiniciam nem reembaralham a música atual. O antigo bloco customizado `cobbletunes:music_trigger` foi removido para que a instalação do CobbleTunes no servidor continue opcional para os clientes; mundos que usavam esse bloco devem converter as âncoras para markers antes de atualizar. Veja [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) para os exemplos completos.

### Ambientação e estruturas

A ambientação regional por bioma está definida para Kanto, Johto, Hoenn, Sinnoh e Unova. As faixas são agrupadas entre biomas vanilla e Terralith como planícies, florestas, cavernas, oceanos, montanhas, neve, desertos e regiões vulcânicas.

As músicas de bioma usam memória, orçamento de rotação, intervalos de silêncio e debounce. Cruzar um bioma muito pequeno não força imediatamente uma nova música caso o bioma mude novamente durante a janela de debounce. Ao sair de um bioma resolvido, a música atual fica memorizada por **3–6 minutos** aleatórios. Voltar antes desse prazo mantém a música lembrada, a menos que o jogador já tenha confirmado transições por **3 outros biomas**. Quando qualquer um dos limites é atingido, o bioma escolhe uma nova música e evita repetir a anterior sempre que houver outra opção válida no pool.

Música de estrutura tem prioridade sobre ambientação de bioma. Com a ponte opcional do servidor, as fontes exatas suportadas incluem:

- todas as 71 estruturas registradas do Cobbleverse entre os datapacks principal, Johto, Hoenn e Sinnoh, incluindo os caminhos atuais `legendary/` e `mythical/`;
- estruturas vanilla como Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals e outras;
- todas as 26 estruturas do Terralith referenciadas pelos structure sets ativos, redirecionadas para os pools vanilla/BCA mais próximos;
- vilas dark, default, fighting, fairy e ice do CobblemonAdditions `4.3.0` redirecionadas para os pools BCA pequenos, médios e grandes, além da Witch Hut reutilizando o pool de Swamp Hut;
- variantes do Repurposed Structures redirecionadas para o pool vanilla/BCA mais próximo;
- todas as 13 estruturas registradas pela versão leve LegendaryMonuments-Cobbleverse redirecionadas para músicas regionais/de estruturas já existentes;
- todos os **32 IDs registrados de estruturas Habitat do Cobblemon 1.8**, mapeados por ambiente/tema para pools existentes de Trail Ruins, Jungle Pyramid, Igloo, Ocean Ruin, Desert Pyramid, Swamp Hut, Mineshaft Mesa e relacionados;
- todos os **29 IDs de ruínas do Cobblemon**: as seis torres de Gimmighoul (`deserted`, `frozen`, `lush`, `rooted`, `sunscorched` e `temperate`) mantêm o pool dedicado de Pokémon Tower/Lavender Town/Pokégear Unown, enquanto as outras 23 ruínas reutilizam o pool de estrutura existente mais próximo;
- as três **Shipwreck Coves** do Cobblemon, incluindo `magma_shipwreck_cove` do 1.8, e os três IDs de barcos de pesca reutilizam o pool existente de Shipwreck;
- zonas manuais ancoradas por entidades vanilla `minecraft:marker` com a tag `cobbletunes_zone:<zoneId>`.

O novo **Habitat Block** do Cobblemon não é usado como detector automático de música. Ele é um bloco colocável/spawner, enquanto a música de estrutura do CobbleTunes continua ligada a identidades reais de worldgen `StructureStart`; áreas personalizadas com Habitat Block ainda podem receber música por um marcador manual `cobbletunes_zone:`.

No modo com servidor, a detecção resolve o `StructureStart` registrado real pelas referências dos chunks próximos, usando o mesmo conceito de identidade worldgen de comandos como `/locate structure`. Estruturas compactas de até 16×16 blocos recebem 4 blocos de margem horizontal por lado e 4 na vertical; estruturas maiores mantêm 1 bloco horizontal e 2 na vertical. Quando estruturas mapeadas se sobrepõem, estruturas especiais vencem estruturas genéricas, estruturas genéricas vencem vilas e empates de prioridade preferem a menor estrutura antes de usar a ordem do ID de registro. O debug informa o ID de registro autoritativo e a zona escolhida. Assim, o roteamento fica estável e determinístico sem usar blocos colocáveis pelo jogador como evidência.

A maioria das músicas fixas de zona fica em loop até o jogador sair. Vilas funcionam de forma diferente: o tema escolhido toca uma vez, espera entre **5 e 60 segundos** e repete a mesma faixa enquanto o jogador continuar dentro da vila. Ao sair e entrar novamente o mod escolhe outra faixa quando o pool possui uma alternativa. Game Corner e Cassino também continuam sem loop individual e avançam pela playlist embaralhada de 15 faixas. Batalha e vitória assumem temporariamente o áudio sem apagar o estado atual da zona. Os IDs atuais de lendários e míticos do Cobbleverse são normalizados para os IDs de zona já existentes no CobbleTunes, enquanto os IDs achatados antigos continuam válidos como aliases de compatibilidade. Terralith não adiciona novos eventos de som: vilas, huts, rubble, estruturas Mage, Spire e locais subterrâneos reutilizam pools já existentes de Village, Igloo, Trail Ruins, Mansion, End City, Stronghold, Jungle Pyramid, Mineshaft, Ocean Ruin e outros. Repurposed Structures também reutiliza pools existentes. A versão leve LegendaryMonuments-Cobbleverse também não adiciona novos áudios: Distortion Portal, Giratina Island, Turnback Cave e Eternatus Cocoon reutilizam Distortion World de Sinnoh; Lake Acuity, Lake Valor e Lake Verity reutilizam Lake Caverns; Stark Mountain reutiliza sua faixa de estrutura já existente; os santuários Firescourge, Grasswither, Groundblight e Icerend reutilizam Mt. Chimney, Ecruteak, Underground Ruins e Ice Path; Outskirt Stand reutiliza Route 111 de Hoenn. O único bioma próprio desse JAR, `legendarymonuments:distortion_world_biome`, reutiliza diretamente a ambientação Distortion World de Sinnoh.

### Alerta de HP baixo

O Pokémon ativo é verificado periodicamente durante a batalha. Quando um Pokémon ativo e vivo chega a 25% de HP ou menos, o CobbleTunes toca o alerta de HP baixo uma única vez.

O efeito usa `0.80x` o volume configurado para música, fazendo o alerta tocar a 80% do volume atual de música do CobbleTunes.

### Música espacial de evolução

A música de evolução é totalmente do cliente. Para evoluções visíveis no mundo, cada cliente com CobbleTunes observa os `PokemonEntity` sincronizados próximos e reage ao estado `isEvolving` sem pacote customizado do servidor. O suspense começa após 20 ticks para alinhar com o início da animação visual de evolução do Cobblemon, para quando o estado sincronizado volta para false e então toca o sting regional de conclusão na mesma posição do Pokémon.

Se uma evolução for concluída pela tela Summary do Cobblemon enquanto esse Pokémon não possui uma entidade viva no mundo, o CobbleTunes cria um snapshot de `CobblemonClient.storage.party` quando a Summary abre e observa esse armazenamento sincronizado da party por uma mudança de espécie no mesmo UUID. Ele não depende dos objetos de Pokémon guardados pela própria tela Summary, que podem continuar desatualizados durante a evolução. O watcher permanece armado por 10 segundos após a Summary fechar para capturar uma sincronização final atrasada. Nesse fallback ele não toca suspense e reproduz somente o cue regional de conclusão na posição do jogador. Uma entidade viva próxima ou uma evolução espacial já rastreada bloqueia o fallback para evitar áudio duplicado.

Os sons de evolução no mundo são posicionais e usam a categoria **Discos/Jukebox** do Minecraft em vez da categoria global de Música. O suspense usa `0.62x` e a conclusão `0.78x` do volume de música configurado no CobbleTunes, com atenuação linear até aproximadamente 32 blocos. Várias evoluções próximas podem tocar ao mesmo tempo em direções diferentes. Enquanto qualquer cue de evolução audível estiver tocando, incluindo a congratulação, a música de ambiente/estrutura do mundo reduz suavemente para `0.20x` e permanece nesse nível durante toda a sequência. Somente depois que o cue final de congratulação termina a música do mundo retorna ao volume normal com um fade de 2 segundos. Batalha, vitória e menu não recebem esse ducking. A conclusão fallback da Summary é emitida na posição do jogador com o mesmo volume de `0.78x`, reduz o ambiente para `0.20x` durante o cue e usa o mesmo fade de retorno de 2 segundos. Se o estado de evolução visível terminar sem a espécie do Pokémon mudar, o CobbleTunes trata como uma evolução interrompida e não toca o sting de congratulação.

O roteamento regional usa a região da Pokédex Nacional do Pokémon em evolução, com formas de Alola, Galar, Hisui e Paldea substituindo a região da espécie base. Alola escolhe aleatoriamente entre `um_evo.ogg` e `um_evo2.ogg`, e Galar entre `swsh_evo.ogg` e `swsh_evo2.ogg`. A conclusão sempre usa o arquivo plano `<prefix>_congrat.ogg` da mesma região. Os arquivos de evolução são esperados diretamente em `assets/cobbletunes/sounds/evolution/`, seguindo nomes como `fr_evo.ogg`, `bl_congrat.ogg` e `sv_evo.ogg`. Arquivos ausentes ficam em silêncio e não reduzem a música do mundo.

### Configuração

O CobbleTunes cria seus JSONs dentro de `config/cobbletunes/`. O Mod Menu é opcional: quando instalado, o botão Configure edita o mesmo JSON do cliente mostrado abaixo. Sem Mod Menu, o jogador pode editar qualquer opção do cliente diretamente em `config/cobbletunes/client.json`. O debug do servidor continua em `config/cobbletunes/server.json`. Arquivos antigos soltos `cobbletunes-client.json` e `cobbletunes-server.json` são migrados automaticamente no próximo início.

#### `config/cobbletunes/client.json`

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

#### `config/cobbletunes/server.json`

| Opção | Padrão |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

[Clique aqui para baixar o resource pack a ser usado com o Cobbletunes](https://mega.nz/file/zeIwST5L#_D9raffsmXnsZ1KKtpSYw0_-Nhak7XhcO89OG8Pf468)

### Organização do projeto

- **`src/main/kotlin`** — inicialização comum/servidor, eventos de batalha, classificação do RCT, pontes com WildBosses e Raid Dens, detecção de estruturas incluindo aliases de compatibilidade do Cobbleverse e mapeamentos do Terralith, CobblemonAdditions, Repurposed Structures e Legendary Monuments, markers de zonas manuais, configs e rede.
- **`src/client/kotlin`** — roteamento dos pacotes e classificador de batalha somente cliente, região, ponte de Victory/Loot Menu, inferência local de Raid Dens/WildBosses/RCT, watcher espacial de evolução observado pelo cliente, observação de biomas, estado musical, fades, menu, HP baixo e a tela opcional de configuração do Mod Menu. A identidade de estruturas worldgen é intencionalmente autoritativa no servidor.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — todas as chaves e caminhos do resource pack.

### Licença

O CobbleTunes é distribuído como **All Rights Reserved (ARR)**. O mod original e não modificado pode ser usado em servidores públicos ou privados e redistribuído como parte de modpacks públicos ou privados sem necessidade de pedir permissão. Reuploads ou mirrors avulsos não são permitidos. Copiar ou reutilizar o código-fonte/assets do projeto, ou distribuir publicamente builds modificadas, forks, trabalhos derivados ou binários alterados, exige permissão prévia. Consulte [`LICENSE`](./LICENSE) para os termos completos.
