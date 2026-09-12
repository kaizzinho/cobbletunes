# CobbleTunes — Sound Manifest

This manifest matches the current final source and `sounds.json`. Every entry below is registered by `TrackRegistry`, the client evolution watcher, or the low-HP effect path. The former Galar Battle Tower battle key is retained for resource-pack compatibility and manual `/playsound` testing, but Battle Tower floor zones no longer select it automatically.

- **Total sound events:** 441
- **Battle:** 117
- **Victory:** 31
- **Battle Tower:** 25
- **Game Corner:** 15
- **Evolution:** 22
- **Ambience and structures:** 227
- **Menu:** 3
- **Effects:** 1
- **Base folder:** `assets/cobbletunes/sounds/`
- **Music files:** streamed through `sounds.json`

The current source audit resolves all 441 `sounds.json` keys from code. A resource pack still needs to provide the matching `.ogg` files.

Compatibility baseline: **Minecraft 1.21.1 + Cobblemon 1.8.0**. Existing battle-side, Victory/capture, client battle synchronization, and evolution hooks were audited against the supplied 1.8 JAR. Alpha-specific battle routing and the complete registered Cobblemon 1.8 worldgen structure set are now supported without adding new sound events.

## Routing notes

- CobbleTunes is client-first. On a remote server without CobbleTunes, the client infers standard battle routes from Cobblemon's synchronized battle actors and rosters, observes capture success locally, and uses client-visible WildBosses/RCT/Raid Dens metadata when available. When the server bridge is present, its packets are authoritative and the local fallback is disabled.
- Server packets are capability-gated and are only sent to clients that advertise the matching CobbleTunes payload. Public-server clients do not need a CobbleTunes server to use battle, Victory/capture, biome, menu, low-HP, and other client-owned music systems.
- Evolution music is fully client-observed and never uses a CobbleTunes server packet. Nearby clients watch Cobblemon's synchronized `PokemonEntity.isEvolving` state, begin the suspense cue after 20 ticks to align with the visible animation, and stop it when the state returns to false. A regional completion sting plays from the Pokémon position only when the species actually changed, so an interrupted evolution does not produce a false congratulations cue. The cues use the Records/Jukebox category with 32-block attenuation; any audible evolution cue, including congratulations, keeps world ambience/structure music ducked to `0.20x` without ducking battle, Victory, or menu music. After the final audible evolution cue ends, the world soundtrack fades back to normal over 2 seconds.
- Client-only Raid Dens detection can promote a battle from the native `cobblemonraiddens:battle.raid.tier_*` sound and uses the same tier-weighted pool. The client also treats the raid boss reaching 0 HP as the clear point, so the five-second regional Victory base cue does not wait for Raid Dens to close its battle/dimension state, and it stays at full volume for another two seconds before fade-out begins.
- Client-only structure routing intentionally does not infer worldgen structures from ordinary player-placeable blocks. Bells, healer+PC setups, Gilded/Gimmighoul chests, and portal-like obsidian/netherrack builds are never authoritative structure evidence. Exact `StructureStart` IDs and manual marker zones remain server-enhanced routing, so public-server clients safely fall back to biome ambience instead of misclassifying player bases.
- Biome track memory preserves short returns without pinning a player to the same long theme forever. Leaving a debounce-committed biome rolls a random **3–6 minute** memory window; the memory also expires after **3 other committed biome transitions**. An expired return selects a fresh track and excludes the previous track whenever the biome pool has another valid choice. Tiny pass-through biomes do not count unless they survive the normal transition debounce.
- Battle routing keeps role and region separate, with RCT metadata preferred and opposing-roster voting as fallback.
- The supplied RCT Tower datapack uses lore-specific weighted pools for its ten named `pokemon_trainer_*` encounters. Generic `f1_trainer*` through `f10_trainer*` floor trainers are forced to ordinary trainer routing with region chosen by opposing-roster majority. Named pools are: Red 60% `johto_champion` / 40% `unova_pvp_champion_johto`; Green and Oak 60% `kanto_champion` / 40% `unova_pvp_champion_kanto`; Gold and Kris 60% `johto_champion` / 40% `unova_pvp_champion_johto`; May 50% `hoenn_rival_pvp` / 25% `hoenn_champion_wallace` / 25% `unova_pvp_champion_hoenn`; Steven 60% `hoenn_champion_wallace` / 40% `unova_pvp_champion_hoenn`; Barry 80% `sinnoh_rival_pvp` / 20% `unova_pvp_champion_sinnoh`; Silver 100% `johto_rival_pvp`; Morimoto 100% `tower_b2w2_pwt_final`. Pools avoid immediate repeats when alternatives exist. Exact matching also accepts normalized `rctmod:` IDs.
- Alolan, Galarian, Hisuian, and Paldean forms override the base National Dex region for wild, trainer-roster, Boss, and Victory routing.
- Cobblemon 1.8 Alpha Pokémon are detected directly from synchronized Alpha metadata (`Pokemon.isAlpha()` server-side and `PokemonProperties.isAlpha` client-side), never from visual heuristics. Only wild Alpha opponents use the route. Priority is Raid Dens → WildBosses → Alpha → ordinary routing. Alpha reuses the Boss selector with a fixed 70% regional PvP / 20% generic Legendary / 10% BW World Tournament profile, equivalent to the existing Rare weighting. Alpha Victory/capture remains normal regional wild Victory.
- WildBosses and Cobblemon Raid Dens share the same tier-weighted regional battle pools. Raid Dens stays reflection-only, but detection checks the server-side `EntityBackedBattleActor` Pokémon first through `IRaidAccessor`, resolves `crd_getRaidBoss()` directly when available, falls back through `crd_getRaidId()` and `RaidHelper.ACTIVE_RAIDS`, and only then tries the battle-level `IRaidBattle` marker. Native `RaidTier.getStars()` drives the mapping: 1-star raids use Uncommon odds, 2-star Rare, 3–4-star Epic, 5–6-star Legendary, and 7-star Mythic. Raid battles reuse the regional rival/PvP, generic Legendary, and BW World Tournament pools with the same 80/15/5, 70/20/10, 60/30/10, 50/35/15, and 45/40/15 weights. Species-specific Legendary encounter tracks remain excluded from the generic pool. Raid completion is read from Raid Dens' reflected `RAID_END` event; a successful clear plays the regional wild Victory theme for a 5-second base duration, holds it at full volume for another 2 seconds before fade-out, preserves that cue across the Raid Dens-to-overworld dimension transition, and a failed raid only releases the battle-music override.
- Legendary routing includes species and form overrides for Kyurem, Necrozma, Eternatus, Calyrex, Terapagos, Galarian birds, and other dedicated encounters.
- Standard Battle Victory is side-aware. With the CobbleTunes server bridge, each Cobblemon `BATTLE_FAINTED` event only arms a short outcome watch. Early Victory is sent after Showdown has declared the winning actor(s) and CobbleTunes has independently verified that the complete server-side opposing `BattleSide` roster is at 0 HP. This covers multi-Pokémon trainers, PvP, doubles and 2v2/team battles and avoids premature Victory after an intermediate faint or an unresolved simultaneous KO. On client-only public servers, hidden trainer/PvP reserves are never guessed; those battles wait for confirmed battle end, while ordinary wild battles can still use the fast visible-faint path. The normal cue has a 3-second base duration and then remains at full volume for another 2 seconds before fade-out starts. When Cobblemon Loot Menu is installed, `LootSelectionScreen` extends that already-started Victory until the screen closes; after the screen closes, Victory stays at full volume for 2 seconds before fade-out begins. Cobblemon's later `BATTLE_VICTORY` event remains the fallback and does not restart a cue already sent by the decisive-faint path. Successful captures and Raid Dens keep their dedicated Victory paths.
- Hisui intentionally has no post-battle Victory file in this pack and falls back without creating a fake theme.
- Battle Tower ambience uses marker-driven low, mid, high, and final floor pools: floors 1–3 low, 4–6 mid, 7–9 high, and 10 final. Floor zones affect ambience only; they do not override trainer battle routing.
- With the server bridge, all 71 registered structures across the supplied Cobbleverse main, Johto, Hoenn, and Sinnoh datapacks are recognized. Current nested IDs such as `cobbleverse:legendary/articuno`, `cobbleverse:legendary/groudon`, and `cobbleverse:mythical/manaphy` normalize to the existing CobbleTunes zone IDs; the older flattened IDs remain compatibility aliases.
- With the server bridge, CobblemonAdditions `4.3.0` dark, default, fighting, fairy, and ice villages reuse the existing BCA small/mid/large pools. `bca:village/witch_hut` reuses the Swamp Hut pool, and the older generic BCA village IDs remain supported. Village tracks play once, wait a random 5–60 seconds, then replay the same selection while the player stays inside; re-entering prefers a different track when possible.
- With the server bridge, all 26 structures referenced by the supplied Terralith structure sets reuse existing vanilla/BCA pools, so Terralith structure support adds no sound events. The two extra structure definitions not referenced by a Terralith structure set are intentionally ignored.
- With the server bridge, Repurposed Structures `7.5.21+1.21.1` is detected as a soft integration. All 107 worldgen structure IDs in that JAR reuse existing vanilla/BCA structure pools, so it adds no new audio files.
- With the server bridge, the `LegendaryMonuments-Cobbleverse` light build is detected as a soft integration for all 13 structures registered by that JAR, without adding sound events: Distortion Portal, Giratina Island, Turnback Cave, and Eternatus Cocoon use `ambience.sinnoh.end_distortion_world`; Lake Acuity, Lake Valor, and Lake Verity use `ambience.sinnoh.cave_lake_caverns`; Stark Mountain uses `ambience.vanilla.fortress.stark_mountain`; Firescourge, Grasswither, Groundblight, and Icerend shrines use `ambience.hoenn.volcanic_mt_chimney`, `ambience.johto.forest_ecruteak`, `ambience.unova.cave_underground_ruins`, and `ambience.johto.cave_ice_path`; Outskirt Stand uses `ambience.hoenn.desert_route_111`. The JAR's only custom biome, `legendarymonuments:distortion_world_biome`, directly reuses the Sinnoh Distortion World ambience. Full LM 8.1-only structures/biomes are intentionally not mapped in this Cobbleverse-targeted build.
- Server-enhanced structure detection resolves actual registered `StructureStart` objects from nearby chunk references. Compact structures up to 16×16 blocks receive 4 blocks of horizontal padding per side and 4 vertical blocks of padding; larger structures retain 1 horizontal block and 2 vertical blocks. Overlaps are deterministic: special structures beat generic structures, generic structures beat villages, and equal-priority overlaps prefer the smaller structure before registry-ID order. Debug logging prints the authoritative registry ID and selected CobbleTunes zone.
- All six Cobblemon Gimmighoul tower worldgen structures under `cobblemon:ruins/` map to `cobbletunes:gimmighoul_tower`. The zone randomly reuses `ambience.kanto.pokemon_tower`, `ambience.kanto.deep_dark_lavender_town`, or `ambience.johto.cave_deep_dark_pokegear_unown`; no new sound keys are added.
- The supplied Cobblemon 1.8 JAR contains 32 registered `cobblemon:habitats/*` Structure IDs, 29 `cobblemon:ruins/*` IDs, three `cobblemon:shipwreck_coves/*` IDs, and three `cobblemon:fishing_boat/*` IDs. All 67 are recognized server-side through real `StructureStart` identity. Six Gimmighoul towers use the dedicated pool above; the other 61 IDs reuse existing vanilla/BCA structure pools. The placeable Habitat Block itself is intentionally not used as automatic structure evidence.
- `cobbletunes:game_corner` and `cobbletunes:casino` are manual server zones anchored by vanilla `minecraft:marker` entities tagged with `cobbletunes_zone:<zoneId>`. Entering either zone starts a shuffled Game Corner playlist. Tracks do not loop individually; each file plays once, then another track is selected from the remaining shuffled pool. After all 15 tracks play, the pool reshuffles and avoids an immediate repeat across the cycle boundary.

