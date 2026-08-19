package com.kaizzinho.cobbletunes.event

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import com.kaizzinho.cobbletunes.world.MusicTriggerBlock
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
    private const val SMALL_STRUCTURE_PADDING = 2
    private const val STRUCTURE_PADDING = 1
    private const val STRUCTURE_VERTICAL_PADDING = 2
    private const val TRIGGER_BLOCK_RADIUS = 12
    private val TRIGGER_SCAN_OFFSETS: List<Triple<Int, Int, Int>> by lazy {
        val r = TRIGGER_BLOCK_RADIUS
        buildList {
            for (x in -r..r) {
                for (y in -r..r) {
                    for (z in -r..r) {
                        add(Triple(x, y, z))
                    }
                }
            }
        }.sortedBy { (x, y, z) -> x * x + y * y + z * z }
    }

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

    // core cobblemon structures
    private val COBBLEMON_STRUCTURES: Map<Identifier, String> = mapOf(
        Identifier.of("cobblemon", "ruins/deserted_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/frozen_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/lush_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/rooted_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/sunscorched_gimmi_tower") to "cobbletunes:gimmighoul_tower",
        Identifier.of("cobblemon", "ruins/temperate_gimmi_tower") to "cobbletunes:gimmighoul_tower"
    )

    // vanilla bca structures share music pools
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
        Identifier.of("bca", "village/witch_hut")      to "cobbletunes:vanilla_structure:swamp_hut",
    )

    // terralith datapack structures reuse existing pools
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
        Identifier.of("legendarymonuments", "distortion_portal") to "legendarymonuments:distortion_portal",
        Identifier.of("legendarymonuments", "giratina_island")   to "legendarymonuments:giratina_island",
        Identifier.of("legendarymonuments", "turnback_cave")     to "legendarymonuments:turnback_cave",
        Identifier.of("legendarymonuments", "lake_acuity")       to "legendarymonuments:lake_acuity",
        Identifier.of("legendarymonuments", "lake_valor")        to "legendarymonuments:lake_valor",
        Identifier.of("legendarymonuments", "lake_verity")       to "legendarymonuments:lake_verity",
        Identifier.of("legendarymonuments", "stark_mountain")    to "legendarymonuments:stark_mountain",
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


    // only send real zone changes
    private val playerZoneCache: MutableMap<java.util.UUID, String> = mutableMapOf()
    private var tickCounter = 0

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            if (tickCounter < ZONE_CHECK_INTERVAL_TICKS) return@register
            tickCounter = 0

            for (player in server.playerManager.playerList) {
                val world = player.serverWorld
                val zone = detectZone(player, world)
                val previous = playerZoneCache[player.uuid] ?: ""
                if (zone != previous) {
                    playerZoneCache[player.uuid] = zone
                    ServerPlayNetworking.send(player, StructureZonePayload(zone))
                    if (CobbleTunesServerConfig.current.debugLogging) {
                        LOGGER.info("[$MOD_ID] [Debug] Zone change for ${player.name.string}: '$previous' → '$zone'")
                    }
                }
            }
        }
    }


    private fun detectZone(player: ServerPlayerEntity, world: ServerWorld): String {
        // manual zones win first
        val triggerZone = scanForTriggerBlock(player, world)
        if (triggerZone != null) return triggerZone

        // worldgen zones come next
        return locateNearbyStructure(player, world) ?: ""
    }


    private fun scanForTriggerBlock(player: ServerPlayerEntity, world: ServerWorld): String? {
        val center = player.blockPos

        // tower floors overlap so nearest trigger wins
        for ((x, y, z) in TRIGGER_SCAN_OFFSETS) {
            val pos = center.add(x, y, z)
            val state = world.getBlockState(pos)
            if (state.block !is MusicTriggerBlock) continue

            val be = world.getBlockEntity(pos)
            if (be is MusicTriggerBlock.Entity) {
                return be.zoneId.ifBlank { null }
            }
        }
        return null
    }

    private fun locateNearbyStructure(player: ServerPlayerEntity, world: ServerWorld): String? {
        val structureRegistry = world.registryManager.get(RegistryKeys.STRUCTURE)

        // reverse map keeps chunk lookups cheap
        val structureToZone = mutableMapOf<net.minecraft.world.gen.structure.Structure, String>()
        for ((structureId, zoneId) in GYM_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        for ((structureId, zoneId) in COBBLEMON_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        for ((structureId, zoneId) in VANILLA_AND_BCA_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        for ((structureId, zoneId) in TERRALITH_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        if (legendaryMonumentsAvailable) {
            for ((structureId, zoneId) in LEGENDARY_MONUMENT_STRUCTURES) {
                val structure = structureRegistry.get(structureId) ?: continue
                structureToZone[structure] = zoneId
            }
        }
        if (repurposedStructuresAvailable) {
            for ((structureId, zoneId) in REPURPOSED_STRUCTURES) {
                val structure = structureRegistry.get(structureId) ?: continue
                structureToZone[structure] = zoneId
            }
        }
        if (structureToZone.isEmpty()) return null

        val playerChunk = player.chunkPos
        val playerPos = player.blockPos
        val accessor = world.structureAccessor

        var nearestZoneId: String? = null
        var nearestDistSq = Double.MAX_VALUE
        val seenStarts = mutableSetOf<net.minecraft.structure.StructureStart>()

        for (dx in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
            for (dz in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
                val chunk = world.getChunk(
                    playerChunk.x + dx,
                    playerChunk.z + dz,
                    net.minecraft.world.chunk.ChunkStatus.STRUCTURE_REFERENCES,
                    false
                ) ?: continue

                val starts = accessor.getStructureStarts(chunk.pos) { structure ->
                    structure in structureToZone
                }

                for (start in starts) {
                    if (!start.hasChildren() || !seenStarts.add(start)) continue
                    val zoneId = structureToZone[start.structure] ?: continue
                    val box = start.boundingBox
                    val xPadding = if (box.maxX - box.minX + 1 <= SMALL_STRUCTURE_MAX_SPAN) {
                        SMALL_STRUCTURE_PADDING
                    } else {
                        STRUCTURE_PADDING
                    }
                    val zPadding = if (box.maxZ - box.minZ + 1 <= SMALL_STRUCTURE_MAX_SPAN) {
                        SMALL_STRUCTURE_PADDING
                    } else {
                        STRUCTURE_PADDING
                    }

                    val closestX = playerPos.x.coerceIn(box.minX - xPadding, box.maxX + xPadding)
                    val closestY = playerPos.y.coerceIn(
                        box.minY - STRUCTURE_VERTICAL_PADDING,
                        box.maxY + STRUCTURE_VERTICAL_PADDING
                    )
                    val closestZ = playerPos.z.coerceIn(box.minZ - zPadding, box.maxZ + zPadding)
                    val ddx = (playerPos.x - closestX).toDouble()
                    val ddy = (playerPos.y - closestY).toDouble()
                    val ddz = (playerPos.z - closestZ).toDouble()
                    val distSq = ddx * ddx + ddy * ddy + ddz * ddz

                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq
                        nearestZoneId = zoneId
                    }
                }
            }
        }
        return nearestZoneId?.takeIf { nearestDistSq == 0.0 }
    }
}
