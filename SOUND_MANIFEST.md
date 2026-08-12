# CobbleTunes — Sound Manifest

This manifest matches the current final source and `sounds.json`. Every entry below is wired by `TrackRegistry` or the low-HP effect path; there are no reserved-only sound keys in this build.

- **Total sound events:** 404
- **Battle:** 117
- **Victory:** 31
- **Battle Tower:** 25
- **Ambience and structures:** 227
- **Menu:** 3
- **Effects:** 1
- **Base folder:** `assets/cobbletunes/sounds/`
- **Music files:** streamed through `sounds.json`

The current source audit resolves all 404 `sounds.json` keys from code. A resource pack still needs to provide the matching `.ogg` files.

## Routing notes

- Battle routing keeps role and region separate, with RCT metadata preferred and opposing-roster voting as fallback.
- Alolan, Galarian, Hisuian, and Paldean forms override the base National Dex region for wild, trainer-roster, Boss, and Victory routing.
- Legendary routing includes species and form overrides for Kyurem, Necrozma, Eternatus, Calyrex, Terapagos, Galarian birds, and other dedicated encounters.
- Victory music starts immediately on `BATTLE_VICTORY` when Cobblemon Loot Menu is installed, stays active while the loot screen is open, then restores the latest structure or biome target.
- Hisui intentionally has no post-battle Victory file in this pack and falls back without creating a fake theme.
- Battle Tower ambience uses low, mid, high, and final floor pools. Trainer battles started inside a Battle Tower zone use the Galar Battle Tower battle theme.

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
| `battle.galar.battle_tower` | `battle/galar/battle_tower.ogg` | trainer battle inside battle tower zone |
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

Victory files are used only by the optional Cobblemon Loot Menu bridge. The theme begins at the battle victory event rather than waiting for the GUI to appear.

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

## Ambience and structure music

Regional biome tracks rotate with silence windows. Zone tracks such as Gyms, Poké Centers, Poké Marts, Battle Tower floors, exact Cobbleverse structures, and vanilla/BCA structure pools loop while their zone owns the audio slot.

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

### Exact Cobbleverse structures

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

## Menu music

| Key | File | Usage |
|---|---|---|
| `menu.hgss` | `menu/hgss.ogg` | title screen and submenu pool |
| `menu.frlg` | `menu/frlg.ogg` | title screen and submenu pool |
| `menu.emerald` | `menu/emerald.ogg` | title screen and submenu pool |

## Effects

| Key | File | Usage |
|---|---|---|
| `effect.lowhp` | `effect/lowhp.ogg` | low hp battle cue |

## Resource pack notes

- Keep every path lowercase and exact.
- Keep music entries streamed.
- `effect.lowhp` is played as a short sound effect and is boosted above the configured music volume in code.
- Missing files stay silent instead of crashing CobbleTunes.
- `/playsound cobbletunes:<key> music @s` can be used to test music keys directly.