## Manual zone marker setup tutorial

Manual server zones use vanilla `minecraft:marker` entities instead of a custom CobbleTunes block. This keeps the server bridge optional for clients because CobbleTunes no longer registers gameplay content that a joining client must know about.

The server checks nearby marker entities every 10 ticks. A marker is a CobbleTunes zone anchor when one of its scoreboard tags starts with `cobbletunes_zone:`. Everything after that prefix is the normal CobbleTunes zone ID. Manual marker zones have priority over automatic worldgen structure detection, and when more than one eligible marker is in range the nearest one wins.

Each marker uses a **12-block default radius**. Add `cobbletunes_range:<blocks>` to override the radius for that marker only. Supported explicit values are **1–32**; a missing, malformed, zero/negative, or out-of-range value falls back to 12. CobbleTunes keeps the entity scan bounded to the 32-block maximum, then checks the real radius of every candidate marker before it can win. Multiple markers that resolve to the same zone ID are one logical zone, so moving from one anchor to another does not restart or reshuffle the music. For a large building, place more than one marker with the same zone tag so the whole interior stays covered. Markers are invisible and server-side, so they do not need a CobbleTunes block model or client registry entry.

### Basic placement

Summon a marker with the full CobbleTunes zone tag:

```mcfunction
/summon minecraft:marker <x> <y> <z> {Tags:["cobbletunes_zone:cobbletunes:pokecenter"]}
```

Optional per-marker radius:

```mcfunction
/summon minecraft:marker <x> <y> <z> {Tags:["cobbletunes_zone:cobbletunes:pokecenter","cobbletunes_range:12"]}
```

Battle Tower floor example with a management tag:

```mcfunction
/summon minecraft:marker <x> <y> <z> {Tags:["cobbletunes_zone:cobbletunes:battle_tower_floor_1","cobbletunes_range:12","cobbletunes_bt"]}
```

`cobbletunes_bt` has no routing meaning; it is only an administrator-friendly tag, for example `/kill @e[type=minecraft:marker,tag=cobbletunes_bt]`.

Inspect nearby CobbleTunes markers with:

```mcfunction
/data get entity @e[type=minecraft:marker,tag=cobbletunes_zone:cobbletunes:pokecenter,sort=nearest,limit=1,distance=..12] Tags
```

Remove a nearby marker with:

```mcfunction
/kill @e[type=minecraft:marker,tag=cobbletunes_zone:cobbletunes:pokecenter,sort=nearest,limit=1,distance=..2]
```

Only the tag suffix changes between the examples below.

### Poké Center

Use:

```text
cobbletunes:pokecenter
```

Example:

```mcfunction
/summon minecraft:marker 100 64 100 {Tags:["cobbletunes_zone:cobbletunes:pokecenter"]}
```

Poké Center music is manual-marker-only. Generated Poké Center buildings inside villages keep the parent village theme unless a `cobbletunes:pokecenter` marker is deliberately placed there, and player-built healer/PC setups are never auto-detected. When the marker zone becomes active, the client randomly selects one of the five Poké Center themes and loops it while the player stays inside. Battle and Victory music can temporarily take priority, then the active zone music returns afterward.

### Poké Mart

Use:

```text
cobbletunes:pokemart
```

Example:

```mcfunction
/summon minecraft:marker 120 64 100 {Tags:["cobbletunes_zone:cobbletunes:pokemart"]}
```

Poké Mart music is manual-marker-only. Generated marts inside villages keep the village theme unless a `cobbletunes:pokemart` marker is deliberately placed there. The client randomly selects one of the three Poké Mart themes when the marker zone becomes active and loops that track while the player remains inside.

### Battle Tower floors

For a ten-floor Battle Tower, use one floor-specific zone ID per floor:

| Floor | Zone ID | Music pool |
|---:|---|---|
| 1 | `cobbletunes:battle_tower_floor_1` | low |
| 2 | `cobbletunes:battle_tower_floor_2` | low |
| 3 | `cobbletunes:battle_tower_floor_3` | low |
| 4 | `cobbletunes:battle_tower_floor_4` | mid |
| 5 | `cobbletunes:battle_tower_floor_5` | mid |
| 6 | `cobbletunes:battle_tower_floor_6` | mid |
| 7 | `cobbletunes:battle_tower_floor_7` | high |
| 8 | `cobbletunes:battle_tower_floor_8` | high |
| 9 | `cobbletunes:battle_tower_floor_9` | high |
| 10 | `cobbletunes:battle_tower_floor_10` | final |

Example for floor 4:

```mcfunction
/summon minecraft:marker 200 90 200 {Tags:["cobbletunes_zone:cobbletunes:battle_tower_floor_4"]}
```

The nearest marker wins, which is important for vertically stacked floors. Put each marker near the center of its floor and avoid placing different floor markers at nearly the same vertical position.

The grouped IDs below are also valid when a build does not need numbered floors:

```text
cobbletunes:battle_tower_low
cobbletunes:battle_tower_mid
cobbletunes:battle_tower_high
cobbletunes:battle_tower_final
```

Battle Tower zone tracks loop while the floor owns the audio slot. Floor markers control ambience only. Generic RCT floor trainers use ordinary roster-majority regional trainer music, while exact named special trainers use their lore pools. When the battle or Victory sequence ends, CobbleTunes restores the latest active floor zone.

### Game Corner and casino

Use either of these IDs:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Example:

```mcfunction
/summon minecraft:marker 300 64 300 {Tags:["cobbletunes_zone:cobbletunes:game_corner"]}
```

Both IDs use the same 15-track Game Corner pool. The first track is random. Individual tracks do not loop. When one finishes, CobbleTunes advances to another track from the shuffled no-repeat queue. After all 15 tracks have played, the pool is reshuffled and the previous track is prevented from immediately repeating across the cycle boundary.

Leaving the Game Corner clears the current shuffle queue. Re-entering starts a fresh randomized order. Battle and Victory music can interrupt the zone without changing the Game Corner marker itself.

### Placement tips

- markers are invisible and do not need to be hidden inside a block
- keep players within the configured radius of at least one marker; untagged markers default to 12 blocks
- use several markers with the same zone tag for large rooms or long hallways
- if different manual zones overlap the nearest marker wins
- manual markers override nearby automatic structure music while they are in range
- leaving every manual marker lets the current automatic structure or biome ambience take over again
- use floor-specific Battle Tower IDs for stacked floors; they use the same 12-block default radius as other manual zones, with nearest-marker resolution when ranges overlap

### Migration from older CobbleTunes builds

The old `cobbletunes:music_trigger` custom block has been removed. It made a server-side CobbleTunes install part of registry synchronization, which worked against the new client-first goal. If an existing world used those blocks, replace each old trigger with a vanilla marker carrying the equivalent `cobbletunes_zone:<ZoneId>` tag before updating. The zone IDs and music pools themselves are unchanged.

## Battle music

### Kanto

| Key | File | Usage |
|---|---|---|
| `battle.kanto.champion_rival` | `battle/kanto/champion_rival.ogg` | kanto champion and rival pvp |
| `battle.kanto.legendary_mewtwo` | `battle/kanto/legendary_mewtwo.ogg` | mewtwo |
| `battle.kanto.legendary_deoxys` | `battle/kanto/legendary_deoxys.ogg` | deoxys |
| `battle.kanto.legendary_default` | `battle/kanto/legendary_default.ogg` | regional legendary fallback |
| `battle.kanto.gym_leader` | `battle/kanto/gym_leader.ogg` | regional gym leader battle |
| `battle.kanto.wild` | `battle/kanto/wild.ogg` | regional wild battle |
| `battle.kanto.trainer` | `battle/kanto/trainer.ogg` | regional trainer battle |

### Johto

| Key | File | Usage |
|---|---|---|
| `battle.johto.hgss_legendary_super_ancient` | `battle/johto/hgss_legendary_super_ancient.ogg` | groudon kyogre rayquaza hgss encounter |
| `battle.johto.legendary_raikou` | `battle/johto/legendary_raikou.ogg` | raikou |
| `battle.johto.gym_leader` | `battle/johto/gym_leader.ogg` | regional gym leader battle |
| `battle.johto.rival_pvp` | `battle/johto/rival_pvp.ogg` | regional rival and pvp |
| `battle.johto.team_rocket` | `battle/johto/team_rocket.ogg` | team rocket route |
| `battle.johto.trainer` | `battle/johto/trainer.ogg` | regional trainer battle |
| `battle.johto.legendary_default` | `battle/johto/legendary_default.ogg` | regional legendary fallback |
| `battle.johto.champion` | `battle/johto/champion.ogg` | regional champion battle |
| `battle.johto.legendary_suicune` | `battle/johto/legendary_suicune.ogg` | suicune |
| `battle.johto.legendary_lugia` | `battle/johto/legendary_lugia.ogg` | lugia |
| `battle.johto.legendary_ho_oh` | `battle/johto/legendary_ho_oh.ogg` | ho oh |
| `battle.johto.legendary_entei` | `battle/johto/legendary_entei.ogg` | entei |
| `battle.johto.wild` | `battle/johto/wild.ogg` | regional wild battle |

### Hoenn

| Key | File | Usage |
|---|---|---|
| `battle.hoenn.champion_wallace` | `battle/hoenn/champion_wallace.ogg` | hoenn champion battle |
| `battle.hoenn.elite_four` | `battle/hoenn/elite_four.ogg` | regional elite four battle |
| `battle.hoenn.legendary_regis` | `battle/hoenn/legendary_regis.ogg` | regirock regice registeel |
| `battle.hoenn.legendary_super_ancient` | `battle/hoenn/legendary_super_ancient.ogg` | groudon kyogre rayquaza |
| `battle.hoenn.team_aqua_magma_leaders` | `battle/hoenn/team_aqua_magma_leaders.ogg` | team aqua and magma admin boss route |
| `battle.hoenn.rival_pvp` | `battle/hoenn/rival_pvp.ogg` | regional rival and pvp |
| `battle.hoenn.gym_leader` | `battle/hoenn/gym_leader.ogg` | regional gym leader battle |
| `battle.hoenn.team_aqua_magma_grunt` | `battle/hoenn/team_aqua_magma_grunt.ogg` | team aqua and magma grunt route |
| `battle.hoenn.trainer` | `battle/hoenn/trainer.ogg` | regional trainer battle |
| `battle.hoenn.legendary_mew` | `battle/hoenn/legendary_mew.ogg` | mew |
| `battle.hoenn.wild` | `battle/hoenn/wild.ogg` | regional wild battle |

