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

object StructureZoneDetector {

    private const val ZONE_CHECK_INTERVAL_TICKS = 100  // ~5 seconds
    private const val CHUNK_SCAN_RADIUS = 3             // chunks around player to scan for structure references
    // How close the player must actually be to a structure's REAL bounding box
    // (not just "a reference chunk was found nearby") to count as inside it.
    // Sprawling jigsaw structures — villages especially — leave a structure
    // reference in every chunk their bounding box touches, including outlying
    // paths/houses far from the visible center, so the reference scan alone
    // is not a reliable proximity signal on its own.
    private const val STRUCTURE_PROXIMITY_MARGIN = 24.0
    private const val TRIGGER_BLOCK_RADIUS = 12        // blocks, for MusicTriggerBlock scan

    // All known Cobbleverse worldgen gym/league structures → their zoneId string.
    // The zoneId is sent verbatim to the client, which maps it to a RegionOfOrigin.
    private val GYM_STRUCTURES: Map<Identifier, String> = mapOf(
        // Kanto gyms
        Identifier.of("cobbleverse", "brock")             to "cobbleverse:brock",
        Identifier.of("cobbleverse", "misty")             to "cobbleverse:misty",
        Identifier.of("cobbleverse", "ltsurge")           to "cobbleverse:ltsurge",
        Identifier.of("cobbleverse", "erika")             to "cobbleverse:erika",
        Identifier.of("cobbleverse", "koga")              to "cobbleverse:koga",
        Identifier.of("cobbleverse", "sabrina")           to "cobbleverse:sabrina",
        Identifier.of("cobbleverse", "blaine")            to "cobbleverse:blaine",
        Identifier.of("cobbleverse", "giovanni")          to "cobbleverse:giovanni",
        // Kanto league / special
        Identifier.of("cobbleverse", "kanto_league")      to "cobbleverse:kanto_league",
        Identifier.of("cobbleverse", "team_rocket_tower") to "cobbleverse:team_rocket_tower",
        Identifier.of("cobbleverse", "crown_spire")       to "cobbleverse:crown_spire",
        Identifier.of("cobbleverse", "dawn_tower")        to "cobbleverse:dawn_tower",
        Identifier.of("cobbleverse", "dusk_tower")        to "cobbleverse:dusk_tower",
        // Johto gyms (gym leaders have custom NPC names in Cobbleverse)
        Identifier.of("cobbleverse", "valerio")           to "cobbleverse:valerio",
        Identifier.of("cobbleverse", "chiara")            to "cobbleverse:chiara",
        Identifier.of("cobbleverse", "angelo")            to "cobbleverse:angelo",
        Identifier.of("cobbleverse", "alfredo")           to "cobbleverse:alfredo",
        Identifier.of("cobbleverse", "furio")             to "cobbleverse:furio",
        Identifier.of("cobbleverse", "jasmine")           to "cobbleverse:jasmine",
        Identifier.of("cobbleverse", "raffaello")         to "cobbleverse:raffaello",
        Identifier.of("cobbleverse", "sandra")            to "cobbleverse:sandra",
        // Johto league / special
        Identifier.of("cobbleverse", "johto_league")      to "cobbleverse:johto_league",
        Identifier.of("cobbleverse", "rocket_radio_tower") to "cobbleverse:rocket_radio_tower",
        // Hoenn gyms
        Identifier.of("cobbleverse", "rudi")              to "cobbleverse:rudi",
        Identifier.of("cobbleverse", "adriano")           to "cobbleverse:adriano",
        Identifier.of("cobbleverse", "tell_pat")          to "cobbleverse:tell_pat",
        Identifier.of("cobbleverse", "alice")             to "cobbleverse:alice",
        Identifier.of("cobbleverse", "norman")            to "cobbleverse:norman",
        Identifier.of("cobbleverse", "fiammetta")         to "cobbleverse:fiammetta",
        Identifier.of("cobbleverse", "walter")            to "cobbleverse:walter",
        Identifier.of("cobbleverse", "petra")             to "cobbleverse:petra",
        // Hoenn league
        Identifier.of("cobbleverse", "hoenn_league")      to "cobbleverse:hoenn_league",
        // Sinnoh gyms
        Identifier.of("cobbleverse", "gardenia")          to "cobbleverse:gardenia",
        Identifier.of("cobbleverse", "ferruccio")         to "cobbleverse:ferruccio",
        Identifier.of("cobbleverse", "marzia")            to "cobbleverse:marzia",
        Identifier.of("cobbleverse", "fannie")            to "cobbleverse:fannie",
        Identifier.of("cobbleverse", "corrado")           to "cobbleverse:corrado",
        Identifier.of("cobbleverse", "bianca")            to "cobbleverse:bianca",
        Identifier.of("cobbleverse", "omar")              to "cobbleverse:omar",
        Identifier.of("cobbleverse", "pedro")             to "cobbleverse:pedro",
        // Sinnoh league
        Identifier.of("cobbleverse", "sinnoh_league")     to "cobbleverse:sinnoh_league",
        Identifier.of("cobbleverse", "team_galactic_hq")  to "cobbleverse:team_galactic_hq",

        // Kanto special structures
        Identifier.of("cobbleverse", "ash")            to "cobbleverse:ash",
        Identifier.of("cobbleverse", "crown_cemetery") to "cobbleverse:crown_cemetery",
        Identifier.of("cobbleverse", "articuno")       to "cobbleverse:articuno",
        Identifier.of("cobbleverse", "zapdos")         to "cobbleverse:zapdos",
        Identifier.of("cobbleverse", "moltres")        to "cobbleverse:moltres",
        Identifier.of("cobbleverse", "mew")            to "cobbleverse:mew",

        // Johto special structures
        Identifier.of("cobbleverse", "bell_tower")     to "cobbleverse:bell_tower",
        Identifier.of("cobbleverse", "burned_tower")   to "cobbleverse:burned_tower",
        Identifier.of("cobbleverse", "celebi_shrine")  to "cobbleverse:celebi_shrine",
        Identifier.of("cobbleverse", "whirl_island")   to "cobbleverse:whirl_island",

        // Hoenn special structures
        Identifier.of("cobbleverse", "groudon")        to "cobbleverse:groudon",
        Identifier.of("cobbleverse", "kyogre")         to "cobbleverse:kyogre",
        Identifier.of("cobbleverse", "regirock")       to "cobbleverse:regirock",
        Identifier.of("cobbleverse", "regice")         to "cobbleverse:regice",
        Identifier.of("cobbleverse", "registeel")      to "cobbleverse:registeel",
        Identifier.of("cobbleverse", "deoxys")         to "cobbleverse:deoxys",
        Identifier.of("cobbleverse", "jirachi")        to "cobbleverse:jirachi",
        Identifier.of("cobbleverse", "secret_garden")  to "cobbleverse:secret_garden",
        Identifier.of("cobbleverse", "sky_pillar")     to "cobbleverse:sky_pillar",
        Identifier.of("cobbleverse", "dyna_tree")      to "cobbleverse:dyna_tree",

        // Sinnoh special structures
        Identifier.of("cobbleverse", "spear_pillar")         to "cobbleverse:spear_pillar",
        Identifier.of("cobbleverse", "snowpoint_temple")     to "cobbleverse:snowpoint_temple",
        Identifier.of("cobbleverse", "split_decision_temple") to "cobbleverse:split_decision_temple",
        Identifier.of("cobbleverse", "flower_paradise")      to "cobbleverse:flower_paradise",
        Identifier.of("cobbleverse", "fullmoon_island")      to "cobbleverse:fullmoon_island",
        Identifier.of("cobbleverse", "crescent_isle")        to "cobbleverse:crescent_isle",
        Identifier.of("cobbleverse", "eterna_building")      to "cobbleverse:eterna_building",
        Identifier.of("cobbleverse", "wind_plant")           to "cobbleverse:wind_plant",
        Identifier.of("cobbleverse", "manaphy")              to "cobbleverse:manaphy",
    )

