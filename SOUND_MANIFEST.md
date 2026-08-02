# CobbleTunes — Sound Manifest

This is the authoritative reference for anyone building a resource pack for CobbleTunes. Every row below is a registered `SoundEvent` — if the file at the listed path doesn't exist in your resource pack, that specific context just plays nothing (no error, no crash).

**Base path for everything:** `assets/cobbletunes/sounds/`
**File format:** `.ogg`, mono or stereo, any sample rate Minecraft accepts.

Column legend:
- **Key** — the `sounds.json` key (also usable directly via `/playsound cobbletunes:<key> music @s` for testing)
- **File path** — where to put your `.ogg`, relative to the base path above (omit the `.ogg` extension in `sounds.json`, but the actual file needs it)
- **Trigger** — what causes this sound to play

---

## 1. Battle Music

One folder per region: `battle/<region>/`. Ten regions total. Not every region has every context — some intentionally reuse one track for two roles (matches the source games, e.g. Kanto's Gym Leader/Elite Four share a theme).

### Kanto (`battle/kanto/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.kanto.wild` | `battle/kanto/wild.ogg` | Wild battle, opponent Dex 1–151 |
| `battle.kanto.trainer` | `battle/kanto/trainer.ogg` | Trainer battle, majority-vote region = Kanto |
| `battle.kanto.gym_leader` | `battle/kanto/gym_leader.ogg` | Gym Leader battle (Kanto) — also used for Elite Four |
| `battle.kanto.champion_rival` | `battle/kanto/champion_rival.ogg` | Champion battle (Kanto) — also used for PvP pool |
| `battle.kanto.legendary_default` | `battle/kanto/legendary_default.ogg` | Legendary/Mythical encounter, Kanto region default (no species override) |
| `battle.kanto.legendary_mewtwo` | `battle/kanto/legendary_mewtwo.ogg` | Mewtwo (Dex 150) specifically |
| `battle.kanto.legendary_deoxys` | `battle/kanto/legendary_deoxys.ogg` | Deoxys (Dex 386) specifically |

### Johto (`battle/johto/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.johto.wild` | `battle/johto/wild.ogg` | Wild battle, Dex 152–251 |
| `battle.johto.trainer` | `battle/johto/trainer.ogg` | Trainer battle, region = Johto |
| `battle.johto.gym_leader` | `battle/johto/gym_leader.ogg` | Gym Leader (also used for Elite Four) |
| `battle.johto.champion` | `battle/johto/champion.ogg` | Champion battle |
| `battle.johto.rival_pvp` | `battle/johto/rival_pvp.ogg` | Rival battle / PvP pool |
| `battle.johto.legendary_default` | `battle/johto/legendary_default.ogg` | Legendary default, no species override |
| `battle.johto.legendary_raikou` | `battle/johto/legendary_raikou.ogg` | Raikou (Dex 243) |
| `battle.johto.legendary_entei` | `battle/johto/legendary_entei.ogg` | Entei (Dex 244) |
| `battle.johto.legendary_suicune` | `battle/johto/legendary_suicune.ogg` | Suicune (Dex 245) |
| `battle.johto.legendary_lugia` | `battle/johto/legendary_lugia.ogg` | Lugia (Dex 249) |
| `battle.johto.legendary_ho_oh` | `battle/johto/legendary_ho_oh.ogg` | Ho-Oh (Dex 250) |
| `battle.johto.team_rocket` | `battle/johto/team_rocket.ogg` | Reserved — not currently routed (no faction-tier context yet) |

### Hoenn (`battle/hoenn/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.hoenn.wild` | `battle/hoenn/wild.ogg` | Wild battle, Dex 252–386 |
| `battle.hoenn.trainer` | `battle/hoenn/trainer.ogg` | Trainer battle, region = Hoenn |
| `battle.hoenn.gym_leader` | `battle/hoenn/gym_leader.ogg` | Gym Leader battle |
| `battle.hoenn.elite_four` | `battle/hoenn/elite_four.ogg` | Elite Four (distinct from Gym Leader) |
| `battle.hoenn.champion_wallace` | `battle/hoenn/champion_wallace.ogg` | Champion battle |
| `battle.hoenn.rival_pvp` | `battle/hoenn/rival_pvp.ogg` | Rival battle / PvP pool |
| `battle.hoenn.legendary_regis` | `battle/hoenn/legendary_regis.ogg` | Regirock/Regice/Registeel (Dex 377–379) |
| `battle.hoenn.legendary_super_ancient` | `battle/hoenn/legendary_super_ancient.ogg` | Groudon/Kyogre/Rayquaza (Dex 382–384) |
| `battle.hoenn.legendary_mew` | `battle/hoenn/legendary_mew.ogg` | Mew (Dex 151) |
| `battle.hoenn.team_aqua_magma_grunt` | `battle/hoenn/team_aqua_magma_grunt.ogg` | Reserved — no faction-tier context yet |
| `battle.hoenn.team_aqua_magma_leaders` | `battle/hoenn/team_aqua_magma_leaders.ogg` | Reserved |

### Sinnoh (`battle/sinnoh/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.sinnoh.wild` | `battle/sinnoh/wild.ogg` | Wild battle, Dex 387–493 |
| `battle.sinnoh.trainer` | `battle/sinnoh/trainer.ogg` | Trainer battle, region = Sinnoh |
| `battle.sinnoh.gym_leader` | `battle/sinnoh/gym_leader.ogg` | Gym Leader battle |
| `battle.sinnoh.elite_four` | `battle/sinnoh/elite_four.ogg` | Elite Four |
| `battle.sinnoh.champion` | `battle/sinnoh/champion.ogg` | Champion battle |
| `battle.sinnoh.rival_pvp` | `battle/sinnoh/rival_pvp.ogg` | Rival battle / PvP pool |
| `battle.sinnoh.legendary_default` | `battle/sinnoh/legendary_default.ogg` | Legendary default |
| `battle.sinnoh.legendary_alt` | `battle/sinnoh/legendary_alt.ogg` | Alternate legendary pool entry (region-tagged, no species override) |
| `battle.sinnoh.legendary_dialga_palkia` | `battle/sinnoh/legendary_dialga_palkia.ogg` | Dialga/Palkia (Dex 483–484) |
| `battle.sinnoh.legendary_giratina` | `battle/sinnoh/legendary_giratina.ogg` | Giratina (Dex 487) |
| `battle.sinnoh.legendary_lake_trio` | `battle/sinnoh/legendary_lake_trio.ogg` | Uxie/Mesprit/Azelf (Dex 480–482) |
| `battle.sinnoh.team_galactic_grunt` | `battle/sinnoh/team_galactic_grunt.ogg` | Reserved |
| `battle.sinnoh.team_galactic_commander` | `battle/sinnoh/team_galactic_commander.ogg` | Reserved |
| `battle.sinnoh.team_galactic_boss` | `battle/sinnoh/team_galactic_boss.ogg` | Reserved |
| `battle.sinnoh.frontier_brain` | `battle/sinnoh/frontier_brain.ogg` | Reserved |

### Unova (`battle/unova/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.unova.wild` | `battle/unova/wild.ogg` | Wild battle, Dex 494–649 |
| `battle.unova.trainer` | `battle/unova/trainer.ogg` | Trainer battle, region = Unova |
| `battle.unova.gym_leader` | `battle/unova/gym_leader.ogg` | Gym Leader (also used for Elite Four) |
| `battle.unova.champion_iris` | `battle/unova/champion_iris.ogg` | Champion battle |
| `battle.unova.rival_hugh` | `battle/unova/rival_hugh.ogg` | Rival battle / PvP pool |
| `battle.unova.champion_kanto_pwt` | `battle/unova/champion_kanto_pwt.ogg` | PvP pool — PWT-style Kanto champion remix |
| `battle.unova.champion_johto_pwt` | `battle/unova/champion_johto_pwt.ogg` | PvP pool — PWT-style Johto champion remix |
| `battle.unova.champion_hoenn_pwt` | `battle/unova/champion_hoenn_pwt.ogg` | PvP pool — PWT-style Hoenn champion remix |
| `battle.unova.champion_sinnoh_pwt` | `battle/unova/champion_sinnoh_pwt.ogg` | PvP pool — PWT-style Sinnoh champion remix |
| `battle.unova.legendary_black_white_kyurem` | `battle/unova/legendary_black_white_kyurem.ogg` | Reshiram/Zekrom/Kyurem (Dex 643–646) |

⚠️ **`team_plasma_grunt`/`team_plasma_n`/`team_plasma_colress`** are declared in code as reserved `SoundEvent`s for a future faction-tier feature, but currently have **no corresponding `sounds.json` entry** — supplying audio for them does nothing yet, and they're omitted from this table for that reason. This is a genuine gap in the current build, not a resource-pack limitation.

### Alola (`battle/alola/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.alola.wild` | `battle/alola/wild.ogg` | Wild battle, Dex 722–809 |
| `battle.alola.trainer` | `battle/alola/trainer.ogg` | Trainer battle, region = Alola |
| `battle.alola.island_kahuna` | `battle/alola/island_kahuna.ogg` | Island Kahuna battle (Alola has no Gyms — this fills the Gym Leader context) |
| `battle.alola.elite_four` | `battle/alola/elite_four.ogg` | Elite Four |
| `battle.alola.champion_summit` | `battle/alola/champion_summit.ogg` | Champion battle (also used for PvP pool) |
| `battle.alola.legendary_ultra_beast` | `battle/alola/legendary_ultra_beast.ogg` | Regional default — covers any Ultra Beast without its own override |
| `battle.alola.legendary_tapu` | `battle/alola/legendary_tapu.ogg` | Tapu Koko/Lele/Bulu/Fini (Dex 785–788) |
| `battle.alola.legendary_solgaleo_lunala_necrozma` | `battle/alola/legendary_solgaleo_lunala_necrozma.ogg` | Solgaleo/Lunala/Necrozma (Dex 791/792/800) |
| `battle.alola.legendary_necrozma_fused` | `battle/alola/legendary_necrozma_fused.ogg` | Fused Necrozma variant (Dex 800, alt pick alongside the above) |
| `battle.alola.team_skull_guzma` | `battle/alola/team_skull_guzma.ogg` | Reserved |
| `battle.alola.aether_foundation` | `battle/alola/aether_foundation.ogg` | Reserved |
| `battle.alola.aether_president_lusamine` | `battle/alola/aether_president_lusamine.ogg` | Reserved |
| `battle.alola.ultra_recon_squad` | `battle/alola/ultra_recon_squad.ogg` | Reserved |

### Galar (`battle/galar/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.galar.wild` | `battle/galar/wild.ogg` | Wild battle, Dex 810–898 |
| `battle.galar.trainer` | `battle/galar/trainer.ogg` | Trainer battle, region = Galar |
| `battle.galar.gym_leader` | `battle/galar/gym_leader.ogg` | Gym Leader battle |
| `battle.galar.league_tournament` | `battle/galar/league_tournament.ogg` | Elite Four context (Galar has no E4 — League Tournament fills this role) |
| `battle.galar.champion_leon` | `battle/galar/champion_leon.ogg` | Champion battle (also used for PvP pool) |
| `battle.galar.legendary_mysterious_being` | `battle/galar/legendary_mysterious_being.ogg` | Regional default — Dynamax Adventures-style catch-all |
| `battle.galar.legendary_eternatus` | `battle/galar/legendary_eternatus.ogg` | Eternatus (Dex 890) |
| `battle.galar.legendary_glastrier_spectrier` | `battle/galar/legendary_glastrier_spectrier.ogg` | Glastrier/Spectrier (Dex 896–897) |
| `battle.galar.battle_tower` | `battle/galar/battle_tower.ogg` | Reserved |

### Hisui (`battle/hisui/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.hisui.wild` | `battle/hisui/wild.ogg` | Wild battle, Hisui region tag (Legends: Arceus has no trainer/gym structure) |
| `battle.hisui.legendary_arceus` | `battle/hisui/legendary_arceus.ogg` | Arceus (Dex 493) |

### Kalos (`battle/kalos/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.kalos.wild` | `battle/kalos/wild.ogg` | Wild battle, Dex 650–721 |
| `battle.kalos.trainer` | `battle/kalos/trainer.ogg` | Trainer battle, region = Kalos |
| `battle.kalos.gym_leader` | `battle/kalos/gym_leader.ogg` | Gym Leader battle |
| `battle.kalos.elite_four` | `battle/kalos/elite_four.ogg` | Elite Four |
| `battle.kalos.champion` | `battle/kalos/champion.ogg` | Champion battle (also used for PvP pool) |
| `battle.kalos.rival_friend` | `battle/kalos/rival_friend.ogg` | Rival/friend battle, PvP pool |
| `battle.kalos.legendary_trio` | `battle/kalos/legendary_trio.ogg` | Xerneas/Yveltal/Zygarde (Dex 716–718) |
| `battle.kalos.team_flare_grunt` | `battle/kalos/team_flare_grunt.ogg` | Reserved |
| `battle.kalos.team_flare_lysandre` | `battle/kalos/team_flare_lysandre.ogg` | Reserved |

### Paldea (`battle/paldea/`)

| Key | File path | Trigger |
|---|---|---|
| `battle.paldea.wild` | `battle/paldea/wild.ogg` | Wild battle, Dex 906–1025 |
| `battle.paldea.trainer` | `battle/paldea/trainer.ogg` | Trainer battle, region = Paldea |
| `battle.paldea.gym_leader` | `battle/paldea/gym_leader.ogg` | Gym Leader battle |
| `battle.paldea.elite_four` | `battle/paldea/elite_four.ogg` | Elite Four |
| `battle.paldea.champion_top` | `battle/paldea/champion_top.ogg` | Champion battle (also used for PvP pool) |
| `battle.paldea.legendary_solgaleo_lunala_dlc` | `battle/paldea/legendary_solgaleo_lunala_dlc.ogg` | Indigo Disk DLC remix, Solgaleo/Lunala (Dex 791–792) |

---

## 2. Biome Ambience

One folder per region: `ambience/<region>/`. Only Kanto, Johto, Hoenn, Sinnoh, and Unova have ambience coverage (by design — no post-Unova ambience gathered). **Tracks pool across regions per matching biome** — e.g. any region's "forest"-tagged track is a candidate for any forest-type biome, not exclusive to that region.

### Kanto (`ambience/kanto/`)

| Key | File path |
|---|---|
| `ambience.kanto.plains_road_to_viridian` | `ambience/kanto/plains_road_to_viridian.ogg` |
| `ambience.kanto.plains_pallet_town` | `ambience/kanto/plains_pallet_town.ogg` |
| `ambience.kanto.route_lavender_somber` | `ambience/kanto/route_lavender_somber.ogg` |
| `ambience.kanto.forest_viridian` | `ambience/kanto/forest_viridian.ogg` |
| `ambience.kanto.route_cerulean_transition` | `ambience/kanto/route_cerulean_transition.ogg` |
| `ambience.kanto.cave_mt_moon` | `ambience/kanto/cave_mt_moon.ogg` |
| `ambience.kanto.coastal_to_bill` | `ambience/kanto/coastal_to_bill.ogg` |
| `ambience.kanto.ocean_the_sea` | `ambience/kanto/ocean_the_sea.ogg` |
| `ambience.kanto.volcanic_cinnabar` | `ambience/kanto/volcanic_cinnabar.ogg` |
| `ambience.kanto.deep_dark_lavender_town` | `ambience/kanto/deep_dark_lavender_town.ogg` |
| `ambience.kanto.deep_dark_pokemon_mansion` | `ambience/kanto/deep_dark_pokemon_mansion.ogg` |
| `ambience.kanto.pokemon_tower` | `ambience/kanto/pokemon_tower.ogg` |
| `ambience.kanto.nether_rocket_hideout` | `ambience/kanto/nether_rocket_hideout.ogg` *(reserved — not currently wired to a biome)* |

### Johto (`ambience/johto/`)

| Key | File path |
|---|---|
| `ambience.johto.plains_route_29` | `ambience/johto/plains_route_29.ogg` |
| `ambience.johto.plains_route_30` | `ambience/johto/plains_route_30.ogg` |
| `ambience.johto.plains_new_bark_town` | `ambience/johto/plains_new_bark_town.ogg` |
| `ambience.johto.route_26_open` | `ambience/johto/route_26_open.ogg` |
| `ambience.johto.forest_national_park` | `ambience/johto/forest_national_park.ogg` |
| `ambience.johto.forest_ecruteak` | `ambience/johto/forest_ecruteak.ogg` |
| `ambience.johto.cave_union_cave` | `ambience/johto/cave_union_cave.ogg` |
| `ambience.johto.cave_ruins_of_alph` | `ambience/johto/cave_ruins_of_alph.ogg` |
| `ambience.johto.cave_ice_path` | `ambience/johto/cave_ice_path.ogg` |
| `ambience.johto.cave_dragons_den` | `ambience/johto/cave_dragons_den.ogg` |
| `ambience.johto.cave_deep_dark_pokegear_unown` | `ambience/johto/cave_deep_dark_pokegear_unown.ogg` |
| `ambience.johto.route_38_coastal` | `ambience/johto/route_38_coastal.ogg` |
| `ambience.johto.route_42_mountain` | `ambience/johto/route_42_mountain.ogg` |
| `ambience.johto.route_47_harsh_mountain` | `ambience/johto/route_47_harsh_mountain.ogg` |
| `ambience.johto.ocean_surf` | `ambience/johto/ocean_surf.ogg` |
| `ambience.johto.end_sinjoh_ruins` | `ambience/johto/end_sinjoh_ruins.ogg` |
| `ambience.johto.stronghold_ruins_of_alph` | `ambience/johto/stronghold_ruins_of_alph.ogg` *(reserved)* |

### Hoenn (`ambience/hoenn/`)

| Key | File path |
|---|---|
| `ambience.hoenn.plains_route_101` | `ambience/hoenn/plains_route_101.ogg` |
| `ambience.hoenn.plains_route_104` | `ambience/hoenn/plains_route_104.ogg` |
| `ambience.hoenn.plains_route_110` | `ambience/hoenn/plains_route_110.ogg` |
| `ambience.hoenn.plains_route_120` | `ambience/hoenn/plains_route_120.ogg` |
| `ambience.hoenn.plains_littleroot_town` | `ambience/hoenn/plains_littleroot_town.ogg` |
| `ambience.hoenn.plains_oldale_town` | `ambience/hoenn/plains_oldale_town.ogg` |
| `ambience.hoenn.forest_petalburg_woods` | `ambience/hoenn/forest_petalburg_woods.ogg` |
| `ambience.hoenn.forest_safari_zone` | `ambience/hoenn/forest_safari_zone.ogg` |
| `ambience.hoenn.jungle_route_119` | `ambience/hoenn/jungle_route_119.ogg` |
| `ambience.hoenn.heavy_rain` | `ambience/hoenn/heavy_rain.ogg` *(shared between jungle and ocean biome keys)* |
| `ambience.hoenn.desert_route_111` | `ambience/hoenn/desert_route_111.ogg` |
| `ambience.hoenn.desert_drought` | `ambience/hoenn/desert_drought.ogg` |
| `ambience.hoenn.volcanic_mt_chimney` | `ambience/hoenn/volcanic_mt_chimney.ogg` |
| `ambience.hoenn.volcanic_route_113` | `ambience/hoenn/volcanic_route_113.ogg` |
| `ambience.hoenn.mt_pyre` | `ambience/hoenn/mt_pyre.ogg` *(shared between deep_dark and volcanic biome keys)* |
| `ambience.hoenn.mountain_mt_pyre_exterior` | `ambience/hoenn/mountain_mt_pyre_exterior.ogg` |
| `ambience.hoenn.ocean_crossing_the_sea` | `ambience/hoenn/ocean_crossing_the_sea.ogg` |
| `ambience.hoenn.ocean_surf` | `ambience/hoenn/ocean_surf.ogg` |
| `ambience.hoenn.ocean_oceanic_museum` | `ambience/hoenn/ocean_oceanic_museum.ogg` |
| `ambience.hoenn.deep_ocean_dive` | `ambience/hoenn/deep_ocean_dive.ogg` |
| `ambience.hoenn.cave_of_origin` | `ambience/hoenn/cave_of_origin.ogg` |
| `ambience.hoenn.nether_hideout` | `ambience/hoenn/nether_hideout.ogg` *(reserved)* |

### Sinnoh (`ambience/sinnoh/`)

| Key | File path |
|---|---|
| `ambience.sinnoh.plains_route_201` | `ambience/sinnoh/plains_route_201.ogg` |
| `ambience.sinnoh.plains_route_203` | `ambience/sinnoh/plains_route_203.ogg` |
| `ambience.sinnoh.plains_route_205` | `ambience/sinnoh/plains_route_205.ogg` |
| `ambience.sinnoh.plains_route_209` | `ambience/sinnoh/plains_route_209.ogg` |
| `ambience.sinnoh.plains_route_210` | `ambience/sinnoh/plains_route_210.ogg` |
| `ambience.sinnoh.plains_twinleaf_town` | `ambience/sinnoh/plains_twinleaf_town.ogg` |
| `ambience.sinnoh.plains_sandgem_town` | `ambience/sinnoh/plains_sandgem_town.ogg` |
| `ambience.sinnoh.forest_eterna_forest` | `ambience/sinnoh/forest_eterna_forest.ogg` |
| `ambience.sinnoh.forest_floaroma_town` | `ambience/sinnoh/forest_floaroma_town.ogg` |
| `ambience.sinnoh.snowy_route_216` | `ambience/sinnoh/snowy_route_216.ogg` |
| `ambience.sinnoh.snowy_snowpoint_city` | `ambience/sinnoh/snowy_snowpoint_city.ogg` |
| `ambience.sinnoh.mountain_mt_coronet` | `ambience/sinnoh/mountain_mt_coronet.ogg` |
| `ambience.sinnoh.cave_lake_caverns` | `ambience/sinnoh/cave_lake_caverns.ogg` |
| `ambience.sinnoh.cave_oreburgh_mine` | `ambience/sinnoh/cave_oreburgh_mine.ogg` |
| `ambience.sinnoh.ocean_lake` | `ambience/sinnoh/ocean_lake.ogg` |
| `ambience.sinnoh.end_distortion_world` | `ambience/sinnoh/end_distortion_world.ogg` |

### Unova (`ambience/unova/`)

| Key | File path |
|---|---|
| `ambience.unova.plains_route_19_summer` | `ambience/unova/plains_route_19_summer.ogg` |
| `ambience.unova.plains_aspertia_city` | `ambience/unova/plains_aspertia_city.ogg` |
| `ambience.unova.cave_underground_ruins` | `ambience/unova/cave_underground_ruins.ogg` |
| `ambience.unova.snowy_frozen_city` | `ambience/unova/snowy_frozen_city.ogg` |
| `ambience.unova.stronghold_underground_ruins` | `ambience/unova/stronghold_underground_ruins.ogg` *(reserved)* |

---

## 3. Structure & Proximity Ambience

### Gyms — `ambience/gym/` (one per region, flat file — no subfolder)

| Key | File path | Trigger |
|---|---|---|
| `ambience.gym.kanto_gym` | `ambience/gym/kanto_gym.ogg` | Inside a Kanto gym structure |
| `ambience.gym.johto_gym` | `ambience/gym/johto_gym.ogg` | Inside a Johto gym structure |
| `ambience.gym.hoenn_gym` | `ambience/gym/hoenn_gym.ogg` | Inside a Hoenn gym structure |
| `ambience.gym.sinnoh_gym` | `ambience/gym/sinnoh_gym.ogg` | Inside a Sinnoh gym structure |
| `ambience.gym.unova_gym` | `ambience/gym/unova_gym.ogg` | Inside a Unova gym structure |

### Poké Centers — `ambience/pokecenter/` (flat pool, random pick)

| Key | File path |
|---|---|
| `ambience.pokecenter.kanto_center` | `ambience/pokecenter/kanto_center.ogg` |
| `ambience.pokecenter.johto_center` | `ambience/pokecenter/johto_center.ogg` |
| `ambience.pokecenter.hoenn_center` | `ambience/pokecenter/hoenn_center.ogg` |
| `ambience.pokecenter.sinnoh_center` | `ambience/pokecenter/sinnoh_center.ogg` |
| `ambience.pokecenter.unova_center` | `ambience/pokecenter/unova_center.ogg` |

Triggered by a `MusicTriggerBlock` with `ZoneId:"cobbletunes:pokecenter"` — place one inside a Poké Center schematic.

### Poké Marts — `ambience/pokemart/` (flat pool, random pick)

| Key | File path |
|---|---|
| `ambience.pokemart.mart1` | `ambience/pokemart/mart1.ogg` |
| `ambience.pokemart.mart2` | `ambience/pokemart/mart2.ogg` |
| `ambience.pokemart.mart3` | `ambience/pokemart/mart3.ogg` |

Triggered by a `MusicTriggerBlock` with `ZoneId:"cobbletunes:pokemart"`.

### Special Structures — `ambience/structure/` (1:1, no pooling — one track per exact location)

| Key | File path | Structure |
|---|---|---|
| `ambience.structure.ash` | `ambience/structure/ash.ogg` | Ash's house |
| `ambience.structure.crown_cemetery` | `ambience/structure/crown_cemetery.ogg` | Crown Cemetery |
| `ambience.structure.articuno` | `ambience/structure/articuno.ogg` | Articuno's cave |
| `ambience.structure.zapdos` | `ambience/structure/zapdos.ogg` | Zapdos's location |
| `ambience.structure.moltres` | `ambience/structure/moltres.ogg` | Moltres's location |
| `ambience.structure.mew` | `ambience/structure/mew.ogg` | Mew's shrine |
| `ambience.structure.bell_tower` | `ambience/structure/bell_tower.ogg` | Bell Tower (Ho-Oh) |
| `ambience.structure.burned_tower` | `ambience/structure/burned_tower.ogg` | Burned Tower |
| `ambience.structure.celebi_shrine` | `ambience/structure/celebi_shrine.ogg` | Celebi Shrine |
| `ambience.structure.whirl_island` | `ambience/structure/whirl_island.ogg` | Whirl Island |
| `ambience.structure.groudon` | `ambience/structure/groudon.ogg` | Groudon's cave |
| `ambience.structure.kyogre` | `ambience/structure/kyogre.ogg` | Kyogre's cave |
| `ambience.structure.regirock` | `ambience/structure/regirock.ogg` | Regirock's ruins |
| `ambience.structure.regice` | `ambience/structure/regice.ogg` | Regice's cave |
| `ambience.structure.registeel` | `ambience/structure/registeel.ogg` | Registeel's tomb |
| `ambience.structure.deoxys` | `ambience/structure/deoxys.ogg` | Deoxys's location |
| `ambience.structure.jirachi` | `ambience/structure/jirachi.ogg` | Jirachi's location |
| `ambience.structure.secret_garden` | `ambience/structure/secret_garden.ogg` | Secret Garden |
| `ambience.structure.sky_pillar` | `ambience/structure/sky_pillar.ogg` | Sky Pillar |
| `ambience.structure.dyna_tree` | `ambience/structure/dyna_tree.ogg` | Dyna Tree |
| `ambience.structure.spear_pillar` | `ambience/structure/spear_pillar.ogg` | Spear Pillar |
| `ambience.structure.snowpoint_temple` | `ambience/structure/snowpoint_temple.ogg` | Snowpoint Temple (Regigigas) |
| `ambience.structure.split_decision_temple` | `ambience/structure/split_decision_temple.ogg` | Lake trio temple |
| `ambience.structure.route210` | `ambience/structure/route210.ogg` | Flower Paradise (filename kept from source track) |
| `ambience.structure.fullmoon_island` | `ambience/structure/fullmoon_island.ogg` | Fullmoon Island |
| `ambience.structure.crescent_isle` | `ambience/structure/crescent_isle.ogg` | Crescent Isle (Cresselia) |
| `ambience.structure.eterna_building` | `ambience/structure/eterna_building.ogg` | Eterna Building / Old Chateau |
| `ambience.structure.wild_plant` | `ambience/structure/wild_plant.ogg` | Wind Plant (filename typo preserved from source) |
| `ambience.structure.manaphy` | `ambience/structure/manaphy.ogg` | Manaphy's shrine |

### Vanilla & BCA Structures — `ambience/vanilla/<category>/` (pooled, random pick per category)

⚠️ **Unlike the description in earlier drafts of this doc, filenames here are NOT arbitrary.** Each file is registered against an exact `SoundEvent` key — the file must match the name below exactly (case-sensitive, no extension in `sounds.json`, `.ogg` on disk) or that specific pool entry stays silent.

| Category folder | Key | File |
|---|---|---|
| `ambience/vanilla/ancient_city/` | `ambience.vanilla.ancient_city.cerulean_cave` | `cerulean_cave.ogg` |
| | `ambience.vanilla.ancient_city.distortion_world` | `distortion_world.ogg` |
| | `ambience.vanilla.ancient_city.dragonspiral_tower` | `dragonspiral_tower.ogg` |
| | `ambience.vanilla.ancient_city.mt_silver` | `mt_silver.ogg` |
| | `ambience.vanilla.ancient_city.pokemon_mansion` | `pokemon_mansion.ogg` |
| `ambience/vanilla/bastion_remnant/` | `ambience.vanilla.bastion_remnant.hideout` | `hideout.ogg` |
| | `ambience.vanilla.bastion_remnant.reversal_mountain` | `reversal_mountain.ogg` |
| `ambience/vanilla/bca_village_large/` | `ambience.vanilla.bca_village_large.celadon_city` | `celadon_city.ogg` |
| | `ambience.vanilla.bca_village_large.cerulean_city` | `cerulean_city.ogg` |
| | `ambience.vanilla.bca_village_large.cinnabar` | `cinnabar.ogg` |
| | `ambience.vanilla.bca_village_large.fallarbor` | `fallarbor.ogg` |
| | `ambience.vanilla.bca_village_large.rustboro` | `rustboro.ogg` |
| | `ambience.vanilla.bca_village_large.verdanturf` | `verdanturf.ogg` |
| | `ambience.vanilla.bca_village_large.violet` | `violet.ogg` |
| `ambience/vanilla/bca_village_mid/` | `ambience.vanilla.bca_village_mid.celadon_city` | `celadon_city.ogg` |
| | `ambience.vanilla.bca_village_mid.cerulean_city` | `cerulean_city.ogg` |
| | `ambience.vanilla.bca_village_mid.cinnabar` | `cinnabar.ogg` |
| | `ambience.vanilla.bca_village_mid.fallarbor` | `fallarbor.ogg` |
| | `ambience.vanilla.bca_village_mid.rustboro` | `rustboro.ogg` |
| | `ambience.vanilla.bca_village_mid.verdanturf` | `verdanturf.ogg` |
| | `ambience.vanilla.bca_village_mid.violet` | `violet.ogg` |
| `ambience/vanilla/bca_village_small/` | `ambience.vanilla.bca_village_small.celadon_city` | `celadon_city.ogg` |
| | `ambience.vanilla.bca_village_small.cerulean_city` | `cerulean_city.ogg` |
| | `ambience.vanilla.bca_village_small.cinnabar` | `cinnabar.ogg` |
| | `ambience.vanilla.bca_village_small.fallarbor` | `fallarbor.ogg` |
| | `ambience.vanilla.bca_village_small.rustboro` | `rustboro.ogg` |
| | `ambience.vanilla.bca_village_small.verdanturf` | `verdanturf.ogg` |
| | `ambience.vanilla.bca_village_small.violet` | `violet.ogg` |
| `ambience/vanilla/buried_treasure/` | `ambience.vanilla.buried_treasure.undella_town` | `undella_town.ogg` |
| | `ambience.vanilla.buried_treasure.lacunosa_town` | `lacunosa_town.ogg` |
| | `ambience.vanilla.buried_treasure.route109` | `route109.ogg` |
| `ambience/vanilla/desert_pyramid/` | `ambience.vanilla.desert_pyramid.relic_castle` | `relic_castle.ogg` |
| | `ambience.vanilla.desert_pyramid.desert_ruins` | `desert_ruins.ogg` |
| | `ambience.vanilla.desert_pyramid.mirage_tower` | `mirage_tower.ogg` |
| `ambience/vanilla/end_city/` | `ambience.vanilla.end_city.hall_of_origin` | `hall_of_origin.ogg` |
| | `ambience.vanilla.end_city.mt_chimney` | `mt_chimney.ogg` |
| | `ambience.vanilla.end_city.n_castle` | `n_castle.ogg` |
| | `ambience.vanilla.end_city.spear_pillar` | `spear_pillar.ogg` |
| `ambience/vanilla/fortress/` | `ambience.vanilla.fortress.rocket_hq` | `rocket_hq.ogg` |
| | `ambience.vanilla.fortress.stark_mountain` | `stark_mountain.ogg` |
| | `ambience.vanilla.fortress.hideout` | `hideout.ogg` |
| | `ambience.vanilla.fortress.mt_ember` | `mt_ember.ogg` |
| `ambience/vanilla/igloo/` | `ambience.vanilla.igloo.shoal_cave` | `shoal_cave.ogg` |
| | `ambience.vanilla.igloo.cold_storage` | `cold_storage.ogg` |
| | `ambience.vanilla.igloo.ice_path` | `ice_path.ogg` |
| `ambience/vanilla/jungle_pyramid/` | `ambience.vanilla.jungle_pyramid.ilex_forest` | `ilex_forest.ogg` |
| | `ambience.vanilla.jungle_pyramid.island_cave` | `island_cave.ogg` |
| | `ambience.vanilla.jungle_pyramid.pinwheel_forest` | `pinwheel_forest.ogg` |
| | `ambience.vanilla.jungle_pyramid.safari_zone` | `safari_zone.ogg` |
| `ambience/vanilla/mansion/` | `ambience.vanilla.mansion.poke_mansion` | `poke_mansion.ogg` |
| | `ambience.vanilla.mansion.strange_house` | `strange_house.ogg` |
| | `ambience.vanilla.mansion.old_chateau` | `old_chateau.ogg` |
| `ambience/vanilla/mineshaft/` | `ambience.vanilla.mineshaft.mt_moon` | `mt_moon.ogg` |
| | `ambience.vanilla.mineshaft.oreburgh_mine` | `oreburgh_mine.ogg` |
| | `ambience.vanilla.mineshaft.union_cave` | `union_cave.ogg` |
| `ambience/vanilla/mineshaft_mesa/` | `ambience.vanilla.mineshaft_mesa.chargestone_cave` | `chargestone_cave.ogg` |
| | `ambience.vanilla.mineshaft_mesa.reversal_mountain` | `reversal_mountain.ogg` |
| | `ambience.vanilla.mineshaft_mesa.stark_mountain` | `stark_mountain.ogg` |
| `ambience/vanilla/monument/` | `ambience.vanilla.monument.cave_origin` | `cave_origin.ogg` |
| | `ambience.vanilla.monument.whirl_islands` | `whirl_islands.ogg` |
| `ambience/vanilla/nether_fossil/` | `ambience.vanilla.nether_fossil.lavender_town` | `lavender_town.ogg` |
| | `ambience.vanilla.nether_fossil.mt_pyre` | `mt_pyre.ogg` |
| | `ambience.vanilla.nether_fossil.poke_tower` | `poke_tower.ogg` |
| | `ambience.vanilla.nether_fossil.strange_house` | `strange_house.ogg` |
| `ambience/vanilla/ocean_ruin/` *(covers `ocean_ruin_cold` + `ocean_ruin_warm`)* | `ambience.vanilla.ocean_ruin.dive_theme` | `dive_theme.ogg` |
| | `ambience.vanilla.ocean_ruin.marine_tube` | `marine_tube.ogg` |
| | `ambience.vanilla.ocean_ruin.underwater` | `underwater.ogg` |
| `ambience/vanilla/pillager_outpost/` | `ambience.vanilla.pillager_outpost.radio_tower_takeover` | `radio_tower_takeover.ogg` |
| | `ambience.vanilla.pillager_outpost.rocket_hideout` | `rocket_hideout.ogg` |
| | `ambience.vanilla.pillager_outpost.team_aqua_magma_hideout` | `team_aqua_magma_hideout.ogg` |
| `ambience/vanilla/ruined_portal/` *(covers all 7 biome variants)* | `ambience.vanilla.ruined_portal.distorn_world` | `distorn_world.ogg` *(filename typo preserved from source)* |
| | `ambience.vanilla.ruined_portal.radio_signal` | `radio_signal.ogg` |
| `ambience/vanilla/shipwreck/` *(covers `shipwreck` + `shipwreck_beached`)* | `ambience.vanilla.shipwreck.plasma_frigate` | `plasma_frigate.ogg` |
| | `ambience.vanilla.shipwreck.ss_aqua` | `ss_aqua.ogg` |
| | `ambience.vanilla.shipwreck.abandoned_ship` | `abandoned_ship.ogg` |
| `ambience/vanilla/stronghold/` | `ambience.vanilla.stronghold.spear_pillar` | `spear_pillar.ogg` |
| | `ambience.vanilla.stronghold.dragonpsiral` | `dragonpsiral.ogg` *(filename typo preserved from source)* |
| | `ambience.vanilla.stronghold.mt_coronet` | `mt_coronet.ogg` |
| `ambience/vanilla/swamp_hut/` | `ambience.vanilla.swamp_hut.losrtlorn_forest` | `losrtlorn_forest.ogg` *(filename typo preserved from source)* |
| | `ambience.vanilla.swamp_hut.ecruteak_city` | `ecruteak_city.ogg` |
| | `ambience.vanilla.swamp_hut.great_marsh` | `great_marsh.ogg` |
| `ambience/vanilla/trail_ruins/` | `ambience.vanilla.trail_ruins.sevault_canyon` | `sevault_canyon.ogg` |
| | `ambience.vanilla.trail_ruins.solaceon_ruins` | `solaceon_ruins.ogg` |
| | `ambience.vanilla.trail_ruins.abyssal_ruins` | `abyssal_ruins.ogg` |
| | `ambience.vanilla.trail_ruins.ruins_of_alph` | `ruins_of_alph.ogg` |
| `ambience/vanilla/trial_chambers/` | `ambience.vanilla.trial_chambers.teamplasma_hq` | `teamplasma_hq.ogg` |
| | `ambience.vanilla.trial_chambers.victory_road` | `victory_road.ogg` |
| | `ambience.vanilla.trial_chambers.silph_co` | `silph_co.ogg` |
| | `ambience.vanilla.trial_chambers.team_galactic` | `team_galactic.ogg` |
| `ambience/vanilla/village_desert/` | `ambience.vanilla.village_desert.rustboro_city` | `rustboro_city.ogg` |
| | `ambience.vanilla.village_desert.fallarbor_town` | `fallarbor_town.ogg` |
| | `ambience.vanilla.village_desert.lentimas_town` | `lentimas_town.ogg` |
| | `ambience.vanilla.village_desert.route_111` | `route_111.ogg` |
| `ambience/vanilla/village_plains/` | `ambience.vanilla.village_plains.pewter_city` | `pewter_city.ogg` |
| | `ambience.vanilla.village_plains.rustboro_city` | `rustboro_city.ogg` |
| | `ambience.vanilla.village_plains.twinleaf_town` | `twinleaf_town.ogg` |
| | `ambience.vanilla.village_plains.cherrygrove_city` | `cherrygrove_city.ogg` |
| | `ambience.vanilla.village_plains.littleroot_town` | `littleroot_town.ogg` |
| | `ambience.vanilla.village_plains.newbark_town` | `newbark_town.ogg` |
| | `ambience.vanilla.village_plains.nuvema_town` | `nuvema_town.ogg` |
| | `ambience.vanilla.village_plains.pallet_town` | `pallet_town.ogg` |
| `ambience/vanilla/village_savanna/` | `ambience.vanilla.village_savanna.verdantuf_town` | `verdantuf_town.ogg` *(filename typo preserved from source)* |
| | `ambience.vanilla.village_savanna.azalea_town` | `azalea_town.ogg` |
| | `ambience.vanilla.village_savanna.floccesy_town` | `floccesy_town.ogg` |
| | `ambience.vanilla.village_savanna.goldenrod_town` | `goldenrod_town.ogg` |
| `ambience/vanilla/village_snowy/` | `ambience.vanilla.village_snowy.snowpoint_city` | `snowpoint_city.ogg` |
| | `ambience.vanilla.village_snowy.icirrus_city` | `icirrus_city.ogg` |
| | `ambience.vanilla.village_snowy.mahogany_town` | `mahogany_town.ogg` |
| `ambience/vanilla/village_taiga/` | `ambience.vanilla.village_taiga.fortree_city` | `fortree_city.ogg` |
| | `ambience.vanilla.village_taiga.canalave_city` | `canalave_city.ogg` |
| | `ambience.vanilla.village_taiga.celestic_town` | `celestic_town.ogg` |
| | `ambience.vanilla.village_taiga.eterna_city` | `eterna_city.ogg` |

**BCA note:** `bca_village_small/mid/large` correspond to CobblemonAdditions' village size tiers (`bca:village/small`, `bca:village/mid`, `bca:village/large`). Only relevant if CobblemonAdditions is installed.

---

## 4. Menu Music — `menu/` (flat pool, random pick)

| Key | File path |
|---|---|
| `menu.emerald` | `menu/emerald.ogg` |
| `menu.frlg` | `menu/frlg.ogg` |
| `menu.hgss` | `menu/hgss.ogg` |

Plays continuously across the title screen and every submenu (Singleplayer list, Options, Mods, etc.) as one session — stops the instant a world loads, resumes (with a fresh random pick) when returning to the title screen.

---

## 5. Effects — `effect/`

| Key | File path | Trigger |
|---|---|---|
| `effect.lowhp` | `effect/lowhp.ogg` | Player's active battle Pokémon drops to ≤25% HP. Played once (not looped) per trigger — if your file already contains multiple "beeps," that's fine, it just plays through once. |

---

## Notes for resource pack builders

- **Missing files are safe.** Any path without a matching `.ogg` simply produces silence for that specific context — no crash, no error spam beyond a one-time "file does not exist" warning in the log at startup.
- **You don't need every file.** A partial pack (e.g. just Kanto + Johto) works fine — unmapped regions/contexts will just have nothing play until you add more.
- **`sounds.json` format** — every entry follows:
  ```json
  "cobbletunes.<key>": {
    "category": "music",
    "sounds": [{ "name": "cobbletunes:<file path without .ogg>", "stream": true }]
  }
  ```
  Use `"stream": true` for all of these — they're long-form tracks, not short effects (the one exception is `effect.lowhp`, which can omit `stream` since it's a brief cue).
- **Testing a single track** without joining a battle or walking to a biome: `/playsound cobbletunes:<key> music @s` works for any key in this document.