### Sinnoh

| Key | File | Usage |
|---|---|---|
| `battle.sinnoh.platinum_legendary_arceus` | `battle/sinnoh/platinum_legendary_arceus.ogg` | arceus platinum encounter |
| `battle.sinnoh.legendary_alt` | `battle/sinnoh/legendary_alt.ogg` | sinnoh alternate generic legendary fallback |
| `battle.sinnoh.legendary_giratina` | `battle/sinnoh/legendary_giratina.ogg` | giratina |
| `battle.sinnoh.legendary_dialga_palkia` | `battle/sinnoh/legendary_dialga_palkia.ogg` | dialga and palkia |
| `battle.sinnoh.team_galactic_boss` | `battle/sinnoh/team_galactic_boss.ogg` | team galactic boss route |
| `battle.sinnoh.team_galactic_commander` | `battle/sinnoh/team_galactic_commander.ogg` | team galactic commander route |
| `battle.sinnoh.team_galactic_grunt` | `battle/sinnoh/team_galactic_grunt.ogg` | team galactic grunt route |
| `battle.sinnoh.gym_leader` | `battle/sinnoh/gym_leader.ogg` | regional gym leader battle |
| `battle.sinnoh.rival_pvp` | `battle/sinnoh/rival_pvp.ogg` | regional rival and pvp |
| `battle.sinnoh.trainer` | `battle/sinnoh/trainer.ogg` | regional trainer battle |
| `battle.sinnoh.frontier_brain` | `battle/sinnoh/frontier_brain.ogg` | frontier brain route |
| `battle.sinnoh.champion` | `battle/sinnoh/champion.ogg` | regional champion battle |
| `battle.sinnoh.elite_four` | `battle/sinnoh/elite_four.ogg` | regional elite four battle |
| `battle.sinnoh.legendary_default` | `battle/sinnoh/legendary_default.ogg` | regional legendary fallback |
| `battle.sinnoh.wild` | `battle/sinnoh/wild.ogg` | regional wild battle |
| `battle.sinnoh.legendary_lake_trio` | `battle/sinnoh/legendary_lake_trio.ogg` | uxie mesprit azelf |

### Unova

| Key | File | Usage |
|---|---|---|
| `battle.unova.champion_iris` | `battle/unova/champion_iris.ogg` | unova champion battle |
| `battle.unova.legendary_black_white_kyurem` | `battle/unova/legendary_black_white_kyurem.ogg` | black and white kyurem forms |
| `battle.unova.champion_sinnoh_pwt` | `battle/unova/champion_sinnoh_pwt.ogg` | sinnoh pwt remix for pvp and boss pools |
| `battle.unova.champion_hoenn_pwt` | `battle/unova/champion_hoenn_pwt.ogg` | hoenn pwt remix for pvp and boss pools |
| `battle.unova.champion_johto_pwt` | `battle/unova/champion_johto_pwt.ogg` | johto pwt remix for pvp and boss pools |
| `battle.unova.champion_kanto_pwt` | `battle/unova/champion_kanto_pwt.ogg` | kanto pwt remix for pvp and boss pools |
| `battle.unova.gym_leader` | `battle/unova/gym_leader.ogg` | regional gym leader battle |
| `battle.unova.trainer` | `battle/unova/trainer.ogg` | regional trainer battle |
| `battle.unova.rival_hugh` | `battle/unova/rival_hugh.ogg` | unova rival and pvp |
| `battle.unova.wild` | `battle/unova/wild.ogg` | regional wild battle |
| `battle.unova.team_plasma_n` | `battle/unova/team_plasma_n.ogg` | team plasma n route |
| `battle.unova.team_plasma_grunt` | `battle/unova/team_plasma_grunt.ogg` | team plasma grunt route |
| `battle.unova.team_plasma_colress` | `battle/unova/team_plasma_colress.ogg` | team plasma admin colress route |
| `battle.unova.bw_legendary_reshiramzekrom` | `battle/unova/bw_legendary_reshiramzekrom.ogg` | reshiram and zekrom |
| `battle.unova.bw_legendary_kyurem` | `battle/unova/bw_legendary_kyurem.ogg` | base kyurem |
| `battle.unova.bw_legendary_default` | `battle/unova/bw_legendary_default.ogg` | unova legendary fallback |

### Kalos

| Key | File | Usage |
|---|---|---|
| `battle.kalos.xy_legendary_mewtwo` | `battle/kalos/xy_legendary_mewtwo.ogg` | mewtwo and kanto birds xy encounter |
| `battle.kalos.champion` | `battle/kalos/champion.ogg` | regional champion battle |
| `battle.kalos.elite_four` | `battle/kalos/elite_four.ogg` | regional elite four battle |
| `battle.kalos.team_flare_lysandre` | `battle/kalos/team_flare_lysandre.ogg` | team flare lysandre route |
| `battle.kalos.legendary_trio` | `battle/kalos/legendary_trio.ogg` | xerneas yveltal zygarde |
| `battle.kalos.team_flare_grunt` | `battle/kalos/team_flare_grunt.ogg` | team flare grunt admin route |
| `battle.kalos.gym_leader` | `battle/kalos/gym_leader.ogg` | regional gym leader battle |
| `battle.kalos.trainer` | `battle/kalos/trainer.ogg` | regional trainer battle |
| `battle.kalos.wild` | `battle/kalos/wild.ogg` | regional wild battle |
| `battle.kalos.rival_friend` | `battle/kalos/rival_friend.ogg` | kalos rival and pvp |

### Alola

| Key | File | Usage |
|---|---|---|
| `battle.alola.usum_legendary_ultra_necrozma` | `battle/alola/usum_legendary_ultra_necrozma.ogg` | ultra necrozma |
| `battle.alola.legendary_ultra_beast` | `battle/alola/legendary_ultra_beast.ogg` | alola ultra beast fallback |
| `battle.alola.legendary_tapu` | `battle/alola/legendary_tapu.ogg` | tapu guardians |
| `battle.alola.champion_summit` | `battle/alola/champion_summit.ogg` | regional battle route |
| `battle.alola.elite_four` | `battle/alola/elite_four.ogg` | regional elite four battle |
| `battle.alola.legendary_solgaleo_lunala_necrozma` | `battle/alola/legendary_solgaleo_lunala_necrozma.ogg` | solgaleo lunala base necrozma |
| `battle.alola.island_kahuna` | `battle/alola/island_kahuna.ogg` | alola kahuna gym route |
| `battle.alola.legendary_necrozma_fused` | `battle/alola/legendary_necrozma_fused.ogg` | dusk mane and dawn wings necrozma |
| `battle.alola.aether_president_lusamine` | `battle/alola/aether_president_lusamine.ogg` | lusamine route |
| `battle.alola.ultra_recon_squad` | `battle/alola/ultra_recon_squad.ogg` | ultra recon squad route |
| `battle.alola.aether_foundation` | `battle/alola/aether_foundation.ogg` | aether foundation route |
| `battle.alola.team_skull_guzma` | `battle/alola/team_skull_guzma.ogg` | team skull and guzma route |
| `battle.alola.trainer` | `battle/alola/trainer.ogg` | regional trainer battle |
| `battle.alola.wild` | `battle/alola/wild.ogg` | regional wild battle |

### Galar

| Key | File | Usage |
|---|---|---|
| `battle.galar.swsh_legendary_zacian_zamazenta` | `battle/galar/swsh_legendary_zacian_zamazenta.ogg` | zacian and zamazenta |
| `battle.galar.swsh_legendary_giants` | `battle/galar/swsh_legendary_giants.ogg` | regi family and regieleki regidrago |
| `battle.galar.swsh_legendary_eternatus_final` | `battle/galar/swsh_legendary_eternatus_final.ogg` | eternamax eternatus |
| `battle.galar.swsh_legendary_calyrex_fused` | `battle/galar/swsh_legendary_calyrex_fused.ogg` | ice rider and shadow rider calyrex |
| `battle.galar.swsh_legendary_calyrex` | `battle/galar/swsh_legendary_calyrex.ogg` | calyrex |
| `battle.galar.swsh_legendary_birds` | `battle/galar/swsh_legendary_birds.ogg` | galarian articuno zapdos moltres |
| `battle.galar.league_tournament` | `battle/galar/league_tournament.ogg` | galar elite four route |
| `battle.galar.gym_leader` | `battle/galar/gym_leader.ogg` | regional gym leader battle |
| `battle.galar.trainer` | `battle/galar/trainer.ogg` | regional trainer battle |
| `battle.galar.legendary_mysterious_being` | `battle/galar/legendary_mysterious_being.ogg` | galar legendary fallback |
| `battle.galar.battle_tower` | `battle/galar/battle_tower.ogg` | retained compatibility/manual test key; not automatically routed by floor zones |
| `battle.galar.champion_leon` | `battle/galar/champion_leon.ogg` | galar champion and pvp |
| `battle.galar.legendary_eternatus` | `battle/galar/legendary_eternatus.ogg` | eternatus |
| `battle.galar.wild` | `battle/galar/wild.ogg` | regional wild battle |
| `battle.galar.legendary_glastrier_spectrier` | `battle/galar/legendary_glastrier_spectrier.ogg` | glastrier and spectrier |

### Hisui

| Key | File | Usage |
|---|---|---|
| `battle.hisui.wild` | `battle/hisui/wild.ogg` | regional wild battle |
| `battle.hisui.legendary_arceus` | `battle/hisui/legendary_arceus.ogg` | arceus hisui encounter |

### Paldea

| Key | File | Usage |
|---|---|---|
| `battle.paldea.sv_mythical_pecharunt` | `battle/paldea/sv_mythical_pecharunt.ogg` | pecharunt |
| `battle.paldea.sv_legendary_terapagos_stellar` | `battle/paldea/sv_legendary_terapagos_stellar.ogg` | stellar terapagos |
| `battle.paldea.sv_legendary_terapagos` | `battle/paldea/sv_legendary_terapagos.ogg` | terapagos |
| `battle.paldea.sv_legendary_ogerpon` | `battle/paldea/sv_legendary_ogerpon.ogg` | ogerpon |
| `battle.paldea.sv_legendary_loyal_three` | `battle/paldea/sv_legendary_loyal_three.ogg` | okidogi munkidori fezandipiti |
| `battle.paldea.sv_legendary_koraidon_miraidon` | `battle/paldea/sv_legendary_koraidon_miraidon.ogg` | koraidon and miraidon |
| `battle.paldea.sv_legendary_calamity` | `battle/paldea/sv_legendary_calamity.ogg` | treasures of ruin |
| `battle.paldea.legendary_solgaleo_lunala_dlc` | `battle/paldea/legendary_solgaleo_lunala_dlc.ogg` | solgaleo and lunala indigo disk encounter |
| `battle.paldea.champion_top` | `battle/paldea/champion_top.ogg` | paldea champion and pvp |
| `battle.paldea.elite_four` | `battle/paldea/elite_four.ogg` | regional elite four battle |
| `battle.paldea.gym_leader` | `battle/paldea/gym_leader.ogg` | regional gym leader battle |
| `battle.paldea.trainer` | `battle/paldea/trainer.ogg` | regional trainer battle |
| `battle.paldea.wild` | `battle/paldea/wild.ogg` | regional wild battle |

