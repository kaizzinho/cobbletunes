package com.kaizzinho.cobbletunes.event

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader

object StructureZoneDetector {

    private const val ZONE_CHECK_INTERVAL_TICKS = 10
    private const val CHUNK_SCAN_RADIUS = 3
    private const val SMALL_STRUCTURE_MAX_SPAN = 16
    private const val SMALL_STRUCTURE_PADDING = 4
    private const val SMALL_STRUCTURE_VERTICAL_PADDING = 4
    private const val STRUCTURE_PADDING = 1
    private const val STRUCTURE_VERTICAL_PADDING = 2
    private const val DEFAULT_MANUAL_ZONE_RADIUS = 12
    private const val MIN_MANUAL_ZONE_RADIUS = 1
    private const val MAX_MANUAL_ZONE_RADIUS = 32
    private const val MANUAL_ZONE_TAG_PREFIX = "cobbletunes_zone:"
    private const val MANUAL_ZONE_RANGE_TAG_PREFIX = "cobbletunes_range:"
    private const val PRIORITY_VILLAGE = 100
    private const val PRIORITY_GENERIC_STRUCTURE = 200
    private const val PRIORITY_SPECIAL_STRUCTURE = 300

    private val GYM_STRUCTURES: Map<Identifier, String> = mapOf(
        Identifier.of("cobbleverse", "brock")             to "cobbleverse:brock",
        Identifier.of("cobbleverse", "misty")             to "cobbleverse:misty",
        Identifier.of("cobbleverse", "ltsurge")           to "cobbleverse:ltsurge",
        Identifier.of("cobbleverse", "erika")             to "cobbleverse:erika",
        Identifier.of("cobbleverse", "koga")              to "cobbleverse:koga",
        Identifier.of("cobbleverse", "sabrina")           to "cobbleverse:sabrina",
        Identifier.of("cobbleverse", "blaine")            to "cobbleverse:blaine",
        Identifier.of("cobbleverse", "giovanni")          to "cobbleverse:giovanni",
        Identifier.of("cobbleverse", "kanto_league")      to "cobbleverse:kanto_league",
        Identifier.of("cobbleverse", "team_rocket_tower") to "cobbleverse:team_rocket_tower",
        Identifier.of("cobbleverse", "crown_spire")       to "cobbleverse:crown_spire",
        Identifier.of("cobbleverse", "dawn_tower")        to "cobbleverse:dawn_tower",
        Identifier.of("cobbleverse", "dusk_tower")        to "cobbleverse:dusk_tower",
        Identifier.of("cobbleverse", "valerio")           to "cobbleverse:valerio",
        Identifier.of("cobbleverse", "chiara")            to "cobbleverse:chiara",
        Identifier.of("cobbleverse", "angelo")            to "cobbleverse:angelo",
        Identifier.of("cobbleverse", "alfredo")           to "cobbleverse:alfredo",
        Identifier.of("cobbleverse", "furio")             to "cobbleverse:furio",
        Identifier.of("cobbleverse", "jasmine")           to "cobbleverse:jasmine",
        Identifier.of("cobbleverse", "raffaello")         to "cobbleverse:raffaello",
        Identifier.of("cobbleverse", "sandra")            to "cobbleverse:sandra",
        Identifier.of("cobbleverse", "johto_league")      to "cobbleverse:johto_league",
        Identifier.of("cobbleverse", "rocket_radio_tower") to "cobbleverse:rocket_radio_tower",
        Identifier.of("cobbleverse", "rudi")              to "cobbleverse:rudi",
        Identifier.of("cobbleverse", "adriano")           to "cobbleverse:adriano",
        Identifier.of("cobbleverse", "tell_pat")          to "cobbleverse:tell_pat",
        Identifier.of("cobbleverse", "alice")             to "cobbleverse:alice",
        Identifier.of("cobbleverse", "norman")            to "cobbleverse:norman",
        Identifier.of("cobbleverse", "fiammetta")         to "cobbleverse:fiammetta",
        Identifier.of("cobbleverse", "walter")            to "cobbleverse:walter",
        Identifier.of("cobbleverse", "petra")             to "cobbleverse:petra",
        Identifier.of("cobbleverse", "hoenn_league")      to "cobbleverse:hoenn_league",
        Identifier.of("cobbleverse", "gardenia")          to "cobbleverse:gardenia",
        Identifier.of("cobbleverse", "ferruccio")         to "cobbleverse:ferruccio",
        Identifier.of("cobbleverse", "marzia")            to "cobbleverse:marzia",
        Identifier.of("cobbleverse", "fannie")            to "cobbleverse:fannie",
        Identifier.of("cobbleverse", "corrado")           to "cobbleverse:corrado",
        Identifier.of("cobbleverse", "bianca")            to "cobbleverse:bianca",
        Identifier.of("cobbleverse", "omar")              to "cobbleverse:omar",
        Identifier.of("cobbleverse", "pedro")             to "cobbleverse:pedro",
        Identifier.of("cobbleverse", "sinnoh_league")     to "cobbleverse:sinnoh_league",
        Identifier.of("cobbleverse", "team_galactic_hq")  to "cobbleverse:team_galactic_hq",

        Identifier.of("cobbleverse", "ash")            to "cobbleverse:ash",
        Identifier.of("cobbleverse", "crown_cemetery") to "cobbleverse:crown_cemetery",
        Identifier.of("cobbleverse", "legendary/articuno") to "cobbleverse:articuno",
        Identifier.of("cobbleverse", "articuno")           to "cobbleverse:articuno",
        Identifier.of("cobbleverse", "legendary/zapdos")   to "cobbleverse:zapdos",
        Identifier.of("cobbleverse", "zapdos")             to "cobbleverse:zapdos",
        Identifier.of("cobbleverse", "legendary/moltres")  to "cobbleverse:moltres",
        Identifier.of("cobbleverse", "moltres")            to "cobbleverse:moltres",
        Identifier.of("cobbleverse", "mythical/mew")       to "cobbleverse:mew",
        Identifier.of("cobbleverse", "mew")                to "cobbleverse:mew",

        Identifier.of("cobbleverse", "bell_tower")     to "cobbleverse:bell_tower",
        Identifier.of("cobbleverse", "burned_tower")   to "cobbleverse:burned_tower",
        Identifier.of("cobbleverse", "celebi_shrine")  to "cobbleverse:celebi_shrine",
        Identifier.of("cobbleverse", "whirl_island")   to "cobbleverse:whirl_island",

        Identifier.of("cobbleverse", "legendary/groudon")  to "cobbleverse:groudon",
        Identifier.of("cobbleverse", "groudon")            to "cobbleverse:groudon",
        Identifier.of("cobbleverse", "legendary/kyogre")   to "cobbleverse:kyogre",
        Identifier.of("cobbleverse", "kyogre")             to "cobbleverse:kyogre",
        Identifier.of("cobbleverse", "legendary/regirock") to "cobbleverse:regirock",
        Identifier.of("cobbleverse", "regirock")           to "cobbleverse:regirock",
        Identifier.of("cobbleverse", "legendary/regice")   to "cobbleverse:regice",
        Identifier.of("cobbleverse", "regice")             to "cobbleverse:regice",
        Identifier.of("cobbleverse", "legendary/registeel") to "cobbleverse:registeel",
        Identifier.of("cobbleverse", "registeel")           to "cobbleverse:registeel",
        Identifier.of("cobbleverse", "mythical/deoxys")   to "cobbleverse:deoxys",
        Identifier.of("cobbleverse", "deoxys")            to "cobbleverse:deoxys",
        Identifier.of("cobbleverse", "mythical/jirachi")  to "cobbleverse:jirachi",
        Identifier.of("cobbleverse", "jirachi")           to "cobbleverse:jirachi",
        Identifier.of("cobbleverse", "secret_garden")  to "cobbleverse:secret_garden",
        Identifier.of("cobbleverse", "sky_pillar")     to "cobbleverse:sky_pillar",
        Identifier.of("cobbleverse", "dyna_tree")      to "cobbleverse:dyna_tree",

        Identifier.of("cobbleverse", "spear_pillar")         to "cobbleverse:spear_pillar",
        Identifier.of("cobbleverse", "snowpoint_temple")     to "cobbleverse:snowpoint_temple",
        Identifier.of("cobbleverse", "split_decision_temple") to "cobbleverse:split_decision_temple",
        Identifier.of("cobbleverse", "flower_paradise")      to "cobbleverse:flower_paradise",
        Identifier.of("cobbleverse", "fullmoon_island")      to "cobbleverse:fullmoon_island",
        Identifier.of("cobbleverse", "crescent_isle")        to "cobbleverse:crescent_isle",
        Identifier.of("cobbleverse", "eterna_building")      to "cobbleverse:eterna_building",
        Identifier.of("cobbleverse", "wind_plant")           to "cobbleverse:wind_plant",
        Identifier.of("cobbleverse", "mythical/manaphy")     to "cobbleverse:manaphy",
        Identifier.of("cobbleverse", "manaphy")              to "cobbleverse:manaphy",
    )