    // Pillar 9 extension: vanilla + BCA structures. zoneId format is
    // "cobbletunes:vanilla_structure:<category>" — CobbleTunesClient strips
    // the prefix and looks up the category in TrackRegistry.vanillaStructureTrackFor().
    // Unlike GYM_STRUCTURES (1:1 structure→zoneId), several vanilla structure
    // variants map to the SAME category (ocean_ruin_cold/warm both → "ocean_ruin",
    // all 7 ruined_portal variants → "ruined_portal") since they share one pool.
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
        // BCA (CobblemonAdditions) — namespace confirmed via its datapack.
        Identifier.of("bca", "village/small") to "cobbletunes:vanilla_structure:bca_village_small",
        Identifier.of("bca", "village/mid")   to "cobbletunes:vanilla_structure:bca_village_mid",
        Identifier.of("bca", "village/large") to "cobbletunes:vanilla_structure:bca_village_large",
    )

    // Per-player zone tracking — avoids sending packets when nothing changed.
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
        // 1. Check for nearby MusicTriggerBlock (hand-placed structures)
        val triggerZone = scanForTriggerBlock(player, world)
        if (triggerZone != null) return triggerZone

        // 2. Check for nearby worldgen gym structure
        return locateNearbyGym(player, world) ?: ""
    }


    private fun scanForTriggerBlock(player: ServerPlayerEntity, world: ServerWorld): String? {
        val center = player.blockPos
        val r = TRIGGER_BLOCK_RADIUS
        for (x in -r..r) {
            for (y in -r..r) {
                for (z in -r..r) {
                    val pos = center.add(x, y, z)
                    val state = world.getBlockState(pos)
                    if (state.block is MusicTriggerBlock) {
                        val be = world.getBlockEntity(pos)
                        if (be is MusicTriggerBlock.Entity) {
                            return be.zoneId.ifBlank { null }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun locateNearbyGym(player: ServerPlayerEntity, world: ServerWorld): String? {
        val structureRegistry = world.registryManager.get(RegistryKeys.STRUCTURE)

        // Build reverse map: Structure object → zoneId, for O(1) lookup
        // against whatever structureReferences returns per chunk. Combines
        // Cobbleverse structures with vanilla/BCA structures into one lookup
        // since the chunk scan itself is identical for both.
        val structureToZone = mutableMapOf<net.minecraft.world.gen.structure.Structure, String>()
        for ((structureId, zoneId) in GYM_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        for ((structureId, zoneId) in VANILLA_AND_BCA_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        if (structureToZone.isEmpty()) return null

        val playerChunk = player.chunkPos
        val playerPos = player.blockPos
        val accessor = world.structureAccessor

        var nearestZoneId: String? = null
        var nearestDistSq = Double.MAX_VALUE

        for (dx in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
            for (dz in -CHUNK_SCAN_RADIUS..CHUNK_SCAN_RADIUS) {
                val chunk = world.getChunk(
                    playerChunk.x + dx,
                    playerChunk.z + dz,
                    net.minecraft.world.chunk.ChunkStatus.STRUCTURE_REFERENCES,
                    false
                ) ?: continue

                for ((structure, _) in chunk.structureReferences) {
                    val zoneId = structureToZone[structure] ?: continue

                    // Reference found — now verify REAL proximity against the
                    // structure's actual bounding box before accepting it.
                    val start = accessor.getStructureAt(playerPos, structure)
                    if (!start.hasChildren()) continue

                    val box = start.boundingBox
                    val closestX = playerPos.x.coerceIn(box.minX, box.maxX)
                    val closestY = playerPos.y.coerceIn(box.minY, box.maxY)
                    val closestZ = playerPos.z.coerceIn(box.minZ, box.maxZ)
                    val ddx = (playerPos.x - closestX).toDouble()
                    val ddy = (playerPos.y - closestY).toDouble()
                    val ddz = (playerPos.z - closestZ).toDouble()
                    val distSq = ddx * ddx + ddy * ddy + ddz * ddz

                    if (distSq <= STRUCTURE_PROXIMITY_MARGIN * STRUCTURE_PROXIMITY_MARGIN &&
                        distSq < nearestDistSq) {
                        nearestDistSq = distSq
                        nearestZoneId = zoneId
                    }
                }
            }
        }
        return nearestZoneId
    }
}