## Victory music

Victory files are used by the optional Cobblemon Loot Menu bridge, successful capture cues, and successful Raid Dens clears. With the server bridge, normal Battle Victory uses Cobblemon's `BATTLE_FAINTED` event only to arm a short outcome watch. CobbleTunes waits for Showdown's authoritative `win` instruction, verifies that the complete opposing `BattleSide` roster is at 0 HP, and only then starts Victory. This prevents the first faint in a trainer, Gym, PvP, doubles, or 2v2 battle from stopping the battle theme and leaves simultaneous-KO resolution to Showdown. Client-only trainer/PvP battles intentionally wait for battle end because the remote client does not receive the opponent's complete hidden reserve roster; ordinary wild battles may still start from the visible final faint. The normal cue uses a 3-second base duration followed by a 2-second full-volume tail before fade-out. With Cobblemon Loot Menu installed, opening `LootSelectionScreen` extends that Victory until the screen closes. Cobblemon's later `BATTLE_VICTORY` lifecycle event remains the fallback and is ignored when the decisive-faint cue was already sent. Capture and Raid Dens Victory paths are unchanged.

| Key | File | Usage |
|---|---|---|
| `victory.xy_victory_wild` | `victory/xy_victory_wild.ogg` | kalos wild victory |
| `victory.xy_victory_trainer` | `victory/xy_victory_trainer.ogg` | kalos trainer elite four champion victory |
| `victory.xy_victory_gymleader` | `victory/xy_victory_gymleader.ogg` | kalos gym victory |
| `victory.swsh_victory_wild` | `victory/swsh_victory_wild.ogg` | galar wild victory |
| `victory.swsh_victory_trainer` | `victory/swsh_victory_trainer.ogg` | galar trainer gym league champion victory |
| `victory.sv_victory_wild` | `victory/sv_victory_wild.ogg` | paldea wild victory |
| `victory.sv_victory_trainer` | `victory/sv_victory_trainer.ogg` | paldea trainer gym elite four champion victory |
| `victory.sm_victory_wild` | `victory/sm_victory_wild.ogg` | alola wild victory |
| `victory.sm_victory_trainer` | `victory/sm_victory_trainer.ogg` | alola trainer kahuna elite four champion victory |
| `victory.platinum_victory_trainer` | `victory/platinum_victory_trainer.ogg` | sinnoh trainer victory |
| `victory.platinum_victory_teamgalactic` | `victory/platinum_victory_teamgalactic.ogg` | sinnoh galactic victory |
| `victory.platinum_victory_gymleader` | `victory/platinum_victory_gymleader.ogg` | sinnoh gym victory |
| `victory.platinum_victory_elitefour` | `victory/platinum_victory_elitefour.ogg` | sinnoh elite four victory |
| `victory.platinum_victory_champion` | `victory/platinum_victory_champion.ogg` | sinnoh champion victory |
| `victory.platinum_victory` | `victory/platinum_victory.ogg` | sinnoh wild victory |
| `victory.hgss_victory_trainer` | `victory/hgss_victory_trainer.ogg` | johto trainer victory |
| `victory.hgss_victory_gymleader` | `victory/hgss_victory_gymleader.ogg` | johto gym elite four champion victory |
| `victory.hgss_victory` | `victory/hgss_victory.ogg` | johto wild victory |
| `victory.frlg_victory_trainer` | `victory/frlg_victory_trainer.ogg` | kanto trainer and elite four victory |
| `victory.frlg_victory_gymleader` | `victory/frlg_victory_gymleader.ogg` | kanto gym and champion victory |
| `victory.frlg_victory` | `victory/frlg_victory.ogg` | kanto wild victory |
| `victory.emerald_victory_wild` | `victory/emerald_victory_wild.ogg` | hoenn wild victory |
| `victory.emerald_victory_trainer` | `victory/emerald_victory_trainer.ogg` | hoenn trainer victory |
| `victory.emerald_victory_gym` | `victory/emerald_victory_gym.ogg` | hoenn gym and elite four victory |
| `victory.emerald_victory_champion` | `victory/emerald_victory_champion.ogg` | hoenn champion victory |
| `victory.emerald_victory_aquamagma` | `victory/emerald_victory_aquamagma.ogg` | hoenn aqua magma victory |
| `victory.bw_victory_trainer` | `victory/bw_victory_trainer.ogg` | unova trainer victory |
| `victory.bw_victory_teamplasma` | `victory/bw_victory_teamplasma.ogg` | unova plasma victory |
| `victory.bw_victory_gymleader` | `victory/bw_victory_gymleader.ogg` | unova gym and elite four victory |
| `victory.bw_victory_champion` | `victory/bw_victory_champion.ogg` | unova champion victory |
| `victory.bw_victory` | `victory/bw_victory.ogg` | unova wild victory |

## Battle Tower music

| Key | File | Pool |
|---|---|---|
| `tower.platinum_battle_tower` | `tower/platinum_battle_tower.ogg` | mid |
| `tower.platinum_battle_hall` | `tower/platinum_battle_hall.ogg` | high |
| `tower.platinum_battle_frontier` | `tower/platinum_battle_frontier.ogg` | low |
| `tower.platinum_battle_factory` | `tower/platinum_battle_factory.ogg` | mid |
| `tower.platinum_battle_castle` | `tower/platinum_battle_castle.ogg` | high |
| `tower.platinum_battle_arcade` | `tower/platinum_battle_arcade.ogg` | high |
| `tower.hgss_battle_tower_reception` | `tower/hgss_battle_tower_reception.ogg` | low |
| `tower.hgss_battle_tower` | `tower/hgss_battle_tower.ogg` | mid |
| `tower.hgss_battle_hall` | `tower/hgss_battle_hall.ogg` | mid |
| `tower.hgss_battle_factory` | `tower/hgss_battle_factory.ogg` | mid |
| `tower.hgss_battle_castle` | `tower/hgss_battle_castle.ogg` | high |
| `tower.hgss_battle_arcade` | `tower/hgss_battle_arcade.ogg` | high |
| `tower.emerald_battle_tower` | `tower/emerald_battle_tower.ogg` | mid |
| `tower.emerald_battle_pyramid_summit` | `tower/emerald_battle_pyramid_summit.ogg` | final |
| `tower.emerald_battle_pyramid` | `tower/emerald_battle_pyramid.ogg` | high |
| `tower.emerald_battle_pike` | `tower/emerald_battle_pike.ogg` | high |
| `tower.emerald_battle_palace` | `tower/emerald_battle_palace.ogg` | high |
| `tower.emerald_battle_frontier` | `tower/emerald_battle_frontier.ogg` | low |
| `tower.emerald_battle_factory` | `tower/emerald_battle_factory.ogg` | mid |
| `tower.emerald_battle_dome` | `tower/emerald_battle_dome.ogg` | high |
| `tower.emerald_battle_arena` | `tower/emerald_battle_arena.ogg` | high |
| `tower.b2w2_pwt_final` | `tower/b2w2_pwt_final.ogg` | final |
| `tower.b2w2_pwt_arena` | `tower/b2w2_pwt_arena.ogg` | mid |
| `tower.b2w2_pwt` | `tower/b2w2_pwt.ogg` | low |
| `tower.frlg_trainer_tower` | `tower/frlg_trainer_tower.ogg` | low |

## Game Corner and Casino music

These tracks are reserved for hand-built Game Corners, casinos, arcades, contest halls, and minigame areas. Add a vanilla `minecraft:marker` with either `cobbletunes_zone:cobbletunes:game_corner` or `cobbletunes_zone:cobbletunes:casino` as its tag. The first track is random. When it ends, CobbleTunes advances through a shuffled no-repeat order until all 15 tracks have played, then reshuffles the pool. Individual Game Corner files use `loop = false`.

The source only registers the sound slots and filenames. No Pokémon soundtrack audio is included.

| Key | File | Suggested source theme | Reference length |
|---|---|---|---:|
| `gamecorner.frlg_rocket_game_corner` | `gamecorner/frlg_rocket_game_corner.ogg` | FRLG Rocket Game Corner | 1:33 |
| `gamecorner.frlg_pokemon_jump` | `gamecorner/frlg_pokemon_jump.ogg` | FRLG Pokémon Jump | 0:58 |
| `gamecorner.frlg_dodrio_berry_picking` | `gamecorner/frlg_dodrio_berry_picking.ogg` | FRLG Dodrio Berry Picking | 1:03 |
| `gamecorner.frlg_union_room` | `gamecorner/frlg_union_room.ogg` | FRLG The Union Room | 0:59 |
| `gamecorner.emerald_game_corner` | `gamecorner/emerald_game_corner.ogg` | Emerald / RSE Game Corner | 1:44 |
| `gamecorner.emerald_contest_lobby` | `gamecorner/emerald_contest_lobby.ogg` | Emerald / RSE Contest Lobby | 0:56 |
| `gamecorner.emerald_pokemon_contest` | `gamecorner/emerald_pokemon_contest.ogg` | Emerald / RSE Pokémon Contest | 1:09 |
| `gamecorner.emerald_trick_house` | `gamecorner/emerald_trick_house.ogg` | Emerald / RSE The Trick House | 1:11 |
| `gamecorner.hgss_goldenrod_game_corner` | `gamecorner/hgss_goldenrod_game_corner.ogg` | HGSS Goldenrod Game Corner | 1:10 |
| `gamecorner.hgss_bug_catching_contest` | `gamecorner/hgss_bug_catching_contest.ogg` | HGSS The Bug-Catching Contest | 0:38 |
| `gamecorner.hgss_pokeathlon_event_time` | `gamecorner/hgss_pokeathlon_event_time.ogg` | HGSS Pokéathlon Event Time | 0:59 |
| `gamecorner.hgss_wifi_plaza_games` | `gamecorner/hgss_wifi_plaza_games.ogg` | HGSS Wi-Fi Plaza Plaza Games | 0:33 |
| `gamecorner.platinum_game_corner` | `gamecorner/platinum_game_corner.ogg` | Platinum Game Corner | 1:24 |
| `gamecorner.platinum_contest_hall` | `gamecorner/platinum_contest_hall.ogg` | Platinum Contest Hall | — |
| `gamecorner.platinum_super_contest` | `gamecorner/platinum_super_contest.ogg` | Platinum Super Contest | — |