    // gimmighoul towers keep the spooky pool
    private val COBBLEMON_STRUCTURES: Map<Identifier, String> = mapOf(
        Identifier.of("cobblemon", "ruins/deserted_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/frozen_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/lush_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/rooted_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/sunscorched_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/temperate_gimmi_tower") to "cobbletunes:gimmighoul_tower"
    )

    // exact 1.8 ids, no block guessing
    private val COBBLEMON_POOLED_STRUCTURES: Map<Identifier, String> = mapOf(
        // habitats
        Identifier.of("cobblemon", "habitats/badlands_shaded_rock") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/berry_patch") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/birch_wildfire_scar") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/bug_mound") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/carved_ice_spikes") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "habitats/deep_sea_spire") to "cobbletunes:vanilla_structure:ocean_ruin",
        Identifier.of("cobblemon", "habitats/desert_oasis") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "habitats/desert_shaded_rock") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "habitats/drifting_icebergs") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "habitats/fae_mounds") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/flowerbed_clearing") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/freshwater_pond") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/fungal_dwelling") to "cobbletunes:vanilla_structure:swamp_hut",
        Identifier.of("cobblemon", "habitats/lush_canopy") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/lush_cenote") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/lush_peat_bog") to "cobbletunes:vanilla_structure:swamp_hut",
        Identifier.of("cobblemon", "habitats/meteorite_impact") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/natural_lightningrod") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/oak_wildfire_scar") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/parched_peat_bog") to "cobbletunes:vanilla_structure:swamp_hut",
        Identifier.of("cobblemon", "habitats/pinkflowerbed_clearing") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/reclaimed_deserted_monument") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "habitats/reclaimed_lush_monument") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/sandpit_clearing") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "habitats/snowy_burrow") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "habitats/snowy_grotto") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "habitats/snowy_thermal_vents") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "habitats/spruce_wildfire_scar") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "habitats/sunflowerbed_clearing") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "habitats/sunscorched_clearing") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "habitats/thermal_crevices") to "cobbletunes:vanilla_structure:mineshaft_mesa",
        Identifier.of("cobblemon", "habitats/zen_garden") to "cobbletunes:vanilla_structure:jungle_pyramid",

        // ruins, minus gimmighoul towers
        Identifier.of("cobblemon", "ruins/ancient_dais_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/crumbling_arch_ruins") to "cobbletunes:vanilla_structure:stronghold",
        Identifier.of("cobblemon", "ruins/decaying_crypt_ruins") to "cobbletunes:vanilla_structure:nether_fossil",
        Identifier.of("cobblemon", "ruins/deserted_house_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/deserted_monument_ruins") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "ruins/deserted_tower_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/deserted_town_center_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/fallen_statue_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/frozen_altar_ruins") to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("cobblemon", "ruins/hidden_bunker_ruins") to "cobbletunes:vanilla_structure:stronghold",
        Identifier.of("cobblemon", "ruins/luna_henge_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/lush_monument_ruins") to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("cobblemon", "ruins/meteor_battleground_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/mossy_oubliette_ruins") to "cobbletunes:vanilla_structure:stronghold",
        Identifier.of("cobblemon", "ruins/old_garden_ruins") to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("cobblemon", "ruins/overgrown_trial_ruins") to "cobbletunes:vanilla_structure:trial_chambers",
        Identifier.of("cobblemon", "ruins/rooted_arch_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/sol_henge_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/stonjourner_henge_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/submerged_forge_ruins") to "cobbletunes:vanilla_structure:ocean_ruin",
        Identifier.of("cobblemon", "ruins/sunscorched_shaded_ruins") to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("cobblemon", "ruins/toppled_pillars_ruins") to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("cobblemon", "ruins/unstable_cave_ruins") to "cobbletunes:vanilla_structure:mineshaft",

        // coves and fishing boats
        Identifier.of("cobblemon", "shipwreck_coves/lush_shipwreck_cove") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("cobblemon", "shipwreck_coves/magma_shipwreck_cove") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("cobblemon", "shipwreck_coves/submerged_shipwreck_cove") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("cobblemon", "fishing_boat/beach") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("cobblemon", "fishing_boat/deep_ocean") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("cobblemon", "fishing_boat/warm_ocean") to "cobbletunes:vanilla_structure:shipwreck"
    )

    // vanilla and bca share pools
    private val VANILLA_AND_BCA_STRUCTURES: Map<Identifier, String> = mapOf(
        Identifier.of("minecraft", "village_plains")   to "cobbletunes:vanilla_structure:village_plains",
        Identifier.of("minecraft", "village_desert")   to "cobbletunes:vanilla_structure:village_desert",
        Identifier.of("minecraft", "village_savanna")  to "cobbletunes:vanilla_structure:village_savanna",
        Identifier.of("minecraft", "village_snowy")    to "cobbletunes:vanilla_structure:village_snowy",
        Identifier.of("minecraft", "village_taiga")    to "cobbletunes:vanilla_structure:village_taiga",
        Identifier.of("minecraft", "ancient_city")     to "cobbletunes:vanilla_structure:ancient_city",
        Identifier.of("minecraft", "trial_chambers")   to "cobbletunes:vanilla_structure:trial_chambers",
        Identifier.of("minecraft", "stronghold")       to "cobbletunes:vanilla_structure:stronghold",
        Identifier.of("minecraft", "mineshaft")        to "cobbletunes:vanilla_structure:mineshaft",
        Identifier.of("minecraft", "mineshaft_mesa")   to "cobbletunes:vanilla_structure:mineshaft_mesa",
        Identifier.of("minecraft", "trail_ruins")      to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("minecraft", "desert_pyramid")   to "cobbletunes:vanilla_structure:desert_pyramid",
        Identifier.of("minecraft", "jungle_pyramid")   to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("minecraft", "igloo")            to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("minecraft", "swamp_hut")        to "cobbletunes:vanilla_structure:swamp_hut",
        Identifier.of("minecraft", "pillager_outpost") to "cobbletunes:vanilla_structure:pillager_outpost",
        Identifier.of("minecraft", "monument")         to "cobbletunes:vanilla_structure:monument",
        Identifier.of("minecraft", "ocean_ruin_cold")  to "cobbletunes:vanilla_structure:ocean_ruin",
        Identifier.of("minecraft", "ocean_ruin_warm")  to "cobbletunes:vanilla_structure:ocean_ruin",
        Identifier.of("minecraft", "shipwreck")         to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("minecraft", "shipwreck_beached") to "cobbletunes:vanilla_structure:shipwreck",
        Identifier.of("minecraft", "buried_treasure")  to "cobbletunes:vanilla_structure:buried_treasure",
        Identifier.of("minecraft", "fortress")         to "cobbletunes:vanilla_structure:fortress",
        Identifier.of("minecraft", "bastion_remnant")  to "cobbletunes:vanilla_structure:bastion_remnant",
        Identifier.of("minecraft", "nether_fossil")    to "cobbletunes:vanilla_structure:nether_fossil",
        Identifier.of("minecraft", "mansion")          to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("minecraft", "ruined_portal")          to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_desert")   to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_jungle")   to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_swamp")    to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_mountain") to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_ocean")    to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "ruined_portal_nether")   to "cobbletunes:vanilla_structure:ruined_portal",
        Identifier.of("minecraft", "end_city")         to "cobbletunes:vanilla_structure:end_city",
        Identifier.of("bca", "village/small") to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/mid")   to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/large") to "cobbletunes:vanilla_structure:bca_village_large",
        Identifier.of("bca", "village/dark_small")     to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/default_small")  to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/fighting_small") to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/dark_mid")       to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/default_mid")    to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/fighting_mid")   to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/default_large")  to "cobbletunes:vanilla_structure:bca_village_large",
        Identifier.of("bca", "village/fighting_large") to "cobbletunes:vanilla_structure:bca_village_large",
        Identifier.of("bca", "village/fairy_small")    to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/fairy_mid")      to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/ice_small")      to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/ice_mid")        to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/ice_large")      to "cobbletunes:vanilla_structure:bca_village_large",
        Identifier.of("bca", "village/witch_hut")      to "cobbletunes:vanilla_structure:swamp_hut",
    )

    // terralith reuses existing pools
    private val TERRALITH_STRUCTURES: Map<Identifier, String> = mapOf(
        Identifier.of("terralith", "desert_outpost")                 to "cobbletunes:vanilla_structure:pillager_outpost",
        Identifier.of("terralith", "fortified_desert_village")     to "cobbletunes:vanilla_structure:village_desert",
        Identifier.of("terralith", "fortified_village")            to "cobbletunes:vanilla_structure:bca_village_large",
        Identifier.of("terralith", "glacial_hut")                  to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("terralith", "igloo")                        to "cobbletunes:vanilla_structure:igloo",
        Identifier.of("terralith", "mage_complex")                 to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "mage_tower")                   to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "mage_tower_autumn")            to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "mage_tower_spring")            to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "mage_tower_summer")            to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "mage_tower_winter")            to "cobbletunes:vanilla_structure:mansion",
        Identifier.of("terralith", "rubble_desert")                to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "rubble_forest")                to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "rubble_jungle")                to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "rubble_mesa")                  to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "rubble_mountain")              to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "rubble_taiga")                 to "cobbletunes:vanilla_structure:trail_ruins",
        Identifier.of("terralith", "spire")                        to "cobbletunes:vanilla_structure:end_city",
        Identifier.of("terralith", "underground/frosted_dungeon") to "cobbletunes:vanilla_structure:stronghold",
        Identifier.of("terralith", "underground/giant_bee_hive")  to "cobbletunes:vanilla_structure:jungle_pyramid",
        Identifier.of("terralith", "underground/mining_outpost")  to "cobbletunes:vanilla_structure:mineshaft",
        Identifier.of("terralith", "underground/oak_cabin")       to "cobbletunes:vanilla_structure:village_taiga",
        Identifier.of("terralith", "underground/old_refinery")    to "cobbletunes:vanilla_structure:mineshaft_mesa",
        Identifier.of("terralith", "underground/sunken_tower")    to "cobbletunes:vanilla_structure:ocean_ruin",
        Identifier.of("terralith", "valley_lodge")                 to "cobbletunes:vanilla_structure:village_taiga",
        Identifier.of("terralith", "witch_hut")                    to "cobbletunes:vanilla_structure:swamp_hut",
    )

    private val legendaryMonumentsAvailable by lazy { FabricLoader.getInstance().isModLoaded("legendarymonuments") }

    private val LEGENDARY_MONUMENT_STRUCTURES: Map<Identifier, String> = mapOf(
        // legendary monuments light, all 13 structures
        Identifier.of("legendarymonuments", "distortion_portal")   to "legendarymonuments:distortion_portal",
        Identifier.of("legendarymonuments", "eternatus_cocoon")    to "legendarymonuments:eternatus_cocoon",
        Identifier.of("legendarymonuments", "firescourge_shrine")  to "legendarymonuments:firescourge_shrine",
        Identifier.of("legendarymonuments", "giratina_island")     to "legendarymonuments:giratina_island",
        Identifier.of("legendarymonuments", "grasswither_shrine")  to "legendarymonuments:grasswither_shrine",
        Identifier.of("legendarymonuments", "groundblight_shrine") to "legendarymonuments:groundblight_shrine",
        Identifier.of("legendarymonuments", "icerend_shrine")      to "legendarymonuments:icerend_shrine",
        Identifier.of("legendarymonuments", "lake_acuity")         to "legendarymonuments:lake_acuity",
        Identifier.of("legendarymonuments", "lake_valor")          to "legendarymonuments:lake_valor",
        Identifier.of("legendarymonuments", "lake_verity")         to "legendarymonuments:lake_verity",
        Identifier.of("legendarymonuments", "outskirt_stand")      to "legendarymonuments:outskirt_stand",
        Identifier.of("legendarymonuments", "stark_mountain")      to "legendarymonuments:stark_mountain",
        Identifier.of("legendarymonuments", "turnback_cave")       to "legendarymonuments:turnback_cave",
    )

    private val repurposedStructuresAvailable by lazy { FabricLoader.getInstance().isModLoaded("repurposed_structures") }

    private val REPURPOSED_STRUCTURES: Map<Identifier, String> = buildMap {
        fun rs(category: String, vararg names: String) {
            val zone = "cobbletunes:vanilla_structure:$category"
            names.forEach { name ->
                put(Identifier.of("repurposed_structures", name), zone)
            }
        }

        rs("ancient_city", "ancient_city_end", "ancient_city_nether", "ancient_city_ocean")
        rs("bastion_remnant", "bastion_underground")
        rs("bca_village_large", "city_overworld")
        rs("fortress", "city_nether", "fortress_jungle")
        rs("fortress", "temple_nether_basalt", "temple_nether_crimson", "temple_nether_soul", "temple_nether_warped", "temple_nether_wasteland")

        rs("igloo", "igloo_grassy", "igloo_mangrove", "igloo_mushroom", "igloo_stone", "pyramid_icy", "pyramid_snowy")
        rs("mansion", "mansion_birch", "mansion_desert", "mansion_jungle", "mansion_mangrove", "mansion_oak", "mansion_savanna", "mansion_snowy", "mansion_taiga")
        rs("mineshaft", "mineshaft_basalt", "mineshaft_birch", "mineshaft_crimson", "mineshaft_dark_forest", "mineshaft_desert", "mineshaft_end", "mineshaft_icy", "mineshaft_jungle", "mineshaft_nether", "mineshaft_ocean", "mineshaft_savanna", "mineshaft_soul", "mineshaft_stone", "mineshaft_swamp", "mineshaft_taiga", "mineshaft_warped")
        rs("monument", "monument_desert", "monument_icy", "monument_jungle", "monument_nether", "pyramid_ocean", "temple_ocean")
        rs("pillager_outpost", "outpost_badlands", "outpost_basalt", "outpost_birch", "outpost_crimson", "outpost_desert", "outpost_end", "outpost_giant_tree_taiga", "outpost_icy", "outpost_jungle", "outpost_mangrove", "outpost_nether_brick", "outpost_oak", "outpost_ocean", "outpost_savanna", "outpost_snowy", "outpost_soul", "outpost_taiga", "outpost_warped")

        rs("desert_pyramid", "pyramid_badlands", "pyramid_end", "pyramid_nether")
        rs("jungle_pyramid", "pyramid_dark_forest", "pyramid_flower_forest", "pyramid_giant_tree_taiga", "pyramid_jungle", "pyramid_mushroom", "temple_taiga")
        rs("ruined_portal", "ruined_portal_end", "ruins_nether")
        rs("trail_ruins", "ruins_land_cold", "ruins_land_hot", "ruins_land_icy", "ruins_land_warm")
        rs("shipwreck", "shipwreck_crimson", "shipwreck_end", "shipwreck_nether_bricks", "shipwreck_warped")
        rs("stronghold", "stronghold_end", "stronghold_nether")

        rs("village_desert", "village_badlands")
        rs("village_plains", "village_birch", "village_cherry", "village_mushroom", "village_oak", "village_ocean")
        rs("village_savanna", "village_bamboo", "village_crimson", "village_jungle")
        rs("village_taiga", "village_dark_forest", "village_giant_taiga", "village_mountains", "village_swamp", "village_warped")
        rs("swamp_hut", "witch_hut_birch", "witch_hut_dark_forest", "witch_hut_giant_tree_taiga", "witch_hut_mangrove", "witch_hut_oak", "witch_hut_taiga")
    }


    private data class ZoneDetection(
        val zoneId: String,
        val source: String,
        val structureId: Identifier? = null
    ) {
        fun debugSignature(): String =
            "$source|${structureId?.toString().orEmpty()}|$zoneId"
    }

    private data class StructureRoute(
        val structureId: Identifier,
        val zoneId: String,
        val priority: Int
    )

    private data class StructureCandidate(
        val route: StructureRoute,
        val volume: Long
    )

    // only send real zone changes
    private val playerZoneCache: MutableMap<java.util.UUID, String> = mutableMapOf()
    private val playerDetectionDebugCache: MutableMap<java.util.UUID, String> = mutableMapOf()
    private var tickCounter = 0

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            if (tickCounter < ZONE_CHECK_INTERVAL_TICKS) return@register
            tickCounter = 0

            for (player in server.playerManager.playerList) {
                if (!ServerPlayNetworking.canSend(player, StructureZonePayload.ID)) {
                    playerZoneCache.remove(player.uuid)
                    playerDetectionDebugCache.remove(player.uuid)
                    continue
                }

                val world = player.serverWorld
                val detection = detectZone(player, world)
                val zone = detection.zoneId
                val previous = playerZoneCache[player.uuid] ?: ""

                if (zone != previous) {
                    playerZoneCache[player.uuid] = zone
                    ServerPlayNetworking.send(player, StructureZonePayload(zone))
                }

                if (CobbleTunesServerConfig.current.debugLogging) {
                    val signature = detection.debugSignature()
                    if (playerDetectionDebugCache[player.uuid] != signature) {
                        playerDetectionDebugCache[player.uuid] = signature
                        val registry = detection.structureId?.toString() ?: "-"
                        LOGGER.info(
                            "[$MOD_ID] [Debug] [Structure] player=${player.name.string} " +
                                "source=${detection.source} registry=$registry zone='${detection.zoneId}'"
                        )
                    }
                }
            }
        }
    }

    private fun detectZone(player: ServerPlayerEntity, world: ServerWorld): ZoneDetection {
        // manual zones win first
        val triggerZone = scanForManualZone(player, world)
        if (triggerZone != null) {
            return ZoneDetection(triggerZone, "manual")
        }

        // then real structure starts
        return locateNearbyStructure(player, world)
            ?: ZoneDetection("", "none")
    }

    private fun scanForManualZone(player: ServerPlayerEntity, world: ServerWorld): String? {
        // scan max range, then check each marker's own range
        val box = player.boundingBox.expand(MAX_MANUAL_ZONE_RADIUS.toDouble())

        // same zone id won't restart across markers
        return world.getOtherEntities(null, box) { entity ->
            entity.commandTags.any { it.startsWith(MANUAL_ZONE_TAG_PREFIX) }
        }
            .mapNotNull { entity ->
                val zoneId = entity.commandTags
                    .firstOrNull { it.startsWith(MANUAL_ZONE_TAG_PREFIX) }
                    ?.removePrefix(MANUAL_ZONE_TAG_PREFIX)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val radius = resolveManualZoneRadius(entity.commandTags)
                val distanceSquared = entity.squaredDistanceTo(player)
                if (distanceSquared > radius.toDouble() * radius.toDouble()) {
                    return@mapNotNull null
                }

                distanceSquared to zoneId
            }
            .minByOrNull { it.first }
            ?.second
    }

    private fun resolveManualZoneRadius(commandTags: Set<String>): Int {
        val explicitRadius = commandTags
            .asSequence()
            .filter { it.startsWith(MANUAL_ZONE_RANGE_TAG_PREFIX) }
            .mapNotNull { tag ->
                tag.removePrefix(MANUAL_ZONE_RANGE_TAG_PREFIX)
                    .toIntOrNull()
                    ?.takeIf { it in MIN_MANUAL_ZONE_RADIUS..MAX_MANUAL_ZONE_RADIUS }
            }
            // duplicate ranges use the smaller one
            .minOrNull()

        return explicitRadius ?: DEFAULT_MANUAL_ZONE_RADIUS
    }

    private fun locateNearbyStructure(
        player: ServerPlayerEntity,
        world: ServerWorld
    ): ZoneDetection? {
        val structureRegistry = world.registryManager.get(RegistryKeys.STRUCTURE)

        // reverse map keeps chunk checks cheap
        val structureToRoute =
            mutableMapOf<net.minecraft.world.gen.structure.Structure, StructureRoute>()

        fun routePriority(zoneId: String, defaultPriority: Int): Int =
            if (
                "village_" in zoneId ||
                "bca_village_" in zoneId
            ) {
                PRIORITY_VILLAGE
            } else {
                defaultPriority
            }

        fun addRoutes(
            mappings: Map<Identifier, String>,
            defaultPriority: Int
        ) {
            for ((structureId, zoneId) in mappings) {
                val structure = structureRegistry.get(structureId) ?: continue
                structureToRoute[structure] = StructureRoute(
                    structureId = structureId,
                    zoneId = zoneId,
                    priority = routePriority(zoneId, defaultPriority)
                )
            }
        }

        addRoutes(GYM_STRUCTURES, PRIORITY_SPECIAL_STRUCTURE)
        addRoutes(COBBLEMON_STRUCTURES, PRIORITY_SPECIAL_STRUCTURE)
        addRoutes(COBBLEMON_POOLED_STRUCTURES, PRIORITY_GENERIC_STRUCTURE)
        addRoutes(VANILLA_AND_BCA_STRUCTURES, PRIORITY_GENERIC_STRUCTURE)
        addRoutes(TERRALITH_STRUCTURES, PRIORITY_GENERIC_STRUCTURE)

        if (legendaryMonumentsAvailable) {
            addRoutes(LEGENDARY_MONUMENT_STRUCTURES, PRIORITY_SPECIAL_STRUCTURE)
        }
        if (repurposedStructuresAvailable) {
            addRoutes(REPURPOSED_STRUCTURES, PRIORITY_GENERIC_STRUCTURE)
        }
        if (structureToRoute.isEmpty()) return null

        val playerChunk = player.chunkPos
        val playerPos = player.blockPos
        val accessor = world.structureAccessor
        val seenStarts = mutableSetOf<net.minecraft.structure.StructureStart>()
        val candidates = mutableListOf<StructureCandidate>()

        for (dx in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
            for (dz in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
                val chunk = world.getChunk(
                    playerChunk.x + dx,
                    playerChunk.z + dz,
                    net.minecraft.world.chunk.ChunkStatus.STRUCTURE_REFERENCES,
                    false
                ) ?: continue

                val starts = accessor.getStructureStarts(chunk.pos) { structure ->
                    structure in structureToRoute
                }

                for (start in starts) {
                    if (!start.hasChildren() || !seenStarts.add(start)) continue
                    val route = structureToRoute[start.structure] ?: continue
                    val box = start.boundingBox
                    val width = box.maxX - box.minX + 1
                    val height = box.maxY - box.minY + 1
                    val depth = box.maxZ - box.minZ + 1
                    val isSmallStructure =
                        width <= SMALL_STRUCTURE_MAX_SPAN && depth <= SMALL_STRUCTURE_MAX_SPAN
                    val horizontalPadding =
                        if (isSmallStructure) SMALL_STRUCTURE_PADDING else STRUCTURE_PADDING
                    val verticalPadding = if (isSmallStructure) {
                        SMALL_STRUCTURE_VERTICAL_PADDING
                    } else {
                        STRUCTURE_VERTICAL_PADDING
                    }

                    val inside =
                        playerPos.x in (box.minX - horizontalPadding)..(box.maxX + horizontalPadding) &&
                            playerPos.y in (box.minY - verticalPadding)..(box.maxY + verticalPadding) &&
                            playerPos.z in (box.minZ - horizontalPadding)..(box.maxZ + horizontalPadding)

                    if (!inside) continue

                    val volume = width.toLong().coerceAtLeast(1L) *
                        height.toLong().coerceAtLeast(1L) *
                        depth.toLong().coerceAtLeast(1L)

                    candidates += StructureCandidate(route, volume)
                }
            }
        }

        val selected = candidates
            .sortedWith(
                compareByDescending<StructureCandidate> { it.route.priority }
                    .thenBy { it.volume }
                    .thenBy { it.route.structureId.toString() }
            )
            .firstOrNull()
            ?: return null

        return ZoneDetection(
            zoneId = selected.route.zoneId,
            source = "server-structure",
            structureId = selected.route.structureId
        )
    }
}
