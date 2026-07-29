package com.kaizzinho.cobbletunes.event

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import com.kaizzinho.cobbletunes.world.MusicTriggerBlock
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/**
 * Pillar 9: server-side zone detector. Runs once every ZONE_CHECK_INTERVAL_TICKS
 * (~5 seconds) per online player. Checks two signal sources in priority order:
 *
 * 1. MusicTriggerBlock scan (higher priority) — scans a tight radius around the
 *    player for any placed MusicTriggerBlock and uses its zoneId directly. This
 *    covers hand-placed Poké Centers and Poké Marts (WorldEdit schematics).
 *
 * 2. Worldgen structure check — calls ServerWorld.locateStructure() for each
 *    known Cobbleverse gym structure within GYM_DETECT_RADIUS blocks. Uses the
 *    nearest result. This covers all Cobbleverse gym structures automatically
 *    without requiring any manual block placement inside them.
 *
 * Sends StructureZonePayload to the player only when their zone CHANGES — no
 * packet spam while they stand still inside a gym.
 *
 * Known gym structure IDs — all under the "cobbleverse" namespace, matching
 * exactly the filenames in cobbleverse/worldgen/structure/ from the datapack.
 * Kanto is the only region currently in the datapack; others are listed here
 * ready for when Johto/Hoenn/Sinnoh datapacks are added — no code change needed.
 *
 * !! VERIFY ServerWorld.locateStructure() signature against your jar !!
 * The standard 1.21.1 call shape is:
 *   world.locateStructure(structureEntry, centerPos, searchRadius, skipExisting)
 * where structureEntry comes from:
 *   world.registryManager.get(RegistryKeys.STRUCTURE).getEntry(Identifier.of(...))
 * This hasn't been genSources-confirmed here — ctrl-click locateStructure in
 * IntelliJ after adding this file to verify the exact overload.
 */
object StructureZoneDetector {

    private const val ZONE_CHECK_INTERVAL_TICKS = 100  // ~5 seconds
    private const val CHUNK_SCAN_RADIUS = 3             // chunks around player to scan for structure references
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
                    LOGGER.debug("[$MOD_ID] Zone change for ${player.name.string}: '$previous' → '$zone'")
                }
            }
        }
    }

    /**
     * Detects the highest-priority zone the player is currently in.
     * Trigger blocks (POKECENTER/POKEMART) beat worldgen structures so that
     * a hand-placed Poké Center near a gym plays center music, not gym music.
     */
    private fun detectZone(player: ServerPlayerEntity, world: ServerWorld): String {
        // 1. Check for nearby MusicTriggerBlock (hand-placed structures)
        val triggerZone = scanForTriggerBlock(player, world)
        if (triggerZone != null) return triggerZone

        // 2. Check for nearby worldgen gym structure
        return locateNearbyGym(player, world) ?: ""
    }

    /**
     * Scans a cubic radius around the player for any MusicTriggerBlock.
     * Returns the block's zoneId, or null if none found.
     * Iterates a moderate radius — 24 blocks keeps this cheap enough for a
     * 5-second tick but covers typical building interiors comfortably.
     */
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

    /**
     * Checks each known gym structure for proximity to the player.
     * Detects nearby Cobbleverse structures using chunk structure references
     * rather than locateStructure() — the latter's overload signature varies
     * across 1.21.x Yarn mappings, making it fragile. ChunkAccess.structureReferences
     * returns every Structure whose start chunk overlaps a given chunk, which is
     * exactly what we need: if the player's chunk (or an adjacent one within
     * CHUNK_SCAN_RADIUS) has a reference to cobbleverse:brock, they are inside
     * or very near that gym.
     *
     * !! VERIFY chunk.structureReferences property name against your jar !!
     * Yarn 1.21.1 name — returns Map<Structure, LongSet>. If IntelliJ shows a
     * different name, swap it here; nothing else in this function changes.
     */
    private fun locateNearbyGym(player: ServerPlayerEntity, world: ServerWorld): String? {
        val structureRegistry = world.registryManager.get(RegistryKeys.STRUCTURE)

        // Build reverse map: Structure object → zoneId, for O(1) lookup
        // against whatever structureReferences returns per chunk.
        val structureToZone = mutableMapOf<net.minecraft.world.gen.structure.Structure, String>()
        for ((structureId, zoneId) in GYM_STRUCTURES) {
            val structure = structureRegistry.get(structureId) ?: continue
            structureToZone[structure] = zoneId
        }
        if (structureToZone.isEmpty()) return null

        val playerChunk = player.chunkPos
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
                    return zoneId
                }
            }
        }
        return null
    }
}