The Ruby/Sapphire soundtrack is used as the naming reference for the shared Hoenn tracks because most of Emerald's music is based on the same soundtrack material. Platinum similarly reuses most Diamond/Pearl music while adding its own exclusive tracks.

## Ambience and structure music

Regional biome tracks rotate with silence windows. Biome visit memory keeps the same selection on short returns, but expires after a random 3–6 minutes away or 3 other debounce-committed biome transitions; expired returns avoid the previous track when another valid pool entry exists. Fixed zone tracks such as Gyms, Poké Centers, Poké Marts, Battle Tower floors, exact Cobbleverse structures, Cobblemon Gimmighoul towers, and non-village vanilla/BCA structure pools loop while their zone owns the audio slot. Village pools are one-shot sessions: the chosen track finishes, waits 5–60 seconds, then replays the same track until the player leaves; entering again prefers a different selection. Game Corner and Casino zones advance through their shuffled 15-track playlist instead of looping one track. Setting either CobbleTunes music volume or Minecraft's Music slider to 0% suspends playback and ambience timers; once both volume controls are above zero again, the latest valid context resumes immediately without carrying over silence or village cooldowns.

### Kanto ambience

| Key | File | Usage |
|---|---|---|
| `ambience.kanto.deep_dark_pokemon_mansion` | `ambience/kanto/deep_dark_pokemon_mansion.ogg` | kanto biome ambience pool |
| `ambience.kanto.volcanic_cinnabar` | `ambience/kanto/volcanic_cinnabar.ogg` | kanto biome ambience pool |
| `ambience.kanto.ocean_the_sea` | `ambience/kanto/ocean_the_sea.ogg` | kanto biome ambience pool |
| `ambience.kanto.nether_rocket_hideout` | `ambience/kanto/nether_rocket_hideout.ogg` | rocket hideout structure and trigger |
| `ambience.kanto.pokemon_tower` | `ambience/kanto/pokemon_tower.ogg` | kanto biome ambience pool |
| `ambience.kanto.deep_dark_lavender_town` | `ambience/kanto/deep_dark_lavender_town.ogg` | kanto biome ambience pool |
| `ambience.kanto.route_lavender_somber` | `ambience/kanto/route_lavender_somber.ogg` | kanto biome ambience pool |
| `ambience.kanto.coastal_to_bill` | `ambience/kanto/coastal_to_bill.ogg` | kanto biome ambience pool |
| `ambience.kanto.cave_mt_moon` | `ambience/kanto/cave_mt_moon.ogg` | kanto biome ambience pool |
| `ambience.kanto.route_cerulean_transition` | `ambience/kanto/route_cerulean_transition.ogg` | kanto biome ambience pool |
| `ambience.kanto.forest_viridian` | `ambience/kanto/forest_viridian.ogg` | kanto biome ambience pool |
| `ambience.kanto.plains_road_to_viridian` | `ambience/kanto/plains_road_to_viridian.ogg` | kanto biome ambience pool |
| `ambience.kanto.plains_pallet_town` | `ambience/kanto/plains_pallet_town.ogg` | kanto biome ambience pool |

### Johto ambience

| Key | File | Usage |
|---|---|---|
| `ambience.johto.route_42_mountain` | `ambience/johto/route_42_mountain.ogg` | johto biome ambience pool |
| `ambience.johto.ocean_surf` | `ambience/johto/ocean_surf.ogg` | johto biome ambience pool |
| `ambience.johto.route_38_coastal` | `ambience/johto/route_38_coastal.ogg` | johto biome ambience pool |
| `ambience.johto.forest_ecruteak` | `ambience/johto/forest_ecruteak.ogg` | johto biome ambience pool |
| `ambience.johto.forest_national_park` | `ambience/johto/forest_national_park.ogg` | johto biome ambience pool |
| `ambience.johto.cave_deep_dark_pokegear_unown` | `ambience/johto/cave_deep_dark_pokegear_unown.ogg` | johto biome ambience pool |
| `ambience.johto.cave_ruins_of_alph` | `ambience/johto/cave_ruins_of_alph.ogg` | johto biome ambience pool |
| `ambience.johto.stronghold_ruins_of_alph` | `ambience/johto/stronghold_ruins_of_alph.ogg` | ruins of alph trigger |
| `ambience.johto.cave_union_cave` | `ambience/johto/cave_union_cave.ogg` | johto biome ambience pool |
| `ambience.johto.plains_route_30` | `ambience/johto/plains_route_30.ogg` | johto biome ambience pool |
| `ambience.johto.end_sinjoh_ruins` | `ambience/johto/end_sinjoh_ruins.ogg` | johto biome ambience pool |
| `ambience.johto.route_47_harsh_mountain` | `ambience/johto/route_47_harsh_mountain.ogg` | johto biome ambience pool |
| `ambience.johto.route_26_open` | `ambience/johto/route_26_open.ogg` | johto biome ambience pool |
| `ambience.johto.cave_dragons_den` | `ambience/johto/cave_dragons_den.ogg` | johto biome ambience pool |
| `ambience.johto.cave_ice_path` | `ambience/johto/cave_ice_path.ogg` | johto biome ambience pool |
| `ambience.johto.plains_route_29` | `ambience/johto/plains_route_29.ogg` | johto biome ambience pool |
| `ambience.johto.plains_new_bark_town` | `ambience/johto/plains_new_bark_town.ogg` | johto biome ambience pool |

### Hoenn ambience

| Key | File | Usage |
|---|---|---|
| `ambience.hoenn.cave_of_origin` | `ambience/hoenn/cave_of_origin.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.deep_ocean_dive` | `ambience/hoenn/deep_ocean_dive.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.heavy_rain` | `ambience/hoenn/heavy_rain.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.desert_drought` | `ambience/hoenn/desert_drought.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.nether_hideout` | `ambience/hoenn/nether_hideout.ogg` | aqua magma hideout trigger |
| `ambience.hoenn.mountain_mt_pyre_exterior` | `ambience/hoenn/mountain_mt_pyre_exterior.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.mt_pyre` | `ambience/hoenn/mt_pyre.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.forest_safari_zone` | `ambience/hoenn/forest_safari_zone.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_route_120` | `ambience/hoenn/plains_route_120.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.jungle_route_119` | `ambience/hoenn/jungle_route_119.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.ocean_surf` | `ambience/hoenn/ocean_surf.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.desert_route_111` | `ambience/hoenn/desert_route_111.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.volcanic_mt_chimney` | `ambience/hoenn/volcanic_mt_chimney.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.volcanic_route_113` | `ambience/hoenn/volcanic_route_113.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_route_110` | `ambience/hoenn/plains_route_110.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.ocean_oceanic_museum` | `ambience/hoenn/ocean_oceanic_museum.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.ocean_crossing_the_sea` | `ambience/hoenn/ocean_crossing_the_sea.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.forest_petalburg_woods` | `ambience/hoenn/forest_petalburg_woods.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_route_104` | `ambience/hoenn/plains_route_104.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_oldale_town` | `ambience/hoenn/plains_oldale_town.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_route_101` | `ambience/hoenn/plains_route_101.ogg` | hoenn biome ambience pool |
| `ambience.hoenn.plains_littleroot_town` | `ambience/hoenn/plains_littleroot_town.ogg` | hoenn biome ambience pool |

### Sinnoh ambience

| Key | File | Usage |
|---|---|---|
| `ambience.sinnoh.end_distortion_world` | `ambience/sinnoh/end_distortion_world.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.mountain_mt_coronet` | `ambience/sinnoh/mountain_mt_coronet.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.snowy_snowpoint_city` | `ambience/sinnoh/snowy_snowpoint_city.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.snowy_route_216` | `ambience/sinnoh/snowy_route_216.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_route_210` | `ambience/sinnoh/plains_route_210.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_route_209` | `ambience/sinnoh/plains_route_209.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.forest_eterna_forest` | `ambience/sinnoh/forest_eterna_forest.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_route_205` | `ambience/sinnoh/plains_route_205.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.forest_floaroma_town` | `ambience/sinnoh/forest_floaroma_town.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.cave_oreburgh_mine` | `ambience/sinnoh/cave_oreburgh_mine.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_route_203` | `ambience/sinnoh/plains_route_203.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_sandgem_town` | `ambience/sinnoh/plains_sandgem_town.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.cave_lake_caverns` | `ambience/sinnoh/cave_lake_caverns.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.ocean_lake` | `ambience/sinnoh/ocean_lake.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_route_201` | `ambience/sinnoh/plains_route_201.ogg` | sinnoh biome ambience pool |
| `ambience.sinnoh.plains_twinleaf_town` | `ambience/sinnoh/plains_twinleaf_town.ogg` | sinnoh biome ambience pool |

### Unova ambience

| Key | File | Usage |
|---|---|---|
| `ambience.unova.snowy_frozen_city` | `ambience/unova/snowy_frozen_city.ogg` | unova biome ambience pool |
| `ambience.unova.cave_underground_ruins` | `ambience/unova/cave_underground_ruins.ogg` | unova biome ambience pool |
| `ambience.unova.stronghold_underground_ruins` | `ambience/unova/stronghold_underground_ruins.ogg` | underground ruins trigger |
| `ambience.unova.plains_route_19_summer` | `ambience/unova/plains_route_19_summer.ogg` | unova biome ambience pool |
| `ambience.unova.plains_aspertia_city` | `ambience/unova/plains_aspertia_city.ogg` | unova biome ambience pool |

### Gym zones

| Key | File | Usage |
|---|---|---|
| `ambience.gym.hoenn_gym` | `ambience/gym/hoenn_gym.ogg` | regional gym zone |
| `ambience.gym.kanto_gym` | `ambience/gym/kanto_gym.ogg` | regional gym zone |
| `ambience.gym.unova_gym` | `ambience/gym/unova_gym.ogg` | regional gym zone |
| `ambience.gym.johto_gym` | `ambience/gym/johto_gym.ogg` | regional gym zone |
| `ambience.gym.sinnoh_gym` | `ambience/gym/sinnoh_gym.ogg` | regional gym zone |

### Poké Center zones

| Key | File | Usage |
|---|---|---|
| `ambience.pokecenter.kanto_center` | `ambience/pokecenter/kanto_center.ogg` | pokecenter zone pool |
| `ambience.pokecenter.hoenn_center` | `ambience/pokecenter/hoenn_center.ogg` | pokecenter zone pool |
| `ambience.pokecenter.unova_center` | `ambience/pokecenter/unova_center.ogg` | pokecenter zone pool |
| `ambience.pokecenter.sinnoh_center` | `ambience/pokecenter/sinnoh_center.ogg` | pokecenter zone pool |
| `ambience.pokecenter.johto_center` | `ambience/pokecenter/johto_center.ogg` | pokecenter zone pool |

### Poké Mart zones

| Key | File | Usage |
|---|---|---|
| `ambience.pokemart.mart3` | `ambience/pokemart/mart3.ogg` | pokemart zone pool |
| `ambience.pokemart.mart2` | `ambience/pokemart/mart2.ogg` | pokemart zone pool |
| `ambience.pokemart.mart1` | `ambience/pokemart/mart1.ogg` | pokemart zone pool |

### Cobblemon Gimmighoul tower reuse

The six registered Cobblemon Gimmighoul tower structures map to one shared zone and reuse three existing ambience events. Supported IDs are `cobblemon:ruins/deserted_gimmi_tower`, `cobblemon:ruins/frozen_gimmi_tower`, `cobblemon:ruins/lush_gimmi_tower`, `cobblemon:ruins/rooted_gimmi_tower`, `cobblemon:ruins/sunscorched_gimmi_tower`, and `cobblemon:ruins/temperate_gimmi_tower`.

| Existing key | Usage |
|---|---|
| `ambience.kanto.pokemon_tower` | Gimmighoul tower creepy pool |
| `ambience.kanto.deep_dark_lavender_town` | Gimmighoul tower creepy pool |
| `ambience.johto.cave_deep_dark_pokegear_unown` | Gimmighoul tower creepy pool |

### Exact Cobbleverse structures

The detector recognizes all 71 registered structures in the supplied Cobbleverse main, Johto, Hoenn, and Sinnoh datapacks. Gym and League structures reuse their regional Gym ambience, while the exact structures below use dedicated events where available. Current nested legendary/mythical registry paths are normalized to these existing zone IDs, so no duplicate sound events are required.

| Key | File | Usage |
|---|---|---|
| `ambience.structure.wild_plant` | `ambience/structure/wild_plant.ogg` | exact structure wind_plant |
| `ambience.structure.split_decision_temple` | `ambience/structure/split_decision_temple.ogg` | exact structure split_decision_temple |
| `ambience.structure.spear_pillar` | `ambience/structure/spear_pillar.ogg` | exact structure spear_pillar |
| `ambience.structure.snowpoint_temple` | `ambience/structure/snowpoint_temple.ogg` | exact structure snowpoint_temple |
| `ambience.structure.route210` | `ambience/structure/route210.ogg` | exact structure flower_paradise |
| `ambience.structure.manaphy` | `ambience/structure/manaphy.ogg` | exact structure manaphy |
| `ambience.structure.eterna_building` | `ambience/structure/eterna_building.ogg` | exact structure eterna_building |
| `ambience.structure.crescent_isle` | `ambience/structure/crescent_isle.ogg` | exact structure crescent_isle |
| `ambience.structure.fullmoon_island` | `ambience/structure/fullmoon_island.ogg` | exact structure fullmoon_island |
| `ambience.structure.zapdos` | `ambience/structure/zapdos.ogg` | exact structure zapdos |
| `ambience.structure.moltres` | `ambience/structure/moltres.ogg` | exact structure moltres |
| `ambience.structure.mew` | `ambience/structure/mew.ogg` | exact structure mew |
| `ambience.structure.crown_cemetery` | `ambience/structure/crown_cemetery.ogg` | exact structure crown_cemetery |
| `ambience.structure.ash` | `ambience/structure/ash.ogg` | exact structure ash |
| `ambience.structure.articuno` | `ambience/structure/articuno.ogg` | exact structure articuno |
| `ambience.structure.whirl_island` | `ambience/structure/whirl_island.ogg` | exact structure whirl_island |
| `ambience.structure.celebi_shrine` | `ambience/structure/celebi_shrine.ogg` | exact structure celebi_shrine |
| `ambience.structure.burned_tower` | `ambience/structure/burned_tower.ogg` | exact structure burned_tower |
| `ambience.structure.bell_tower` | `ambience/structure/bell_tower.ogg` | exact structure bell_tower |
| `ambience.structure.sky_pillar` | `ambience/structure/sky_pillar.ogg` | exact structure sky_pillar |
| `ambience.structure.secret_garden` | `ambience/structure/secret_garden.ogg` | exact structure secret_garden |
| `ambience.structure.registeel` | `ambience/structure/registeel.ogg` | exact structure registeel |
| `ambience.structure.regirock` | `ambience/structure/regirock.ogg` | exact structure regirock |
| `ambience.structure.regice` | `ambience/structure/regice.ogg` | exact structure regice |
| `ambience.structure.kyogre` | `ambience/structure/kyogre.ogg` | exact structure kyogre |
| `ambience.structure.jirachi` | `ambience/structure/jirachi.ogg` | exact structure jirachi |
| `ambience.structure.groudon` | `ambience/structure/groudon.ogg` | exact structure groudon |
| `ambience.structure.dyna_tree` | `ambience/structure/dyna_tree.ogg` | exact structure dyna_tree |
| `ambience.structure.deoxys` | `ambience/structure/deoxys.ogg` | exact structure deoxys |

### Vanilla and BCA structure pools

| Key | File | Usage |
|---|---|---|
| `ambience.vanilla.village_taiga.fortree_city` | `ambience/vanilla/village_taiga/fortree_city.ogg` | village taiga structure pool |
| `ambience.vanilla.village_taiga.eterna_city` | `ambience/vanilla/village_taiga/eterna_city.ogg` | village taiga structure pool |
| `ambience.vanilla.village_taiga.celestic_town` | `ambience/vanilla/village_taiga/celestic_town.ogg` | village taiga structure pool |
| `ambience.vanilla.village_taiga.canalave_city` | `ambience/vanilla/village_taiga/canalave_city.ogg` | village taiga structure pool |
| `ambience.vanilla.village_snowy.snowpoint_city` | `ambience/vanilla/village_snowy/snowpoint_city.ogg` | village snowy structure pool |
| `ambience.vanilla.village_snowy.mahogany_town` | `ambience/vanilla/village_snowy/mahogany_town.ogg` | village snowy structure pool |
| `ambience.vanilla.village_snowy.icirrus_city` | `ambience/vanilla/village_snowy/icirrus_city.ogg` | village snowy structure pool |
| `ambience.vanilla.village_savanna.verdantuf_town` | `ambience/vanilla/village_savanna/verdantuf_town.ogg` | village savanna structure pool |
| `ambience.vanilla.village_savanna.goldenrod_town` | `ambience/vanilla/village_savanna/goldenrod_town.ogg` | village savanna structure pool |
| `ambience.vanilla.village_savanna.floccesy_town` | `ambience/vanilla/village_savanna/floccesy_town.ogg` | village savanna structure pool |
| `ambience.vanilla.village_savanna.azalea_town` | `ambience/vanilla/village_savanna/azalea_town.ogg` | village savanna structure pool |
| `ambience.vanilla.village_plains.twinleaf_town` | `ambience/vanilla/village_plains/twinleaf_town.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.rustboro_city` | `ambience/vanilla/village_plains/rustboro_city.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.pewter_city` | `ambience/vanilla/village_plains/pewter_city.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.pallet_town` | `ambience/vanilla/village_plains/pallet_town.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.nuvema_town` | `ambience/vanilla/village_plains/nuvema_town.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.newbark_town` | `ambience/vanilla/village_plains/newbark_town.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.littleroot_town` | `ambience/vanilla/village_plains/littleroot_town.ogg` | village plains structure pool |
| `ambience.vanilla.village_plains.cherrygrove_city` | `ambience/vanilla/village_plains/cherrygrove_city.ogg` | village plains structure pool |
| `ambience.vanilla.village_desert.rustboro_city` | `ambience/vanilla/village_desert/rustboro_city.ogg` | village desert structure pool |
| `ambience.vanilla.village_desert.route_111` | `ambience/vanilla/village_desert/route_111.ogg` | village desert structure pool |
| `ambience.vanilla.village_desert.lentimas_town` | `ambience/vanilla/village_desert/lentimas_town.ogg` | village desert structure pool |
| `ambience.vanilla.village_desert.fallarbor_town` | `ambience/vanilla/village_desert/fallarbor_town.ogg` | village desert structure pool |
| `ambience.vanilla.trial_chambers.victory_road` | `ambience/vanilla/trial_chambers/victory_road.ogg` | trial chambers structure pool |
| `ambience.vanilla.trial_chambers.team_galactic` | `ambience/vanilla/trial_chambers/team_galactic.ogg` | trial chambers structure pool with exact structure reuse |
| `ambience.vanilla.trial_chambers.teamplasma_hq` | `ambience/vanilla/trial_chambers/teamplasma_hq.ogg` | trial chambers structure pool |
| `ambience.vanilla.trial_chambers.silph_co` | `ambience/vanilla/trial_chambers/silph_co.ogg` | trial chambers structure pool |
| `ambience.vanilla.trail_ruins.solaceon_ruins` | `ambience/vanilla/trail_ruins/solaceon_ruins.ogg` | trail ruins structure pool |
| `ambience.vanilla.trail_ruins.sevault_canyon` | `ambience/vanilla/trail_ruins/sevault_canyon.ogg` | trail ruins structure pool |
| `ambience.vanilla.trail_ruins.ruins_of_alph` | `ambience/vanilla/trail_ruins/ruins_of_alph.ogg` | trail ruins structure pool |
| `ambience.vanilla.trail_ruins.abyssal_ruins` | `ambience/vanilla/trail_ruins/abyssal_ruins.ogg` | trail ruins structure pool |
| `ambience.vanilla.swamp_hut.losrtlorn_forest` | `ambience/vanilla/swamp_hut/losrtlorn_forest.ogg` | swamp hut structure pool |
| `ambience.vanilla.swamp_hut.great_marsh` | `ambience/vanilla/swamp_hut/great_marsh.ogg` | swamp hut structure pool |
| `ambience.vanilla.swamp_hut.ecruteak_city` | `ambience/vanilla/swamp_hut/ecruteak_city.ogg` | swamp hut structure pool |
| `ambience.vanilla.stronghold.spear_pillar` | `ambience/vanilla/stronghold/spear_pillar.ogg` | stronghold structure pool with exact structure reuse |
| `ambience.vanilla.stronghold.mt_coronet` | `ambience/vanilla/stronghold/mt_coronet.ogg` | stronghold structure pool |
| `ambience.vanilla.stronghold.dragonpsiral` | `ambience/vanilla/stronghold/dragonpsiral.ogg` | stronghold structure pool |
| `ambience.vanilla.shipwreck.ss_aqua` | `ambience/vanilla/shipwreck/ss_aqua.ogg` | shipwreck structure pool |
| `ambience.vanilla.shipwreck.plasma_frigate` | `ambience/vanilla/shipwreck/plasma_frigate.ogg` | shipwreck structure pool |
| `ambience.vanilla.shipwreck.abandoned_ship` | `ambience/vanilla/shipwreck/abandoned_ship.ogg` | shipwreck structure pool |
| `ambience.vanilla.ruined_portal.radio_signal` | `ambience/vanilla/ruined_portal/radio_signal.ogg` | ruined portal structure pool |
| `ambience.vanilla.ruined_portal.distorn_world` | `ambience/vanilla/ruined_portal/distorn_world.ogg` | ruined portal structure pool |
| `ambience.vanilla.pillager_outpost.team_aqua_magma_hideout` | `ambience/vanilla/pillager_outpost/team_aqua_magma_hideout.ogg` | pillager outpost structure pool |
| `ambience.vanilla.pillager_outpost.rocket_hideout` | `ambience/vanilla/pillager_outpost/rocket_hideout.ogg` | pillager outpost structure pool |
| `ambience.vanilla.pillager_outpost.radio_tower_takeover` | `ambience/vanilla/pillager_outpost/radio_tower_takeover.ogg` | pillager outpost structure pool with exact structure reuse |
| `ambience.vanilla.ocean_ruin.underwater` | `ambience/vanilla/ocean_ruin/underwater.ogg` | ocean ruin structure pool |
| `ambience.vanilla.ocean_ruin.marine_tube` | `ambience/vanilla/ocean_ruin/marine_tube.ogg` | ocean ruin structure pool |
| `ambience.vanilla.ocean_ruin.dive_theme` | `ambience/vanilla/ocean_ruin/dive_theme.ogg` | ocean ruin structure pool |
| `ambience.vanilla.nether_fossil.strange_house` | `ambience/vanilla/nether_fossil/strange_house.ogg` | nether fossil structure pool |
| `ambience.vanilla.nether_fossil.poke_tower` | `ambience/vanilla/nether_fossil/poke_tower.ogg` | nether fossil structure pool |
| `ambience.vanilla.nether_fossil.mt_pyre` | `ambience/vanilla/nether_fossil/mt_pyre.ogg` | nether fossil structure pool |
| `ambience.vanilla.nether_fossil.lavender_town` | `ambience/vanilla/nether_fossil/lavender_town.ogg` | nether fossil structure pool |
| `ambience.vanilla.monument.whirl_islands` | `ambience/vanilla/monument/whirl_islands.ogg` | monument structure pool |
| `ambience.vanilla.monument.cave_origin` | `ambience/vanilla/monument/cave_origin.ogg` | monument structure pool |
| `ambience.vanilla.mineshaft_mesa.stark_mountain` | `ambience/vanilla/mineshaft_mesa/stark_mountain.ogg` | mineshaft mesa structure pool |
| `ambience.vanilla.mineshaft_mesa.reversal_mountain` | `ambience/vanilla/mineshaft_mesa/reversal_mountain.ogg` | mineshaft mesa structure pool |
| `ambience.vanilla.mineshaft_mesa.chargestone_cave` | `ambience/vanilla/mineshaft_mesa/chargestone_cave.ogg` | mineshaft mesa structure pool |
| `ambience.vanilla.mineshaft.union_cave` | `ambience/vanilla/mineshaft/union_cave.ogg` | mineshaft structure pool |
| `ambience.vanilla.mineshaft.oreburgh_mine` | `ambience/vanilla/mineshaft/oreburgh_mine.ogg` | mineshaft structure pool |
| `ambience.vanilla.mineshaft.mt_moon` | `ambience/vanilla/mineshaft/mt_moon.ogg` | mineshaft structure pool |
| `ambience.vanilla.mansion.strange_house` | `ambience/vanilla/mansion/strange_house.ogg` | mansion structure pool |
| `ambience.vanilla.mansion.poke_mansion` | `ambience/vanilla/mansion/poke_mansion.ogg` | mansion structure pool |
| `ambience.vanilla.mansion.old_chateau` | `ambience/vanilla/mansion/old_chateau.ogg` | mansion structure pool |
| `ambience.vanilla.jungle_pyramid.safari_zone` | `ambience/vanilla/jungle_pyramid/safari_zone.ogg` | jungle pyramid structure pool |
| `ambience.vanilla.jungle_pyramid.pinwheel_forest` | `ambience/vanilla/jungle_pyramid/pinwheel_forest.ogg` | jungle pyramid structure pool |
| `ambience.vanilla.jungle_pyramid.island_cave` | `ambience/vanilla/jungle_pyramid/island_cave.ogg` | jungle pyramid structure pool |
| `ambience.vanilla.jungle_pyramid.ilex_forest` | `ambience/vanilla/jungle_pyramid/ilex_forest.ogg` | jungle pyramid structure pool |
| `ambience.vanilla.igloo.shoal_cave` | `ambience/vanilla/igloo/shoal_cave.ogg` | igloo structure pool |
| `ambience.vanilla.igloo.ice_path` | `ambience/vanilla/igloo/ice_path.ogg` | igloo structure pool |
| `ambience.vanilla.igloo.cold_storage` | `ambience/vanilla/igloo/cold_storage.ogg` | igloo structure pool |
| `ambience.vanilla.fortress.stark_mountain` | `ambience/vanilla/fortress/stark_mountain.ogg` | fortress structure pool |
| `ambience.vanilla.fortress.rocket_hq` | `ambience/vanilla/fortress/rocket_hq.ogg` | fortress structure pool |
| `ambience.vanilla.fortress.mt_ember` | `ambience/vanilla/fortress/mt_ember.ogg` | fortress structure pool |
| `ambience.vanilla.fortress.hideout` | `ambience/vanilla/fortress/hideout.ogg` | fortress structure pool |
| `ambience.vanilla.end_city.spear_pillar` | `ambience/vanilla/end_city/spear_pillar.ogg` | end city structure pool with exact structure reuse |
| `ambience.vanilla.end_city.n_castle` | `ambience/vanilla/end_city/n_castle.ogg` | end city structure pool |
| `ambience.vanilla.end_city.mt_chimney` | `ambience/vanilla/end_city/mt_chimney.ogg` | end city structure pool |
| `ambience.vanilla.end_city.hall_of_origin` | `ambience/vanilla/end_city/hall_of_origin.ogg` | end city structure pool |
| `ambience.vanilla.desert_pyramid.relic_castle` | `ambience/vanilla/desert_pyramid/relic_castle.ogg` | desert pyramid structure pool |
| `ambience.vanilla.desert_pyramid.mirage_tower` | `ambience/vanilla/desert_pyramid/mirage_tower.ogg` | desert pyramid structure pool |
| `ambience.vanilla.desert_pyramid.desert_ruins` | `ambience/vanilla/desert_pyramid/desert_ruins.ogg` | desert pyramid structure pool |
| `ambience.vanilla.buried_treasure.undella_town` | `ambience/vanilla/buried_treasure/undella_town.ogg` | buried treasure structure pool |
| `ambience.vanilla.buried_treasure.route109` | `ambience/vanilla/buried_treasure/route109.ogg` | buried treasure structure pool |
| `ambience.vanilla.buried_treasure.lacunosa_town` | `ambience/vanilla/buried_treasure/lacunosa_town.ogg` | buried treasure structure pool |
| `ambience.vanilla.bca_village_large.violet` | `ambience/vanilla/bca_village_large/violet.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.violet` | `ambience/vanilla/bca_village_mid/violet.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.violet` | `ambience/vanilla/bca_village_small/violet.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.verdanturf` | `ambience/vanilla/bca_village_large/verdanturf.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.verdanturf` | `ambience/vanilla/bca_village_mid/verdanturf.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.verdanturf` | `ambience/vanilla/bca_village_small/verdanturf.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.rustboro` | `ambience/vanilla/bca_village_large/rustboro.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.rustboro` | `ambience/vanilla/bca_village_mid/rustboro.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.rustboro` | `ambience/vanilla/bca_village_small/rustboro.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.fallarbor` | `ambience/vanilla/bca_village_large/fallarbor.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.fallarbor` | `ambience/vanilla/bca_village_mid/fallarbor.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.fallarbor` | `ambience/vanilla/bca_village_small/fallarbor.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.cinnabar` | `ambience/vanilla/bca_village_large/cinnabar.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.cinnabar` | `ambience/vanilla/bca_village_mid/cinnabar.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.cinnabar` | `ambience/vanilla/bca_village_small/cinnabar.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.cerulean_city` | `ambience/vanilla/bca_village_large/cerulean_city.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.cerulean_city` | `ambience/vanilla/bca_village_mid/cerulean_city.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.cerulean_city` | `ambience/vanilla/bca_village_small/cerulean_city.ogg` | bca village small structure pool |
| `ambience.vanilla.bca_village_large.celadon_city` | `ambience/vanilla/bca_village_large/celadon_city.ogg` | bca village large structure pool |
| `ambience.vanilla.bca_village_mid.celadon_city` | `ambience/vanilla/bca_village_mid/celadon_city.ogg` | bca village mid structure pool |
| `ambience.vanilla.bca_village_small.celadon_city` | `ambience/vanilla/bca_village_small/celadon_city.ogg` | bca village small structure pool |
| `ambience.vanilla.bastion_remnant.reversal_mountain` | `ambience/vanilla/bastion_remnant/reversal_mountain.ogg` | bastion remnant structure pool |
| `ambience.vanilla.bastion_remnant.hideout` | `ambience/vanilla/bastion_remnant/hideout.ogg` | bastion remnant structure pool |
| `ambience.vanilla.ancient_city.pokemon_mansion` | `ambience/vanilla/ancient_city/pokemon_mansion.ogg` | ancient city structure pool |
| `ambience.vanilla.ancient_city.mt_silver` | `ambience/vanilla/ancient_city/mt_silver.ogg` | ancient city structure pool |
| `ambience.vanilla.ancient_city.dragonspiral_tower` | `ambience/vanilla/ancient_city/dragonspiral_tower.ogg` | ancient city structure pool |
| `ambience.vanilla.ancient_city.distortion_world` | `ambience/vanilla/ancient_city/distortion_world.ogg` | ancient city structure pool |
| `ambience.vanilla.ancient_city.cerulean_cave` | `ambience/vanilla/ancient_city/cerulean_cave.ogg` | ancient city structure pool |

### Cobblemon 1.8 structure reuse mappings

Cobblemon 1.8 adds no new CobbleTunes sound keys here. The server detector uses exact registered `StructureStart` IDs from the supplied 1.8.0 JAR and routes them into the existing structure pools. The release notes describe 49 new habitat structures; the actual worldgen registry in the JAR exposes **32 habitat Structure IDs**, with additional habitat content represented by templates/pools rather than distinct registered Structure IDs.

| Cobblemon structure IDs | Reused CobbleTunes pool |
|---|---|
| `habitats/berry_patch`, `habitats/flowerbed_clearing`, `habitats/freshwater_pond`, `habitats/pinkflowerbed_clearing`, `habitats/sunflowerbed_clearing`, `habitats/zen_garden` | `jungle_pyramid` |
| `habitats/sandpit_clearing`, `habitats/sunscorched_clearing` | `desert_pyramid` |
| `habitats/badlands_shaded_rock`, `habitats/birch_wildfire_scar`, `habitats/fae_mounds`, `habitats/meteorite_impact`, `habitats/natural_lightningrod`, `habitats/oak_wildfire_scar`, `habitats/spruce_wildfire_scar` | `trail_ruins` |
| `habitats/bug_mound`, `habitats/lush_canopy`, `habitats/lush_cenote`, `habitats/reclaimed_lush_monument` | `jungle_pyramid` |
| `habitats/carved_ice_spikes`, `habitats/drifting_icebergs`, `habitats/snowy_burrow`, `habitats/snowy_grotto`, `habitats/snowy_thermal_vents` | `igloo` |
| `habitats/deep_sea_spire` | `ocean_ruin` |
| `habitats/desert_oasis`, `habitats/desert_shaded_rock`, `habitats/reclaimed_deserted_monument` | `desert_pyramid` |
| `habitats/fungal_dwelling`, `habitats/lush_peat_bog`, `habitats/parched_peat_bog` | `swamp_hut` |
| `habitats/thermal_crevices` | `mineshaft_mesa` |
| `ruins/ancient_dais_ruins`, `ruins/deserted_house_ruins`, `ruins/deserted_tower_ruins`, `ruins/deserted_town_center_ruins`, `ruins/fallen_statue_ruins`, `ruins/luna_henge_ruins`, `ruins/meteor_battleground_ruins`, `ruins/rooted_arch_ruins`, `ruins/sol_henge_ruins`, `ruins/stonjourner_henge_ruins`, `ruins/toppled_pillars_ruins` | `trail_ruins` |
| `ruins/crumbling_arch_ruins`, `ruins/hidden_bunker_ruins`, `ruins/mossy_oubliette_ruins` | `stronghold` |
| `ruins/decaying_crypt_ruins` | `nether_fossil` |
| `ruins/deserted_monument_ruins`, `ruins/sunscorched_shaded_ruins` | `desert_pyramid` |
| `ruins/frozen_altar_ruins` | `igloo` |
| `ruins/lush_monument_ruins` | `jungle_pyramid` |
| `ruins/old_garden_ruins` | `mansion` |
| `ruins/overgrown_trial_ruins` | `trial_chambers` |
| `ruins/submerged_forge_ruins` | `ocean_ruin` |
| `ruins/unstable_cave_ruins` | `mineshaft` |
| `ruins/deserted_gimmi_tower`, `ruins/frozen_gimmi_tower`, `ruins/lush_gimmi_tower`, `ruins/rooted_gimmi_tower`, `ruins/sunscorched_gimmi_tower`, `ruins/temperate_gimmi_tower` | dedicated `cobbletunes:gimmighoul_tower` pool |
| `shipwreck_coves/lush_shipwreck_cove`, `shipwreck_coves/magma_shipwreck_cove`, `shipwreck_coves/submerged_shipwreck_cove`, `fishing_boat/beach`, `fishing_boat/deep_ocean`, `fishing_boat/warm_ocean` | `shipwreck` |

This includes 1.8's new Magma Shipwreck Cove. The **Habitat Block** is not part of this mapping because it is a placeable spawner block rather than a registered worldgen `StructureStart`; custom Habitat Block areas can use manual marker zones instead.

### Terralith structure reuse mappings

Terralith adds no CobbleTunes sound keys. The detector maps all 26 structures referenced by the supplied Terralith structure sets to existing pools. `terralith:underground/witch_hut` and `terralith:underground_cabin` exist as structure definitions in the datapack but are not referenced by those structure sets, so they are not included in the generated-structure mapping.

| Terralith structure family | Reused CobbleTunes pool |
|---|---|
| `desert_outpost` | `pillager_outpost` |
| `fortified_desert_village` | `village_desert` |
| `fortified_village` | `bca_village_large` |
| `glacial_hut`, `igloo` | `igloo` |
| `mage_complex`, `mage_tower`, `mage_tower_autumn`, `mage_tower_spring`, `mage_tower_summer`, `mage_tower_winter` | `mansion` |
| `rubble_desert`, `rubble_forest`, `rubble_jungle`, `rubble_mesa`, `rubble_mountain`, `rubble_taiga` | `trail_ruins` |
| `spire` | `end_city` |
| `underground/frosted_dungeon` | `stronghold` |
| `underground/giant_bee_hive` | `jungle_pyramid` |
| `underground/mining_outpost` | `mineshaft` |
| `underground/oak_cabin`, `valley_lodge` | `village_taiga` |
| `underground/old_refinery` | `mineshaft_mesa` |
| `underground/sunken_tower` | `ocean_ruin` |
| `witch_hut` | `swamp_hut` |

### Repurposed Structures reuse mappings

Repurposed Structures `7.5.21+1.21.1` adds no CobbleTunes sound keys. The detector maps every worldgen structure ID found in the attached JAR to the closest existing structure pool.

| Repurposed Structures family | Reused CobbleTunes pool |
|---|---|
| `ancient_city_*` | `ancient_city` |
| `bastion_underground` | `bastion_remnant` |
| `city_overworld` | `bca_village_large` |
| `city_nether`, `fortress_jungle`, `temple_nether_*` | `fortress` |
| `igloo_*`, `pyramid_icy`, `pyramid_snowy` | `igloo` |
| `mansion_*` | `mansion` |
| `mineshaft_*` | `mineshaft` |
| `monument_*`, `pyramid_ocean`, `temple_ocean` | `monument` |
| `outpost_*` | `pillager_outpost` |
| `pyramid_badlands`, `pyramid_end`, `pyramid_nether` | `desert_pyramid` |
| `pyramid_dark_forest`, `pyramid_flower_forest`, `pyramid_giant_tree_taiga`, `pyramid_jungle`, `pyramid_mushroom`, `temple_taiga` | `jungle_pyramid` |
| `ruined_portal_end`, `ruins_nether` | `ruined_portal` |
| `ruins_land_*` | `trail_ruins` |
| `shipwreck_*` | `shipwreck` |
| `stronghold_*` | `stronghold` |
| `village_badlands` | `village_desert` |
| `village_birch`, `village_cherry`, `village_mushroom`, `village_oak`, `village_ocean` | `village_plains` |
| `village_bamboo`, `village_crimson`, `village_jungle` | `village_savanna` |
| `village_dark_forest`, `village_giant_taiga`, `village_mountains`, `village_swamp`, `village_warped` | `village_taiga` |
| `witch_hut_*` | `swamp_hut` |

This covers all 107 `data/repurposed_structures/worldgen/structure/*.json` entries present in the inspected JAR. The exact IDs are kept in `StructureZoneDetector.kt` so detection follows the same real bounding-box checks used by vanilla, BCA, and Cobbleverse structures.

## Menu music

| Key | File | Usage |
|---|---|---|
| `menu.hgss` | `menu/hgss.ogg` | title screen and submenu pool |
| `menu.frlg` | `menu/frlg.ogg` | title screen and submenu pool |
| `menu.emerald` | `menu/emerald.ogg` | title screen and submenu pool |

## Evolution music

Evolution cues are fully client-side and do not need a server packet. Visible world evolutions are positional: suspense plays at `0.62x` the configured CobbleTunes music volume and completion at `0.78x`, both under Minecraft's Records/Jukebox slider with linear attenuation out to about 32 blocks. The watcher waits 20 ticks after `isEvolving` becomes true so suspense lines up with Cobblemon's visible animation. When `isEvolving` becomes false, any remaining suspense is stopped and the completion sting plays. World ambience remains at `0.20x` for the complete audible sequence, including the congratulations cue, and starts a 2-second fade back to normal only after the last evolution sound has finished.

When an evolution is completed from Cobblemon's Summary UI with no live matching world `PokemonEntity`, CobbleTunes snapshots `CobblemonClient.storage.party` when Summary opens and detects a same-UUID species change from that synchronized client party store. The watch remains active for 10 seconds after the Summary closes so the final storage sync is not lost. It does not depend on the Summary screen's own Pokémon references, which can stay stale during evolution. The completion cue plays from the player's position at `0.78x`; this fallback has no suspense, but it ducks world ambience/structure music to `0.20x` while the congratulations cue is audible and then restores it with the same 2-second fade. A live nearby world entity or active spatial evolution suppresses it to avoid duplicate completion audio.

Alolan, Galarian, Hisuian, and Paldean forms override the base National Dex region. Alola and Galar each have two suspense variants and choose randomly between available files. If an evolution OGG is missing from the active resource packs, that cue stays silent; missing suspense also does not duck world music. Multiple nearby world evolutions can play at the same time from independent positions. In a ZIP resource pack, these paths must resolve directly under `assets/cobbletunes/sounds/evolution/` at archive root, with no extra wrapper directory.

| Key | File | Usage |
|---|---|---|
| `evolution.fr.evo` | `evolution/fr_evo.ogg` | Kanto suspense |
| `evolution.fr.congrat` | `evolution/fr_congrat.ogg` | Kanto completion |
| `evolution.hg.evo` | `evolution/hg_evo.ogg` | Johto suspense |
| `evolution.hg.congrat` | `evolution/hg_congrat.ogg` | Johto completion |
| `evolution.em.evo` | `evolution/em_evo.ogg` | Hoenn suspense |
| `evolution.em.congrat` | `evolution/em_congrat.ogg` | Hoenn completion |
| `evolution.plat.evo` | `evolution/plat_evo.ogg` | Sinnoh suspense |
| `evolution.plat.congrat` | `evolution/plat_congrat.ogg` | Sinnoh completion |
| `evolution.bl.evo` | `evolution/bl_evo.ogg` | Unova suspense |
| `evolution.bl.congrat` | `evolution/bl_congrat.ogg` | Unova completion |
| `evolution.xy.evo` | `evolution/xy_evo.ogg` | Kalos suspense |
| `evolution.xy.congrat` | `evolution/xy_congrat.ogg` | Kalos completion |
| `evolution.um.evo` | `evolution/um_evo.ogg` | Alola suspense variant 1 |
| `evolution.um.evo2` | `evolution/um_evo2.ogg` | Alola suspense variant 2 |
| `evolution.um.congrat` | `evolution/um_congrat.ogg` | Alola completion |
| `evolution.swsh.evo` | `evolution/swsh_evo.ogg` | Galar suspense variant 1 |
| `evolution.swsh.evo2` | `evolution/swsh_evo2.ogg` | Galar suspense variant 2 |
| `evolution.swsh.congrat` | `evolution/swsh_congrat.ogg` | Galar completion |
| `evolution.leg.evo` | `evolution/leg_evo.ogg` | Hisui suspense |
| `evolution.leg.congrat` | `evolution/leg_congrat.ogg` | Hisui completion |
| `evolution.sv.evo` | `evolution/sv_evo.ogg` | Paldea suspense |
| `evolution.sv.congrat` | `evolution/sv_congrat.ogg` | Paldea completion |

## Effects

| Key | File | Usage |
|---|---|---|
| `effect.lowhp` | `effect/lowhp.ogg` | low hp battle cue |

## Resource pack notes

- Keep every path lowercase and exact.
- Keep music entries streamed.
- `effect.lowhp` is played once per low-HP trigger at `0.80x` the configured CobbleTunes music volume.
- Missing files stay silent instead of crashing CobbleTunes.
- `/playsound cobbletunes:<key> music @s` can be used to test normal music keys directly.
- `/playsound cobbletunes:evolution.fr.evo record @s` can be used to test an evolution key through the Records/Jukebox category.
