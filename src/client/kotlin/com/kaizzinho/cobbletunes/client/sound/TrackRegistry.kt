package com.kaizzinho.cobbletunes.client.sound

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

data class MusicTrack(
    val id: String,
    val soundEvent: SoundEvent,
    val loop: Boolean = true,
    val biomeKeys: Set<String> = emptySet(),
    val legendaryDexOverrides: Set<Int> = emptySet(),
    val regions: Set<RegionOfOrigin> = emptySet(),
    val durationSeconds: Int? = null,
    val factionThemes: Set<String> = emptySet()
)

data class EvolutionCue(
    val id: String,
    val soundEvent: SoundEvent,
    val assetPath: String
)

data class EvolutionTheme(
    val suspense: List<EvolutionCue>,
    val complete: EvolutionCue
)

object TrackRegistry {
    private val tracks: MutableMap<MusicContext, MutableList<MusicTrack>> =
        MusicContext.entries.associateWith { mutableListOf<MusicTrack>() }.toMutableMap()

    private val victoryTracks = mutableMapOf<String, MusicTrack>()
    private val battleTowerTracks = mutableMapOf<String, MutableList<MusicTrack>>()

    fun register(context: MusicContext, track: MusicTrack) {
        tracks.getValue(context).add(track)
    }

    fun tracksFor(context: MusicContext): List<MusicTrack> = tracks[context].orEmpty()

    fun trackById(trackId: String): MusicTrack? =
        tracks.values.asSequence().flatten().firstOrNull { it.id == trackId }

    fun legendaryTrackFor(
        dexNumber: Int,
        formName: String = "",
        regionalVariant: String = ""
    ): MusicTrack? {
        val pool = tracks[MusicContext.LEGENDARY_BATTLE].orEmpty()
        val form = formName.trim().lowercase().replace('-', '_')
        val variantRegion = RegionOfOrigin.fromRegionalVariant(regionalVariant)
            ?: RegionOfOrigin.fromRegionalVariant(formName)

        val formSpecificTrackId = when (dexNumber) {
            144, 145, 146 -> if (form == "galar") "galar_swsh_legendary_birds" else "kalos_xy_legendary_mewtwo"
            646 -> if (form == "white" || form == "black") "unova_legendary_black_white_kyurem" else "unova_bw_legendary_kyurem"
            800 -> when (form) {
                "ultra" -> "alola_usum_legendary_ultra_necrozma"
                "dusk_mane", "dawn_wings" -> "alola_legendary_necrozma_fused"
                else -> "alola_legendary_solgaleo_lunala_necrozma"
            }
            890 -> if (form == "eternamax") "galar_swsh_legendary_eternatus_final" else "galar_legendary_eternatus"
            898 -> if (form == "ice" || form == "shadow") "galar_swsh_legendary_calyrex_fused" else "galar_swsh_legendary_calyrex"
            1024 -> if (form == "stellar") "paldea_sv_legendary_terapagos_stellar" else "paldea_sv_legendary_terapagos"
            else -> null
        }
        formSpecificTrackId?.let { id -> pool.firstOrNull { it.id == id }?.let { return it } }

        pool.filter { dexNumber in it.legendaryDexOverrides }
            .randomOrNull()?.let { return it }

        (variantRegion ?: RegionOfOrigin.fromDexNumber(dexNumber))?.let { region ->
            pool.filter { region in it.regions && it.legendaryDexOverrides.isEmpty() }
                .randomOrNull()?.let { return it }
        }

        return pool.filter { it.legendaryDexOverrides.isEmpty() && it.regions.isEmpty() }
            .randomOrNull()
            ?: wildTrackFor(dexNumber, regionalVariant)
    }

    fun regionalBattleTrackFor(
        context: MusicContext,
        dexNumbers: List<Int>,
        preferredRegion: RegionOfOrigin? = null,
        regionalVariants: List<String> = emptyList()
    ): MusicTrack? {
        val pool = tracks[context].orEmpty()
        if (pool.isEmpty()) return null

        // trainer metadata beats roster votes
        val targetRegion = preferredRegion ?: resolveRosterRegion(dexNumbers, regionalVariants)
        if (targetRegion != null) {
            pool.filter { targetRegion in it.regions }.randomOrNull()?.let { return it }

            // hisui falls back to regional wild music
            tracks[MusicContext.WILD_BATTLE].orEmpty()
                .filter { targetRegion in it.regions }
                .randomOrNull()
                ?.let { return it }
        }

        return pool.filter { it.regions.isEmpty() }.randomOrNull()
            ?: pool.randomOrNull()
    }

    data class BossTrackPick(
        val track: MusicTrack,
        val region: RegionOfOrigin?,
        val source: String,
        val context: MusicContext
    )

    private data class BossPoolWeights(
        val regionalPvp: Int,
        val genericLegendary: Int,
        val worldTournament: Int
    )

    private data class WeightedBossPool(
        val source: String,
        val context: MusicContext,
        val weight: Int,
        val tracks: List<MusicTrack>
    )

    private val lastBossTrackByRegion = mutableMapOf<RegionOfOrigin?, String>()

    fun bossTrackFor(
        dexNumbers: List<Int>,
        tierName: String,
        regionalVariants: List<String> = emptyList()
    ): BossTrackPick? {
        val region = resolveRosterRegion(dexNumbers, regionalVariants)
        val weights = bossWeights(tierName)

        val regionalPvp = tracks[MusicContext.PVP_BATTLE].orEmpty().filter { track ->
            (region == null || region in track.regions) && !isWorldTournamentRemix(track)
        }
        val genericLegendary = tracks[MusicContext.LEGENDARY_BATTLE].orEmpty().filter { track ->
            track.legendaryDexOverrides.isEmpty() &&
                (region == null || region in track.regions)
        }
        val worldTournament = tracks[MusicContext.PVP_BATTLE].orEmpty().filter { track ->
            isWorldTournamentRemix(track) &&
                (region == null || region in track.regions)
        }

        val pools = listOf(
            WeightedBossPool("regional-pvp", MusicContext.PVP_BATTLE, weights.regionalPvp, regionalPvp),
            WeightedBossPool(
                "generic-legendary",
                MusicContext.LEGENDARY_BATTLE,
                weights.genericLegendary,
                genericLegendary
            ),
            WeightedBossPool(
                "bw-world-tournament",
                MusicContext.PVP_BATTLE,
                weights.worldTournament,
                worldTournament
            )
        ).filter { it.weight > 0 && it.tracks.isNotEmpty() }

        val lastTrackId = lastBossTrackByRegion[region]
        var selected = chooseBossPoolTrack(pools)

        val alternativesExist = pools
            .flatMap { it.tracks }
            .any { it.id != lastTrackId }

        if (selected?.track?.id == lastTrackId && alternativesExist) {
            chooseBossPoolTrack(pools)?.let { selected = it }
        }

        selected?.let { pick ->
            lastBossTrackByRegion[region] = pick.track.id
            return BossTrackPick(pick.track, region, pick.source, pick.context)
        }

        val wild = region?.let { target ->
            tracks[MusicContext.WILD_BATTLE].orEmpty()
                .filter { target in it.regions }
                .randomOrNull()
        } ?: tracks[MusicContext.WILD_BATTLE].orEmpty().randomOrNull()

        return wild?.let {
            lastBossTrackByRegion[region] = it.id
            BossTrackPick(it, region, "wild-fallback", MusicContext.WILD_BATTLE)
        }
    }

    private fun bossWeights(tierName: String): BossPoolWeights = when (tierName.uppercase()) {
        "UNCOMMON" -> BossPoolWeights(80, 15, 5)
        "RARE" -> BossPoolWeights(70, 20, 10)
        "EPIC" -> BossPoolWeights(60, 30, 10)
        "LEGENDARY" -> BossPoolWeights(50, 35, 15)
        "MYTHIC" -> BossPoolWeights(45, 40, 15)
        else -> BossPoolWeights(60, 30, 10)
    }

    private fun chooseBossPoolTrack(
        pools: List<WeightedBossPool>
    ): WeightedSelection? {
        if (pools.isEmpty()) return null

        val totalWeight = pools.sumOf { it.weight }
        var roll = kotlin.random.Random.nextInt(totalWeight)

        for (pool in pools) {
            if (roll < pool.weight) {
                return WeightedSelection(
                    track = pool.tracks.random(),
                    source = pool.source,
                    context = pool.context
                )
            }
            roll -= pool.weight
        }

        return pools.last().let { pool ->
            WeightedSelection(pool.tracks.random(), pool.source, pool.context)
        }
    }

    private data class WeightedSelection(
        val track: MusicTrack,
        val source: String,
        val context: MusicContext
    )

    private fun isWorldTournamentRemix(track: MusicTrack): Boolean =
        track.id.startsWith("unova_pvp_champion_")

    fun resolveRosterRegion(
        dexNumbers: List<Int>,
        regionalVariants: List<String> = emptyList()
    ): RegionOfOrigin? {
        val votes = dexNumbers.mapIndexedNotNull { index, dexNumber ->
            RegionOfOrigin.fromRegionalVariant(regionalVariants.getOrNull(index).orEmpty())
                ?: RegionOfOrigin.fromDexNumber(dexNumber)
        }
        if (votes.isEmpty()) return null

        val counts = votes.groupingBy { it }.eachCount()
        val topCount = counts.values.max()
        return counts.filterValues { it == topCount }.keys.random()
    }

    fun trainerTrackFor(
        dexNumbers: List<Int>,
        preferredRegion: RegionOfOrigin? = null,
        regionalVariants: List<String> = emptyList()
    ): MusicTrack? =
        regionalBattleTrackFor(
            MusicContext.TRAINER_BATTLE,
            dexNumbers,
            preferredRegion,
            regionalVariants
        )

    fun factionBattleTrackFor(themeId: String): MusicTrack? {
        val normalized = themeId.trim().lowercase()
        return tracks[MusicContext.FACTION_BATTLE]
            .orEmpty()
            .filter { normalized in it.factionThemes }
            .randomOrNull()
    }

    fun wildTrackFor(dexNumber: Int, regionalVariant: String = ""): MusicTrack? {
        val pool = tracks[MusicContext.WILD_BATTLE].orEmpty()

        (RegionOfOrigin.fromRegionalVariant(regionalVariant)
            ?: RegionOfOrigin.fromDexNumber(dexNumber))?.let { region ->
            pool.filter { region in it.regions }.randomOrNull()?.let { return it }
        }

        return pool.filter { it.regions.isEmpty() }.randomOrNull()
    }

    fun ambienceTrackFor(
        biomeId: String,
        region: RegionOfOrigin?,
        excludeTrackId: String? = null
    ): MusicTrack? {
        val pool = tracks[MusicContext.AMBIENCE].orEmpty()

        fun pick(candidates: List<MusicTrack>): MusicTrack? {
            if (candidates.isEmpty()) return null
            val alternatives = excludeTrackId
                ?.let { excluded -> candidates.filter { it.id != excluded } }
                .orEmpty()
            return (if (alternatives.isNotEmpty()) alternatives else candidates).randomOrNull()
        }

        region?.let { r ->
            pick(pool.filter { r in it.regions && biomeId in it.biomeKeys })
                ?.let { return it }
        }

        pick(pool.filter { biomeId in it.biomeKeys })?.let { return it }

        region?.let { r ->
            pick(pool.filter { r in it.regions && it.biomeKeys.isEmpty() })
                ?.let { return it }
        }

        return pick(pool.filter { it.regions.isEmpty() && it.biomeKeys.isEmpty() })
    }

    fun victoryTrackFor(request: VictoryRequest): MusicTrack? {
        val region = request.region.name.lowercase()
        val factionKey = when (request.factionTheme) {
            "team_aqua_magma_grunt", "team_aqua_magma_leaders" -> "hoenn.faction"
            "team_galactic_grunt", "team_galactic_commander", "team_galactic_boss" -> "sinnoh.faction"
            "team_plasma_grunt", "team_plasma_n", "team_plasma_colress" -> "unova.faction"
            else -> null
        }

        val keys = buildList {
            if (request.kind == VictoryKind.FACTION && factionKey != null) add(factionKey)
            when (request.kind) {
                VictoryKind.WILD -> add("$region.wild")
                VictoryKind.TRAINER, VictoryKind.RIVAL, VictoryKind.FRONTIER_BRAIN -> add("$region.trainer")
                VictoryKind.GYM_LEADER -> { add("$region.gym"); add("$region.trainer") }
                VictoryKind.ELITE_FOUR -> { add("$region.e4"); add("$region.gym"); add("$region.trainer") }
                VictoryKind.CHAMPION -> { add("$region.champion"); add("$region.gym"); add("$region.trainer") }
                VictoryKind.FACTION -> add("$region.trainer")
            }
            add("$region.generic")
            add("$region.wild")
        }

        return keys.firstNotNullOfOrNull(victoryTracks::get)
    }

    fun isBattleTowerZone(zoneId: String): Boolean =
        battleTowerPoolKey(zoneId) != null

    fun battleTowerTrackFor(zoneId: String): MusicTrack? =
        battleTowerPoolKey(zoneId)?.let { battleTowerTracks[it] }?.randomOrNull()

    private fun battleTowerPoolKey(zoneId: String): String? {
        if (zoneId in battleTowerTracks) return zoneId

        val floor = zoneId.removePrefix("cobbletunes:battle_tower_floor_")
            .takeIf { zoneId.startsWith("cobbletunes:battle_tower_floor_") }
            ?.toIntOrNull()
            ?: return null

        return when (floor) {
            in 1..3 -> "cobbletunes:battle_tower_low"
            in 4..7 -> "cobbletunes:battle_tower_mid"
            in 8..9 -> "cobbletunes:battle_tower_high"
            10 -> "cobbletunes:battle_tower_final"
            else -> null
        }
    }

    fun battleTowerBattleTrack(): MusicTrack? =
        tracks[MusicContext.BATTLE_TOWER_BATTLE].orEmpty().randomOrNull()

    private fun evolutionCue(prefix: String, suffix: String): EvolutionCue =
        EvolutionCue(
            id = "evolution.$prefix.$suffix",
            soundEvent = soundEvent("evolution.$prefix.$suffix"),
            assetPath = "sounds/evolution/${prefix}_${suffix}.ogg"
        )

    private val evolutionThemes: Map<RegionOfOrigin, EvolutionTheme> by lazy {
        mapOf(
            RegionOfOrigin.KANTO to EvolutionTheme(
                listOf(evolutionCue("fr", "evo")),
                evolutionCue("fr", "congrat")
            ),
            RegionOfOrigin.JOHTO to EvolutionTheme(
                listOf(evolutionCue("hg", "evo")),
                evolutionCue("hg", "congrat")
            ),
            RegionOfOrigin.HOENN to EvolutionTheme(
                listOf(evolutionCue("em", "evo")),
                evolutionCue("em", "congrat")
            ),
            RegionOfOrigin.SINNOH to EvolutionTheme(
                listOf(evolutionCue("plat", "evo")),
                evolutionCue("plat", "congrat")
            ),
            RegionOfOrigin.UNOVA to EvolutionTheme(
                listOf(evolutionCue("bl", "evo")),
                evolutionCue("bl", "congrat")
            ),
            RegionOfOrigin.KALOS to EvolutionTheme(
                listOf(evolutionCue("xy", "evo")),
                evolutionCue("xy", "congrat")
            ),
            RegionOfOrigin.ALOLA to EvolutionTheme(
                listOf(evolutionCue("um", "evo"), evolutionCue("um", "evo2")),
                evolutionCue("um", "congrat")
            ),
            RegionOfOrigin.GALAR to EvolutionTheme(
                listOf(evolutionCue("swsh", "evo"), evolutionCue("swsh", "evo2")),
                evolutionCue("swsh", "congrat")
            ),
            RegionOfOrigin.HISUI to EvolutionTheme(
                listOf(evolutionCue("leg", "evo")),
                evolutionCue("leg", "congrat")
            ),
            RegionOfOrigin.PALDEA to EvolutionTheme(
                listOf(evolutionCue("sv", "evo")),
                evolutionCue("sv", "congrat")
            )
        )
    }

    fun evolutionThemeFor(region: RegionOfOrigin): EvolutionTheme? =
        evolutionThemes[region]

    fun soundEvent(path: String): SoundEvent =
        SoundEvent.of(Identifier.of(MOD_ID, path))

    private val KANTO_PLAINS = setOf(
        "minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains",
        "terralith:valley_clearing", "terralith:shield_clearing", "terralith:steppe",
        "terralith:shrubland", "terralith:shield", "terralith:yosemite_lowlands",
        "terralith:vibrant_prairie"
    )
    private val KANTO_FOREST = setOf(
        "minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
        "terralith:amethyst_rainforest", "terralith:lush_valley", "terralith:lavender_forest"
    )
    private val KANTO_FOOTHILLS = setOf(
        "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
        "terralith:white_cliffs", "terralith:amethyst_canyon", "terralith:granite_cliffs",
        "terralith:rocky_mountains"
    )
    private val KANTO_CAVE = setOf(
        "minecraft:dripstone_caves", "terralith:andesite_caves", "terralith:crystal_caves",
        "terralith:deep_caves", "terralith:diorite_caves", "terralith:granite_caves",
        "terralith:infested_caves", "terralith:mantle_caves", "terralith:thermal_caves",
        "terralith:tuff_caves"
    )
    private val KANTO_COASTAL = setOf(
        "minecraft:beach", "minecraft:stony_shore", "minecraft:snowy_beach",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:white_cliffs", "terralith:gravel_beach", "terralith:rainbow_beach",
        "terralith:mirage_isles", "terralith:warm_river"
    )
    private val KANTO_OCEAN = setOf(
        "minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean",
        "minecraft:cold_ocean", "minecraft:frozen_ocean",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:alpha_islands", "terralith:alpha_islands_winter"
    )
    private val KANTO_VOLCANIC = setOf(
        "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
        "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
        "minecraft:soul_sand_valley", "minecraft:basalt_deltas",
        "terralith:volcanic_crater", "terralith:warped_mesa", "terralith:yellowstone",
        "terralith:ashen_savanna", "terralith:basalt_cliffs", "terralith:caldera",
        "terralith:savanna_badlands"
    )

    private val JOHTO_PLAINS = setOf(
        "minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains",
        "terralith:valley_clearing", "terralith:blooming_valley", "terralith:blooming_plateau",
        "terralith:lavender_valley", "terralith:steppe", "terralith:shield_clearing"
    )
    private val JOHTO_FOREST = setOf(
        "minecraft:flower_forest", "minecraft:forest", "minecraft:birch_forest",
        "minecraft:swamp", "minecraft:mushroom_fields", "minecraft:cherry_grove",
        "terralith:sakura_grove", "terralith:sakura_valley", "terralith:moonlight_grove",
        "terralith:orchid_swamp", "terralith:lavender_forest", "terralith:lush_valley"
    )
    private val JOHTO_CAVE = setOf(
        "minecraft:dripstone_caves", "terralith:andesite_caves", "terralith:fungal_caves",
        "terralith:crystal_caves", "terralith:diorite_caves", "terralith:tuff_caves"
    )
    private val JOHTO_ICE_CAVE = setOf(
        "terralith:ice_caves", "terralith:frostfire_caves", "terralith:glacial_chasm"
    )
    private val JOHTO_LUSH_CAVE = setOf(
        "minecraft:lush_caves", "terralith:underground_jungle"
    )
    private val JOHTO_COASTAL = setOf(
        "minecraft:beach", "minecraft:stony_shore", "minecraft:snowy_beach",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:white_cliffs", "terralith:gravel_beach", "terralith:warm_river"
    )
    private val JOHTO_FOOTHILLS = setOf(
        "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
        "terralith:rocky_mountains", "terralith:white_cliffs", "terralith:amethyst_canyon"
    )
    private val JOHTO_HARSH_MOUNTAIN = setOf(
        "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:frozen_peaks",
        "terralith:scarlet_mountains", "terralith:painted_mountains", "terralith:haze_mountain",
        "terralith:highlands", "terralith:temperate_highlands", "terralith:emerald_peaks",
        "terralith:stony_spires", "terralith:windswept_spires", "terralith:forested_highlands",
        "terralith:volcanic_peaks", "terralith:mountain_steppe", "terralith:alpine_highlands",
        "terralith:alpine_grove", "terralith:yosemite_cliffs"
    )
    private val JOHTO_OCEAN = setOf(
        "minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean",
        "minecraft:cold_ocean", "minecraft:frozen_ocean",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:alpha_islands", "terralith:alpha_islands_winter"
    )

    private val HOENN_PLAINS = setOf(
        "minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains",
        "terralith:valley_clearing", "terralith:shield_clearing", "terralith:blooming_valley",
        "terralith:vibrant_prairie", "terralith:shrubland"
    )
    private val HOENN_FOREST = setOf(
        "minecraft:forest", "minecraft:flower_forest", "minecraft:dark_forest",
        "minecraft:mangrove_swamp", "minecraft:old_growth_birch_forest",
        "terralith:amethyst_rainforest", "terralith:cloud_forest", "terralith:lush_valley",
        "terralith:moonlight_grove"
    )
    private val HOENN_JUNGLE = setOf(
        "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle",
        "terralith:tropical_jungle", "terralith:rocky_jungle", "terralith:jungle_mountains"
    )
    private val HOENN_DESERT = setOf(
        "minecraft:desert", "minecraft:savanna", "minecraft:savanna_plateau",
        "minecraft:windswept_savanna",
        "terralith:desert_oasis", "terralith:lush_desert", "terralith:ancient_sands",
        "terralith:desert_canyon", "terralith:desert_spires", "terralith:gravel_desert",
        "terralith:hot_shrubland", "terralith:red_oasis", "terralith:brushland",
        "terralith:rocky_shrubland", "terralith:arid_highlands", "terralith:bryce_canyon",
        "terralith:fractured_savanna", "terralith:sandstone_valley", "terralith:savanna_slopes",
        "terralith:white_mesa"
    )
    private val HOENN_VOLCANIC = setOf(
        "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
        "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
        "minecraft:soul_sand_valley", "minecraft:basalt_deltas",
        "terralith:volcanic_crater", "terralith:caldera", "terralith:yellowstone",
        "terralith:ashen_savanna", "terralith:savanna_badlands"
    )
    private val HOENN_MOUNTAIN = setOf(
        "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
        "minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:frozen_peaks",
        "terralith:scarlet_mountains", "terralith:skylands", "terralith:skylands_autumn",
        "terralith:skylands_summer", "terralith:skylands_spring", "terralith:highlands",
        "terralith:temperate_highlands", "terralith:forested_highlands"
    )
    private val HOENN_OCEAN = setOf(
        "minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean",
        "minecraft:cold_ocean", "minecraft:frozen_ocean",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:alpha_islands", "terralith:alpha_islands_winter",
        "terralith:gravel_beach", "terralith:rainbow_beach", "terralith:mirage_isles"
    )
    private val HOENN_DEEP_OCEAN = setOf(
        "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean",
        "minecraft:deep_cold_ocean", "minecraft:deep_frozen_ocean"
    )
    private val HOENN_CAVE = setOf(
        "minecraft:dripstone_caves", "terralith:andesite_caves", "terralith:desert_caves",
        "terralith:granite_caves", "terralith:infested_caves", "terralith:deep_caves"
    )

    private val SINNOH_PLAINS = setOf(
        "minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains",
        "terralith:valley_clearing", "terralith:shield_clearing", "terralith:blooming_plateau",
        "terralith:mountain_steppe", "terralith:shield"
    )
    private val SINNOH_FOREST = setOf(
        "minecraft:forest", "minecraft:dark_forest", "minecraft:birch_forest",
        "minecraft:old_growth_birch_forest", "minecraft:swamp",
        "terralith:amethyst_rainforest", "terralith:moonlight_grove", "terralith:orchid_swamp"
    )
    private val SINNOH_SNOWY = setOf(
        "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:snowy_plains",
        "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:grove",
        "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
        "terralith:wintry_forest", "terralith:wintry_lowlands", "terralith:cold_shrubland",
        "terralith:ice_marsh", "terralith:siberian_grove", "terralith:siberian_taiga",
        "terralith:snowy_badlands", "terralith:birch_taiga", "terralith:frozen_cliffs",
        "terralith:skylands_winter", "terralith:snowy_cherry_grove",
        "terralith:snowy_maple_forest", "terralith:snowy_shield", "terralith:alpha_islands_winter"
    )
    private val SINNOH_MOUNTAIN = setOf(
        "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
        "minecraft:stony_peaks", "minecraft:jagged_peaks",
        "terralith:scarlet_mountains", "terralith:painted_mountains", "terralith:haze_mountain",
        "terralith:emerald_peaks", "terralith:stony_spires", "terralith:windswept_spires",
        "terralith:volcanic_peaks", "terralith:alpine_highlands", "terralith:alpine_grove",
        "terralith:yosemite_cliffs", "terralith:skylands", "terralith:skylands_autumn",
        "terralith:skylands_summer", "terralith:skylands_spring"
    )
    private val SINNOH_CAVE_LUSH = setOf(
        "minecraft:lush_caves", "terralith:underground_jungle", "terralith:crystal_caves"
    )
    private val SINNOH_CAVE_MINE = setOf(
        "minecraft:dripstone_caves", "terralith:andesite_caves", "terralith:mantle_caves",
        "terralith:thermal_caves", "terralith:tuff_caves"
    )
    private val SINNOH_OCEAN = setOf(
        "minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean",
        "minecraft:cold_ocean", "minecraft:frozen_ocean",
        "minecraft:river", "minecraft:frozen_river",
        "terralith:alpha_islands", "terralith:warm_river"
    )

    private val UNOVA_PLAINS = setOf(
        "minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains",
        "terralith:valley_clearing", "terralith:moonlight_valley", "terralith:blooming_valley",
        "terralith:steppe", "terralith:vibrant_prairie"
    )
    private val UNOVA_CAVE = setOf(
        "minecraft:dripstone_caves", "terralith:andesite_caves", "terralith:fungal_caves",
        "terralith:infested_caves", "terralith:deep_caves"
    )
    private val UNOVA_SNOWY = setOf(
        "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:snowy_plains",
        "minecraft:snowy_slopes", "minecraft:grove", "minecraft:taiga",
        "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
        "terralith:wintry_forest", "terralith:wintry_lowlands", "terralith:cold_shrubland",
        "terralith:ice_marsh", "terralith:snowy_shield"
    )

    private val DEEP_DARK = setOf(
        "minecraft:deep_dark",
        "minecraft:nether_wastes", "minecraft:soul_sand_valley",
        "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:basalt_deltas"
    )
    private val THE_END = setOf(
        "minecraft:the_end", "minecraft:end_highlands", "minecraft:end_midlands",
        "minecraft:end_barrens", "minecraft:small_end_islands"
    )

    fun bootstrap() {
        registerKantoAmbience(); registerKantoBattle()
        registerJohtoAmbience(); registerJohtoBattle()
        registerHoennAmbience(); registerHoennBattle()
        registerSinnohAmbience(); registerSinnohBattle()
        registerUnovaAmbience(); registerUnovaBattle()
        registerAlolaBattle()
        registerGalarBattle()
        registerHisuiBattle()
        registerKalosBattle()
        registerPaldeaBattle()
        registerVictoryThemes()
        registerBattleTower()
        registerProximityAmbience()
        registerSpecialStructures()
        registerGameCornerThemes()
        registerMenuThemes()
        registerVanillaStructures()
    }
    private fun registerGameCornerThemes() {
        fun game(id: String) {
            register(
                MusicContext.GAME_CORNER,
                MusicTrack("gamecorner_$id", soundEvent("gamecorner.$id"), loop = false)
            )
        }

        game("frlg_rocket_game_corner")
        game("frlg_pokemon_jump")
        game("frlg_dodrio_berry_picking")
        game("frlg_union_room")

        game("emerald_game_corner")
        game("emerald_contest_lobby")
        game("emerald_pokemon_contest")
        game("emerald_trick_house")

        game("hgss_goldenrod_game_corner")
        game("hgss_bug_catching_contest")
        game("hgss_pokeathlon_event_time")
        game("hgss_wifi_plaza_games")

        game("platinum_game_corner")
        game("platinum_contest_hall")
        game("platinum_super_contest")
    }

    private fun registerMenuThemes() {
        register(MusicContext.MENU, MusicTrack("menu_emerald", soundEvent("menu.emerald"), loop = true))
        register(MusicContext.MENU, MusicTrack("menu_frlg",    soundEvent("menu.frlg"),    loop = true))
        register(MusicContext.MENU, MusicTrack("menu_hgss",    soundEvent("menu.hgss"),    loop = true))
    }

    private fun registerKantoAmbience() {
        register(MusicContext.AMBIENCE, MusicTrack("kanto_plains_road_to_viridian", soundEvent("ambience.kanto.plains_road_to_viridian"), biomeKeys = KANTO_PLAINS, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_plains_pallet_town", soundEvent("ambience.kanto.plains_pallet_town"), biomeKeys = KANTO_PLAINS, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_route_lavender_somber", soundEvent("ambience.kanto.route_lavender_somber"), biomeKeys = KANTO_PLAINS, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_forest_viridian", soundEvent("ambience.kanto.forest_viridian"), biomeKeys = KANTO_FOREST, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_route_cerulean_transition", soundEvent("ambience.kanto.route_cerulean_transition"), biomeKeys = KANTO_FOOTHILLS, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_cave_mt_moon", soundEvent("ambience.kanto.cave_mt_moon"), biomeKeys = KANTO_CAVE, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_coastal_to_bill", soundEvent("ambience.kanto.coastal_to_bill"), biomeKeys = KANTO_COASTAL, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_ocean_the_sea", soundEvent("ambience.kanto.ocean_the_sea"), biomeKeys = KANTO_OCEAN, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_volcanic_cinnabar", soundEvent("ambience.kanto.volcanic_cinnabar"), biomeKeys = KANTO_VOLCANIC, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_deep_dark_lavender_town", soundEvent("ambience.kanto.deep_dark_lavender_town"), biomeKeys = DEEP_DARK, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_deep_dark_pokemon_mansion", soundEvent("ambience.kanto.deep_dark_pokemon_mansion"), biomeKeys = DEEP_DARK, regions = setOf(RegionOfOrigin.KANTO), loop = false))

        val pokemonTower = soundEvent("ambience.kanto.pokemon_tower")
        register(MusicContext.AMBIENCE, MusicTrack("kanto_deep_dark_pokemon_tower", pokemonTower, biomeKeys = DEEP_DARK, regions = setOf(RegionOfOrigin.KANTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("kanto_end_pokemon_tower", pokemonTower, biomeKeys = THE_END, regions = setOf(RegionOfOrigin.KANTO), loop = false))

    }

    private fun registerKantoBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("kanto_wild", soundEvent("battle.kanto.wild"), regions = setOf(RegionOfOrigin.KANTO)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("kanto_trainer", soundEvent("battle.kanto.trainer"), regions = setOf(RegionOfOrigin.KANTO)))

        val gymLeader = soundEvent("battle.kanto.gym_leader")
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("kanto_gym_leader", gymLeader, regions = setOf(RegionOfOrigin.KANTO)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("kanto_elite_four", gymLeader, regions = setOf(RegionOfOrigin.KANTO)))

        val championRival = soundEvent("battle.kanto.champion_rival")
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("kanto_champion", championRival, regions = setOf(RegionOfOrigin.KANTO)))
        register(MusicContext.PVP_BATTLE, MusicTrack("kanto_rival_pvp", championRival, regions = setOf(RegionOfOrigin.KANTO)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("kanto_legendary_default", soundEvent("battle.kanto.legendary_default"), regions = setOf(RegionOfOrigin.KANTO)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("kanto_legendary_mewtwo", soundEvent("battle.kanto.legendary_mewtwo"), legendaryDexOverrides = setOf(150)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("kanto_legendary_deoxys", soundEvent("battle.kanto.legendary_deoxys"), legendaryDexOverrides = setOf(386)))
    }

    private fun registerJohtoAmbience() {
        register(MusicContext.AMBIENCE, MusicTrack("johto_plains_route_29", soundEvent("ambience.johto.plains_route_29"), biomeKeys = JOHTO_PLAINS, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_plains_route_30", soundEvent("ambience.johto.plains_route_30"), biomeKeys = JOHTO_PLAINS, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_plains_new_bark_town", soundEvent("ambience.johto.plains_new_bark_town"), biomeKeys = JOHTO_PLAINS, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_route_26_open", soundEvent("ambience.johto.route_26_open"), biomeKeys = JOHTO_PLAINS, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_forest_national_park", soundEvent("ambience.johto.forest_national_park"), biomeKeys = JOHTO_FOREST, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_forest_ecruteak", soundEvent("ambience.johto.forest_ecruteak"), biomeKeys = JOHTO_FOREST, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_cave_union_cave", soundEvent("ambience.johto.cave_union_cave"), biomeKeys = JOHTO_CAVE, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_cave_ruins_of_alph", soundEvent("ambience.johto.cave_ruins_of_alph"), biomeKeys = JOHTO_CAVE, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_cave_ice_path", soundEvent("ambience.johto.cave_ice_path"), biomeKeys = JOHTO_ICE_CAVE, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_cave_dragons_den", soundEvent("ambience.johto.cave_dragons_den"), biomeKeys = JOHTO_LUSH_CAVE, regions = setOf(RegionOfOrigin.JOHTO), loop = false))

        val pokegearUnown = soundEvent("ambience.johto.cave_deep_dark_pokegear_unown")
        register(MusicContext.AMBIENCE, MusicTrack("johto_cave_pokegear_unown", pokegearUnown, biomeKeys = JOHTO_CAVE, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_deep_dark_pokegear_unown", pokegearUnown, biomeKeys = DEEP_DARK, regions = setOf(RegionOfOrigin.JOHTO), loop = false))

        register(MusicContext.AMBIENCE, MusicTrack("johto_route_38_coastal", soundEvent("ambience.johto.route_38_coastal"), biomeKeys = JOHTO_COASTAL, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_route_42_mountain", soundEvent("ambience.johto.route_42_mountain"), biomeKeys = JOHTO_FOOTHILLS, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_route_47_harsh_mountain", soundEvent("ambience.johto.route_47_harsh_mountain"), biomeKeys = JOHTO_HARSH_MOUNTAIN, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_ocean_surf", soundEvent("ambience.johto.ocean_surf"), biomeKeys = JOHTO_OCEAN, regions = setOf(RegionOfOrigin.JOHTO), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("johto_end_sinjoh_ruins", soundEvent("ambience.johto.end_sinjoh_ruins"), biomeKeys = THE_END, regions = setOf(RegionOfOrigin.JOHTO), loop = false))

    }

    private fun registerJohtoBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("johto_wild", soundEvent("battle.johto.wild"), regions = setOf(RegionOfOrigin.JOHTO)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("johto_trainer", soundEvent("battle.johto.trainer"), regions = setOf(RegionOfOrigin.JOHTO)))

        val gymLeader = soundEvent("battle.johto.gym_leader")
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("johto_gym_leader", gymLeader, regions = setOf(RegionOfOrigin.JOHTO)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("johto_elite_four", gymLeader, regions = setOf(RegionOfOrigin.JOHTO)))

        register(MusicContext.CHAMPION_BATTLE, MusicTrack("johto_champion", soundEvent("battle.johto.champion"), regions = setOf(RegionOfOrigin.JOHTO)))
        register(MusicContext.PVP_BATTLE, MusicTrack("johto_rival_pvp", soundEvent("battle.johto.rival_pvp"), regions = setOf(RegionOfOrigin.JOHTO)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_default", soundEvent("battle.johto.legendary_default"), regions = setOf(RegionOfOrigin.JOHTO)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_raikou", soundEvent("battle.johto.legendary_raikou"), legendaryDexOverrides = setOf(243)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_entei", soundEvent("battle.johto.legendary_entei"), legendaryDexOverrides = setOf(244)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_suicune", soundEvent("battle.johto.legendary_suicune"), legendaryDexOverrides = setOf(245)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_lugia", soundEvent("battle.johto.legendary_lugia"), legendaryDexOverrides = setOf(249)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_legendary_ho_oh", soundEvent("battle.johto.legendary_ho_oh"), legendaryDexOverrides = setOf(250)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("johto_hgss_legendary_super_ancient", soundEvent("battle.johto.hgss_legendary_super_ancient"), legendaryDexOverrides = setOf(382, 383, 384)))

        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "johto_team_rocket",
                soundEvent("battle.johto.team_rocket"),
                factionThemes = setOf("team_rocket")
            )
        )
    }

    private fun registerHoennAmbience() {
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_route_101", soundEvent("ambience.hoenn.plains_route_101"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_route_104", soundEvent("ambience.hoenn.plains_route_104"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_route_110", soundEvent("ambience.hoenn.plains_route_110"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_route_120", soundEvent("ambience.hoenn.plains_route_120"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_littleroot_town", soundEvent("ambience.hoenn.plains_littleroot_town"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_plains_oldale_town", soundEvent("ambience.hoenn.plains_oldale_town"), biomeKeys = HOENN_PLAINS, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_forest_petalburg_woods", soundEvent("ambience.hoenn.forest_petalburg_woods"), biomeKeys = HOENN_FOREST, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_forest_safari_zone", soundEvent("ambience.hoenn.forest_safari_zone"), biomeKeys = HOENN_FOREST, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_jungle_route_119", soundEvent("ambience.hoenn.jungle_route_119"), biomeKeys = HOENN_JUNGLE, regions = setOf(RegionOfOrigin.HOENN), loop = false))

        val heavyRain = soundEvent("ambience.hoenn.heavy_rain")
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_jungle_heavy_rain", heavyRain, biomeKeys = HOENN_JUNGLE, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_ocean_heavy_rain", heavyRain, biomeKeys = HOENN_OCEAN, regions = setOf(RegionOfOrigin.HOENN), loop = false))

        register(MusicContext.AMBIENCE, MusicTrack("hoenn_desert_route_111", soundEvent("ambience.hoenn.desert_route_111"), biomeKeys = HOENN_DESERT, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_desert_drought", soundEvent("ambience.hoenn.desert_drought"), biomeKeys = HOENN_DESERT, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_volcanic_mt_chimney", soundEvent("ambience.hoenn.volcanic_mt_chimney"), biomeKeys = HOENN_VOLCANIC, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_volcanic_route_113", soundEvent("ambience.hoenn.volcanic_route_113"), biomeKeys = HOENN_VOLCANIC, regions = setOf(RegionOfOrigin.HOENN), loop = false))

        val mtPyre = soundEvent("ambience.hoenn.mt_pyre")
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_deep_dark_mt_pyre", mtPyre, biomeKeys = DEEP_DARK, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_volcanic_mt_pyre", mtPyre, biomeKeys = HOENN_VOLCANIC, regions = setOf(RegionOfOrigin.HOENN), loop = false))

        register(MusicContext.AMBIENCE, MusicTrack("hoenn_mountain_mt_pyre_exterior", soundEvent("ambience.hoenn.mountain_mt_pyre_exterior"), biomeKeys = HOENN_MOUNTAIN, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_ocean_crossing_the_sea", soundEvent("ambience.hoenn.ocean_crossing_the_sea"), biomeKeys = HOENN_OCEAN, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_ocean_surf", soundEvent("ambience.hoenn.ocean_surf"), biomeKeys = HOENN_OCEAN, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_ocean_oceanic_museum", soundEvent("ambience.hoenn.ocean_oceanic_museum"), biomeKeys = HOENN_OCEAN, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_deep_ocean_dive", soundEvent("ambience.hoenn.deep_ocean_dive"), biomeKeys = HOENN_DEEP_OCEAN, regions = setOf(RegionOfOrigin.HOENN), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("hoenn_cave_of_origin", soundEvent("ambience.hoenn.cave_of_origin"), biomeKeys = HOENN_CAVE, regions = setOf(RegionOfOrigin.HOENN), loop = false))

    }

    private fun registerHoennBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("hoenn_wild", soundEvent("battle.hoenn.wild"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("hoenn_trainer", soundEvent("battle.hoenn.trainer"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("hoenn_gym_leader", soundEvent("battle.hoenn.gym_leader"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("hoenn_elite_four", soundEvent("battle.hoenn.elite_four"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("hoenn_champion_wallace", soundEvent("battle.hoenn.champion_wallace"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.PVP_BATTLE, MusicTrack("hoenn_rival_pvp", soundEvent("battle.hoenn.rival_pvp"), regions = setOf(RegionOfOrigin.HOENN)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("hoenn_legendary_regis", soundEvent("battle.hoenn.legendary_regis"), legendaryDexOverrides = setOf(377, 378, 379)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("hoenn_legendary_super_ancient", soundEvent("battle.hoenn.legendary_super_ancient"), legendaryDexOverrides = setOf(382, 383, 384)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("hoenn_legendary_mew", soundEvent("battle.hoenn.legendary_mew"), legendaryDexOverrides = setOf(151)))

        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "hoenn_team_aqua_magma_grunt",
                soundEvent("battle.hoenn.team_aqua_magma_grunt"),
                factionThemes = setOf("team_aqua_magma_grunt")
            )
        )
        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "hoenn_team_aqua_magma_leaders",
                soundEvent("battle.hoenn.team_aqua_magma_leaders"),
                factionThemes = setOf("team_aqua_magma_leaders")
            )
        )
    }

    private fun registerSinnohAmbience() {
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_route_201", soundEvent("ambience.sinnoh.plains_route_201"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_route_203", soundEvent("ambience.sinnoh.plains_route_203"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_route_209", soundEvent("ambience.sinnoh.plains_route_209"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_route_210", soundEvent("ambience.sinnoh.plains_route_210"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_route_205", soundEvent("ambience.sinnoh.plains_route_205"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_twinleaf_town", soundEvent("ambience.sinnoh.plains_twinleaf_town"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_plains_sandgem_town", soundEvent("ambience.sinnoh.plains_sandgem_town"), biomeKeys = SINNOH_PLAINS, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_forest_eterna_forest", soundEvent("ambience.sinnoh.forest_eterna_forest"), biomeKeys = SINNOH_FOREST, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_forest_floaroma_town", soundEvent("ambience.sinnoh.forest_floaroma_town"), biomeKeys = SINNOH_FOREST, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_snowy_route_216", soundEvent("ambience.sinnoh.snowy_route_216"), biomeKeys = SINNOH_SNOWY, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_mountain_mt_coronet", soundEvent("ambience.sinnoh.mountain_mt_coronet"), biomeKeys = SINNOH_MOUNTAIN, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_cave_lake_caverns", soundEvent("ambience.sinnoh.cave_lake_caverns"), biomeKeys = SINNOH_CAVE_LUSH, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_cave_oreburgh_mine", soundEvent("ambience.sinnoh.cave_oreburgh_mine"), biomeKeys = SINNOH_CAVE_MINE, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_ocean_lake", soundEvent("ambience.sinnoh.ocean_lake"), biomeKeys = SINNOH_OCEAN, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_end_distortion_world", soundEvent("ambience.sinnoh.end_distortion_world"), biomeKeys = THE_END, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("sinnoh_snowy_snowpoint_city", soundEvent("ambience.sinnoh.snowy_snowpoint_city"), biomeKeys = SINNOH_SNOWY, regions = setOf(RegionOfOrigin.SINNOH), loop = false))
    }

    private fun registerSinnohBattle() {
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_legendary_alt", soundEvent("battle.sinnoh.legendary_alt"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.WILD_BATTLE, MusicTrack("sinnoh_wild", soundEvent("battle.sinnoh.wild"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("sinnoh_trainer", soundEvent("battle.sinnoh.trainer"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("sinnoh_gym_leader", soundEvent("battle.sinnoh.gym_leader"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("sinnoh_elite_four", soundEvent("battle.sinnoh.elite_four"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("sinnoh_champion", soundEvent("battle.sinnoh.champion"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.PVP_BATTLE, MusicTrack("sinnoh_rival_pvp", soundEvent("battle.sinnoh.rival_pvp"), regions = setOf(RegionOfOrigin.SINNOH)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_legendary_default", soundEvent("battle.sinnoh.legendary_default"), regions = setOf(RegionOfOrigin.SINNOH)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_legendary_dialga_palkia", soundEvent("battle.sinnoh.legendary_dialga_palkia"), legendaryDexOverrides = setOf(483, 484)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_legendary_giratina", soundEvent("battle.sinnoh.legendary_giratina"), legendaryDexOverrides = setOf(487)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_legendary_lake_trio", soundEvent("battle.sinnoh.legendary_lake_trio"), legendaryDexOverrides = setOf(480, 481, 482)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("sinnoh_platinum_legendary_arceus", soundEvent("battle.sinnoh.platinum_legendary_arceus"), legendaryDexOverrides = setOf(493)))

        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "sinnoh_team_galactic_grunt",
                soundEvent("battle.sinnoh.team_galactic_grunt"),
                factionThemes = setOf("team_galactic_grunt")
            )
        )
        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "sinnoh_team_galactic_commander",
                soundEvent("battle.sinnoh.team_galactic_commander"),
                factionThemes = setOf("team_galactic_commander")
            )
        )
        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "sinnoh_team_galactic_boss",
                soundEvent("battle.sinnoh.team_galactic_boss"),
                factionThemes = setOf("team_galactic_boss")
            )
        )
        register(
            MusicContext.FRONTIER_BRAIN_BATTLE,
            MusicTrack(
                "sinnoh_frontier_brain",
                soundEvent("battle.sinnoh.frontier_brain"),
                regions = setOf(RegionOfOrigin.SINNOH)
            )
        )
    }

    private fun registerUnovaAmbience() {
        register(MusicContext.AMBIENCE, MusicTrack("unova_plains_route_19_summer", soundEvent("ambience.unova.plains_route_19_summer"), biomeKeys = UNOVA_PLAINS, regions = setOf(RegionOfOrigin.UNOVA), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("unova_plains_aspertia_city", soundEvent("ambience.unova.plains_aspertia_city"), biomeKeys = UNOVA_PLAINS, regions = setOf(RegionOfOrigin.UNOVA), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("unova_cave_underground_ruins", soundEvent("ambience.unova.cave_underground_ruins"), biomeKeys = UNOVA_CAVE, regions = setOf(RegionOfOrigin.UNOVA), loop = false))
        register(MusicContext.AMBIENCE, MusicTrack("unova_snowy_frozen_city", soundEvent("ambience.unova.snowy_frozen_city"), biomeKeys = UNOVA_SNOWY, regions = setOf(RegionOfOrigin.UNOVA), loop = false))

    }

    private fun registerUnovaBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("unova_wild", soundEvent("battle.unova.wild"), regions = setOf(RegionOfOrigin.UNOVA)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("unova_trainer", soundEvent("battle.unova.trainer"), regions = setOf(RegionOfOrigin.UNOVA)))

        val gymLeader = soundEvent("battle.unova.gym_leader")
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("unova_gym_leader", gymLeader, regions = setOf(RegionOfOrigin.UNOVA)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("unova_elite_four", gymLeader, regions = setOf(RegionOfOrigin.UNOVA)))

        register(MusicContext.CHAMPION_BATTLE, MusicTrack("unova_champion_iris", soundEvent("battle.unova.champion_iris"), regions = setOf(RegionOfOrigin.UNOVA)))
        register(MusicContext.PVP_BATTLE, MusicTrack("unova_rival_hugh", soundEvent("battle.unova.rival_hugh"), regions = setOf(RegionOfOrigin.UNOVA)))

        // pwt remixes share the unova pvp pool
        register(MusicContext.PVP_BATTLE, MusicTrack("unova_pvp_champion_kanto", soundEvent("battle.unova.champion_kanto_pwt"), regions = setOf(RegionOfOrigin.KANTO)))
        register(MusicContext.PVP_BATTLE, MusicTrack("unova_pvp_champion_johto", soundEvent("battle.unova.champion_johto_pwt"), regions = setOf(RegionOfOrigin.JOHTO)))
        register(MusicContext.PVP_BATTLE, MusicTrack("unova_pvp_champion_hoenn", soundEvent("battle.unova.champion_hoenn_pwt"), regions = setOf(RegionOfOrigin.HOENN)))
        register(MusicContext.PVP_BATTLE, MusicTrack("unova_pvp_champion_sinnoh", soundEvent("battle.unova.champion_sinnoh_pwt"), regions = setOf(RegionOfOrigin.SINNOH)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("unova_bw_legendary_default", soundEvent("battle.unova.bw_legendary_default"), regions = setOf(RegionOfOrigin.UNOVA)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("unova_bw_legendary_reshiramzekrom", soundEvent("battle.unova.bw_legendary_reshiramzekrom"), legendaryDexOverrides = setOf(643, 644)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("unova_bw_legendary_kyurem", soundEvent("battle.unova.bw_legendary_kyurem"), legendaryDexOverrides = setOf(646)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("unova_legendary_black_white_kyurem", soundEvent("battle.unova.legendary_black_white_kyurem"), legendaryDexOverrides = setOf(646)))

        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "unova_team_plasma_grunt",
                soundEvent("battle.unova.team_plasma_grunt"),
                factionThemes = setOf("team_plasma_grunt")
            )
        )
        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "unova_team_plasma_n",
                soundEvent("battle.unova.team_plasma_n"),
                factionThemes = setOf("team_plasma_n")
            )
        )
        register(
            MusicContext.FACTION_BATTLE,
            MusicTrack(
                "unova_team_plasma_colress",
                soundEvent("battle.unova.team_plasma_colress"),
                factionThemes = setOf("team_plasma_colress")
            )
        )
    }
    private fun registerAlolaBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("alola_wild", soundEvent("battle.alola.wild"), regions = setOf(RegionOfOrigin.ALOLA)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("alola_trainer", soundEvent("battle.alola.trainer"), regions = setOf(RegionOfOrigin.ALOLA)))
        // alola uses kahunas for gym routes
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("alola_island_kahuna", soundEvent("battle.alola.island_kahuna"), regions = setOf(RegionOfOrigin.ALOLA)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("alola_elite_four", soundEvent("battle.alola.elite_four"), regions = setOf(RegionOfOrigin.ALOLA)))

        val champion = soundEvent("battle.alola.champion_summit")
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("alola_champion", champion, regions = setOf(RegionOfOrigin.ALOLA)))
        register(MusicContext.PVP_BATTLE, MusicTrack("alola_pvp_champion", champion, regions = setOf(RegionOfOrigin.ALOLA)))

        // ultra beasts use this fallback
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("alola_legendary_default_ultra_beast", soundEvent("battle.alola.legendary_ultra_beast"), regions = setOf(RegionOfOrigin.ALOLA)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("alola_legendary_tapu", soundEvent("battle.alola.legendary_tapu"), legendaryDexOverrides = setOf(785, 786, 787, 788)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("alola_legendary_solgaleo_lunala_necrozma", soundEvent("battle.alola.legendary_solgaleo_lunala_necrozma"), legendaryDexOverrides = setOf(791, 792, 800)))
        // necrozma keeps multiple valid themes
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("alola_legendary_necrozma_fused", soundEvent("battle.alola.legendary_necrozma_fused"), legendaryDexOverrides = setOf(800)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("alola_usum_legendary_ultra_necrozma", soundEvent("battle.alola.usum_legendary_ultra_necrozma"), legendaryDexOverrides = setOf(800)))

        register(MusicContext.FACTION_BATTLE, MusicTrack("alola_team_skull_guzma", soundEvent("battle.alola.team_skull_guzma"), factionThemes = setOf("team_skull_guzma")))
        register(MusicContext.FACTION_BATTLE, MusicTrack("alola_aether_foundation", soundEvent("battle.alola.aether_foundation"), factionThemes = setOf("aether_foundation")))
        register(MusicContext.FACTION_BATTLE, MusicTrack("alola_aether_president_lusamine", soundEvent("battle.alola.aether_president_lusamine"), factionThemes = setOf("aether_president_lusamine")))
        register(MusicContext.FACTION_BATTLE, MusicTrack("alola_ultra_recon_squad", soundEvent("battle.alola.ultra_recon_squad"), factionThemes = setOf("ultra_recon_squad")))
    }

    private fun registerGalarBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("galar_wild", soundEvent("battle.galar.wild"), regions = setOf(RegionOfOrigin.GALAR)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("galar_trainer", soundEvent("battle.galar.trainer"), regions = setOf(RegionOfOrigin.GALAR)))
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("galar_gym_leader", soundEvent("battle.galar.gym_leader"), regions = setOf(RegionOfOrigin.GALAR)))
        // galar league fills the elite four route
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("galar_elite_four", soundEvent("battle.galar.league_tournament"), regions = setOf(RegionOfOrigin.GALAR)))

        val champion = soundEvent("battle.galar.champion_leon")
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("galar_champion", champion, regions = setOf(RegionOfOrigin.GALAR)))
        register(MusicContext.PVP_BATTLE, MusicTrack("galar_pvp_champion", champion, regions = setOf(RegionOfOrigin.GALAR)))

        // dynamax theme is the galar fallback
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_legendary_default_mysterious_being", soundEvent("battle.galar.legendary_mysterious_being"), regions = setOf(RegionOfOrigin.GALAR)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_legendary_eternatus", soundEvent("battle.galar.legendary_eternatus"), legendaryDexOverrides = setOf(890)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_eternatus_final", soundEvent("battle.galar.swsh_legendary_eternatus_final"), legendaryDexOverrides = setOf(890)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_zacian_zamazenta", soundEvent("battle.galar.swsh_legendary_zacian_zamazenta"), legendaryDexOverrides = setOf(888, 889)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_giants", soundEvent("battle.galar.swsh_legendary_giants"), legendaryDexOverrides = setOf(377, 378, 379, 894, 895)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_birds", soundEvent("battle.galar.swsh_legendary_birds"), legendaryDexOverrides = setOf(144, 145, 146)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_calyrex", soundEvent("battle.galar.swsh_legendary_calyrex"), legendaryDexOverrides = setOf(898)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_swsh_legendary_calyrex_fused", soundEvent("battle.galar.swsh_legendary_calyrex_fused"), legendaryDexOverrides = setOf(898)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("galar_legendary_glastrier_spectrier", soundEvent("battle.galar.legendary_glastrier_spectrier"), legendaryDexOverrides = setOf(896, 897)))

        register(MusicContext.BATTLE_TOWER_BATTLE, MusicTrack("galar_battle_tower", soundEvent("battle.galar.battle_tower"), regions = setOf(RegionOfOrigin.GALAR)))
    }

    private fun registerHisuiBattle() {
        // hisui keeps wild and arceus tracks
        register(MusicContext.WILD_BATTLE, MusicTrack("hisui_wild", soundEvent("battle.hisui.wild"), regions = setOf(RegionOfOrigin.HISUI)))
        // arceus keeps its original dex id
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("hisui_legendary_arceus", soundEvent("battle.hisui.legendary_arceus"), legendaryDexOverrides = setOf(493)))
    }

    private fun registerKalosBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("kalos_wild", soundEvent("battle.kalos.wild"), regions = setOf(RegionOfOrigin.KALOS)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("kalos_trainer", soundEvent("battle.kalos.trainer"), regions = setOf(RegionOfOrigin.KALOS)))
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("kalos_gym_leader", soundEvent("battle.kalos.gym_leader"), regions = setOf(RegionOfOrigin.KALOS)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("kalos_elite_four", soundEvent("battle.kalos.elite_four"), regions = setOf(RegionOfOrigin.KALOS)))

        val champion = soundEvent("battle.kalos.champion")
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("kalos_champion", champion, regions = setOf(RegionOfOrigin.KALOS)))
        register(MusicContext.PVP_BATTLE, MusicTrack("kalos_pvp_champion", champion, regions = setOf(RegionOfOrigin.KALOS)))
        register(MusicContext.PVP_BATTLE, MusicTrack("kalos_rival_friend", soundEvent("battle.kalos.rival_friend"), regions = setOf(RegionOfOrigin.KALOS)))

        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("kalos_legendary_trio", soundEvent("battle.kalos.legendary_trio"), legendaryDexOverrides = setOf(716, 717, 718)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("kalos_xy_legendary_mewtwo", soundEvent("battle.kalos.xy_legendary_mewtwo"), legendaryDexOverrides = setOf(144, 145, 146, 150)))

        register(MusicContext.FACTION_BATTLE, MusicTrack("kalos_team_flare_grunt", soundEvent("battle.kalos.team_flare_grunt"), factionThemes = setOf("team_flare_grunt")))
        register(MusicContext.FACTION_BATTLE, MusicTrack("kalos_team_flare_lysandre", soundEvent("battle.kalos.team_flare_lysandre"), factionThemes = setOf("team_flare_lysandre")))
    }

    private fun registerPaldeaBattle() {
        register(MusicContext.WILD_BATTLE, MusicTrack("paldea_wild", soundEvent("battle.paldea.wild"), regions = setOf(RegionOfOrigin.PALDEA)))
        register(MusicContext.TRAINER_BATTLE, MusicTrack("paldea_trainer", soundEvent("battle.paldea.trainer"), regions = setOf(RegionOfOrigin.PALDEA)))
        register(MusicContext.GYM_LEADER_BATTLE, MusicTrack("paldea_gym_leader", soundEvent("battle.paldea.gym_leader"), regions = setOf(RegionOfOrigin.PALDEA)))
        register(MusicContext.ELITE_FOUR_BATTLE, MusicTrack("paldea_elite_four", soundEvent("battle.paldea.elite_four"), regions = setOf(RegionOfOrigin.PALDEA)))

        val champion = soundEvent("battle.paldea.champion_top")
        register(MusicContext.CHAMPION_BATTLE, MusicTrack("paldea_champion", champion, regions = setOf(RegionOfOrigin.PALDEA)))
        register(MusicContext.PVP_BATTLE, MusicTrack("paldea_pvp_champion", champion, regions = setOf(RegionOfOrigin.PALDEA)))

        // indigo disk duplicates are intentional
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_legendary_solgaleo_lunala_dlc", soundEvent("battle.paldea.legendary_solgaleo_lunala_dlc"), legendaryDexOverrides = setOf(791, 792)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_calamity", soundEvent("battle.paldea.sv_legendary_calamity"), legendaryDexOverrides = setOf(1001, 1002, 1003, 1004)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_koraidon_miraidon", soundEvent("battle.paldea.sv_legendary_koraidon_miraidon"), legendaryDexOverrides = setOf(1007, 1008)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_loyal_three", soundEvent("battle.paldea.sv_legendary_loyal_three"), legendaryDexOverrides = setOf(1014, 1015, 1016)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_ogerpon", soundEvent("battle.paldea.sv_legendary_ogerpon"), legendaryDexOverrides = setOf(1017)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_terapagos", soundEvent("battle.paldea.sv_legendary_terapagos"), legendaryDexOverrides = setOf(1024)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_legendary_terapagos_stellar", soundEvent("battle.paldea.sv_legendary_terapagos_stellar"), legendaryDexOverrides = setOf(1024)))
        register(MusicContext.LEGENDARY_BATTLE, MusicTrack("paldea_sv_mythical_pecharunt", soundEvent("battle.paldea.sv_mythical_pecharunt"), legendaryDexOverrides = setOf(1025)))
    }

    private fun registerVictoryThemes() {
        fun victory(id: String, path: String, vararg keys: String) {
            val track = MusicTrack(id, soundEvent("victory.$path"), loop = true)
            register(MusicContext.VICTORY, track)
            keys.forEach { victoryTracks[it] = track }
        }

        victory("victory_kanto_wild", "frlg_victory", "kanto.wild", "kanto.generic")
        victory("victory_kanto_trainer", "frlg_victory_trainer", "kanto.trainer", "kanto.e4")
        victory("victory_kanto_gym", "frlg_victory_gymleader", "kanto.gym", "kanto.champion")

        victory("victory_johto_wild", "hgss_victory", "johto.wild", "johto.generic")
        victory("victory_johto_trainer", "hgss_victory_trainer", "johto.trainer")
        victory("victory_johto_gym", "hgss_victory_gymleader", "johto.gym", "johto.e4", "johto.champion")

        victory("victory_hoenn_wild", "emerald_victory_wild", "hoenn.wild", "hoenn.generic")
        victory("victory_hoenn_trainer", "emerald_victory_trainer", "hoenn.trainer")
        victory("victory_hoenn_gym", "emerald_victory_gym", "hoenn.gym", "hoenn.e4")
        victory("victory_hoenn_champion", "emerald_victory_champion", "hoenn.champion")
        victory("victory_hoenn_aqua_magma", "emerald_victory_aquamagma", "hoenn.faction")

        victory("victory_sinnoh_wild", "platinum_victory", "sinnoh.wild", "sinnoh.generic")
        victory("victory_sinnoh_trainer", "platinum_victory_trainer", "sinnoh.trainer")
        victory("victory_sinnoh_gym", "platinum_victory_gymleader", "sinnoh.gym")
        victory("victory_sinnoh_e4", "platinum_victory_elitefour", "sinnoh.e4")
        victory("victory_sinnoh_champion", "platinum_victory_champion", "sinnoh.champion")
        victory("victory_sinnoh_galactic", "platinum_victory_teamgalactic", "sinnoh.faction")

        victory("victory_unova_wild", "bw_victory", "unova.wild", "unova.generic")
        victory("victory_unova_trainer", "bw_victory_trainer", "unova.trainer")
        victory("victory_unova_gym", "bw_victory_gymleader", "unova.gym", "unova.e4")
        victory("victory_unova_champion", "bw_victory_champion", "unova.champion")
        victory("victory_unova_plasma", "bw_victory_teamplasma", "unova.faction")

        victory("victory_kalos_wild", "xy_victory_wild", "kalos.wild", "kalos.generic")
        victory("victory_kalos_trainer", "xy_victory_trainer", "kalos.trainer", "kalos.e4", "kalos.champion")
        victory("victory_kalos_gym", "xy_victory_gymleader", "kalos.gym")

        victory("victory_alola_wild", "sm_victory_wild", "alola.wild", "alola.generic")
        victory("victory_alola_trainer", "sm_victory_trainer", "alola.trainer", "alola.gym", "alola.e4", "alola.champion")

        victory("victory_galar_wild", "swsh_victory_wild", "galar.wild", "galar.generic")
        victory("victory_galar_trainer", "swsh_victory_trainer", "galar.trainer", "galar.gym", "galar.e4", "galar.champion")

        victory("victory_paldea_wild", "sv_victory_wild", "paldea.wild", "paldea.generic")
        victory("victory_paldea_trainer", "sv_victory_trainer", "paldea.trainer", "paldea.gym", "paldea.e4", "paldea.champion")
        // hisui has no victory theme
    }

    private fun registerBattleTower() {
        fun tower(zoneId: String, id: String, path: String) {
            val track = MusicTrack(id, soundEvent("tower.$path"), loop = true)
            register(MusicContext.BATTLE_TOWER, track)
            battleTowerTracks.getOrPut(zoneId) { mutableListOf() }.add(track)
        }

        val low = "cobbletunes:battle_tower_low"
        tower(low, "tower_frlg_trainer_tower", "frlg_trainer_tower")
        tower(low, "tower_hgss_reception", "hgss_battle_tower_reception")
        tower(low, "tower_emerald_frontier", "emerald_battle_frontier")
        tower(low, "tower_platinum_frontier", "platinum_battle_frontier")
        tower(low, "tower_b2w2_pwt", "b2w2_pwt")

        val mid = "cobbletunes:battle_tower_mid"
        tower(mid, "tower_emerald_tower", "emerald_battle_tower")
        tower(mid, "tower_emerald_factory", "emerald_battle_factory")
        tower(mid, "tower_hgss_tower", "hgss_battle_tower")
        tower(mid, "tower_hgss_factory", "hgss_battle_factory")
        tower(mid, "tower_hgss_hall", "hgss_battle_hall")
        tower(mid, "tower_platinum_tower", "platinum_battle_tower")
        tower(mid, "tower_platinum_factory", "platinum_battle_factory")
        tower(mid, "tower_b2w2_pwt_arena", "b2w2_pwt_arena")

        val high = "cobbletunes:battle_tower_high"
        tower(high, "tower_emerald_arena", "emerald_battle_arena")
        tower(high, "tower_emerald_dome", "emerald_battle_dome")
        tower(high, "tower_emerald_pike", "emerald_battle_pike")
        tower(high, "tower_emerald_palace", "emerald_battle_palace")
        tower(high, "tower_emerald_pyramid", "emerald_battle_pyramid")
        tower(high, "tower_hgss_arcade", "hgss_battle_arcade")
        tower(high, "tower_hgss_castle", "hgss_battle_castle")
        tower(high, "tower_platinum_arcade", "platinum_battle_arcade")
        tower(high, "tower_platinum_castle", "platinum_battle_castle")
        tower(high, "tower_platinum_hall", "platinum_battle_hall")

        val final = "cobbletunes:battle_tower_final"
        tower(final, "tower_emerald_pyramid_summit", "emerald_battle_pyramid_summit")
        tower(final, "tower_b2w2_pwt_final", "b2w2_pwt_final")
    }


    fun gymAmbienceTrackFor(region: RegionOfOrigin): MusicTrack? {
        val pool = tracks[MusicContext.GYM_AMBIENCE].orEmpty()
        return pool.filter { region in it.regions }.randomOrNull()
            ?: pool.randomOrNull()
    }

    private val structureTracks: MutableMap<String, MusicTrack> = mutableMapOf()
    private val structureTrackPools: MutableMap<String, List<MusicTrack>> = mutableMapOf()

    fun specialStructureTrackFor(structureId: String): MusicTrack? =
        structureTrackPools[structureId]?.randomOrNull() ?: structureTracks[structureId]

    private fun registerSpecialStructure(structureId: String, track: MusicTrack) {
        structureTracks[structureId] = track
    }

    private fun registerSpecialStructurePool(structureId: String, tracks: List<MusicTrack>) {
        structureTrackPools[structureId] = tracks
    }

    private val structureCategoryTracks: MutableMap<String, MutableList<MusicTrack>> = mutableMapOf()

    private fun registerStructureCategoryTrack(category: String, track: MusicTrack) {
        structureCategoryTracks.getOrPut(category) { mutableListOf() }.add(track)
    }

    fun vanillaStructureTrackFor(category: String): MusicTrack? =
        structureCategoryTracks[category]?.randomOrNull()

    fun structureTracksFor(category: String): List<MusicTrack> =
        structureCategoryTracks[category].orEmpty()

    private fun registerProximityAmbience() {
        // zone themes loop until exit
        register(MusicContext.GYM_AMBIENCE, MusicTrack(
            "gym_kanto", soundEvent("ambience.gym.kanto_gym"),
            loop = true, regions = setOf(RegionOfOrigin.KANTO)
        ))
        register(MusicContext.GYM_AMBIENCE, MusicTrack(
            "gym_johto", soundEvent("ambience.gym.johto_gym"),
            loop = true, regions = setOf(RegionOfOrigin.JOHTO)
        ))
        register(MusicContext.GYM_AMBIENCE, MusicTrack(
            "gym_hoenn", soundEvent("ambience.gym.hoenn_gym"),
            loop = true, regions = setOf(RegionOfOrigin.HOENN)
        ))
        register(MusicContext.GYM_AMBIENCE, MusicTrack(
            "gym_sinnoh", soundEvent("ambience.gym.sinnoh_gym"),
            loop = true, regions = setOf(RegionOfOrigin.SINNOH)
        ))
        register(MusicContext.GYM_AMBIENCE, MusicTrack(
            "gym_unova", soundEvent("ambience.gym.unova_gym"),
            loop = true, regions = setOf(RegionOfOrigin.UNOVA)
        ))

        register(MusicContext.POKECENTER, MusicTrack("pokecenter_kanto",  soundEvent("ambience.pokecenter.kanto_center"),  loop = true))
        register(MusicContext.POKECENTER, MusicTrack("pokecenter_johto",  soundEvent("ambience.pokecenter.johto_center"),  loop = true))
        register(MusicContext.POKECENTER, MusicTrack("pokecenter_hoenn",  soundEvent("ambience.pokecenter.hoenn_center"),  loop = true))
        register(MusicContext.POKECENTER, MusicTrack("pokecenter_sinnoh", soundEvent("ambience.pokecenter.sinnoh_center"), loop = true))
        register(MusicContext.POKECENTER, MusicTrack("pokecenter_unova",  soundEvent("ambience.pokecenter.unova_center"),  loop = true))

        register(MusicContext.POKEMART, MusicTrack("pokemart_1", soundEvent("ambience.pokemart.mart1"), loop = true))
        register(MusicContext.POKEMART, MusicTrack("pokemart_2", soundEvent("ambience.pokemart.mart2"), loop = true))
        register(MusicContext.POKEMART, MusicTrack("pokemart_3", soundEvent("ambience.pokemart.mart3"), loop = true))
    }

    private fun registerSpecialStructures() {
        fun ss(structureId: String, trackName: String) = registerSpecialStructure(
            "cobbleverse:$structureId",
            MusicTrack("special_$structureId", soundEvent("ambience.structure.$trackName"), loop = true)
        )

        ss("ash",            "ash")
        ss("crown_cemetery", "crown_cemetery")
        ss("articuno",       "articuno")
        ss("zapdos",         "zapdos")
        ss("moltres",        "moltres")
        ss("mew",            "mew")

        ss("bell_tower",    "bell_tower")
        ss("burned_tower",  "burned_tower")
        ss("celebi_shrine", "celebi_shrine")
        ss("whirl_island",  "whirl_island")

        ss("groudon",       "groudon")
        ss("kyogre",        "kyogre")
        ss("regirock",      "regirock")
        ss("regice",        "regice")
        ss("registeel",     "registeel")
        ss("deoxys",        "deoxys")
        ss("jirachi",       "jirachi")
        ss("secret_garden", "secret_garden")
        ss("sky_pillar",    "sky_pillar")
        ss("dyna_tree",     "dyna_tree")

        ss("spear_pillar",        "spear_pillar")
        ss("snowpoint_temple",    "snowpoint_temple")
        ss("split_decision_temple","split_decision_temple")
        ss("flower_paradise",     "route210")
        ss("fullmoon_island",     "fullmoon_island")
        ss("crescent_isle",       "crescent_isle")
        ss("eterna_building",     "eterna_building")
        ss("wind_plant",          "wild_plant")
        ss("manaphy",             "manaphy")

        registerSpecialStructure(
            "cobbleverse:team_rocket_tower",
            MusicTrack("special_team_rocket_tower", soundEvent("ambience.kanto.nether_rocket_hideout"), loop = true)
        )
        registerSpecialStructure(
            "cobbleverse:rocket_radio_tower",
            MusicTrack("special_rocket_radio_tower", soundEvent("ambience.vanilla.pillager_outpost.radio_tower_takeover"), loop = true)
        )
        registerSpecialStructure(
            "cobbleverse:team_galactic_hq",
            MusicTrack("special_team_galactic_hq", soundEvent("ambience.vanilla.trial_chambers.team_galactic"), loop = true)
        )

        fun lm(structureId: String, trackName: String) = registerSpecialStructure(
            "legendarymonuments:$structureId",
            MusicTrack("legendarymonuments_$structureId", soundEvent(trackName), loop = true)
        )

        lm("distortion_portal", "ambience.sinnoh.end_distortion_world")
        lm("giratina_island",   "ambience.sinnoh.end_distortion_world")
        lm("turnback_cave",     "ambience.sinnoh.end_distortion_world")
        lm("lake_acuity",       "ambience.sinnoh.cave_lake_caverns")
        lm("lake_valor",        "ambience.sinnoh.cave_lake_caverns")
        lm("lake_verity",       "ambience.sinnoh.cave_lake_caverns")
        lm("stark_mountain",    "ambience.vanilla.fortress.stark_mountain")

        registerSpecialStructurePool(
            "cobbletunes:gimmighoul_tower",
            listOf(
                MusicTrack(
                    "cobblemon_gimmighoul_tower_pokemon_tower",
                    soundEvent("ambience.kanto.pokemon_tower"),
                    loop = true
                ),
                MusicTrack(
                    "cobblemon_gimmighoul_tower_lavender_town",
                    soundEvent("ambience.kanto.deep_dark_lavender_town"),
                    loop = true
                ),
                MusicTrack(
                    "cobblemon_gimmighoul_tower_unown_radio",
                    soundEvent("ambience.johto.cave_deep_dark_pokegear_unown"),
                    loop = true
                )
            )
        )

        registerSpecialStructure(
            "cobbletunes:rocket_hideout",
            MusicTrack("trigger_rocket_hideout", soundEvent("ambience.kanto.nether_rocket_hideout"), loop = true)
        )
        registerSpecialStructure(
            "cobbletunes:ruins_of_alph",
            MusicTrack("trigger_ruins_of_alph", soundEvent("ambience.johto.stronghold_ruins_of_alph"), loop = true)
        )
        registerSpecialStructure(
            "cobbletunes:aqua_magma_hideout",
            MusicTrack("trigger_aqua_magma_hideout", soundEvent("ambience.hoenn.nether_hideout"), loop = true)
        )
        registerSpecialStructure(
            "cobbletunes:underground_ruins",
            MusicTrack("trigger_underground_ruins", soundEvent("ambience.unova.stronghold_underground_ruins"), loop = true)
        )
    }

    fun caveAmbienceTrack(excludeTrackId: String? = null): MusicTrack? {
        val caveBiomes = setOf(
            "minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:deep_dark",
            "terralith:andesite_caves", "terralith:crystal_caves", "terralith:deep_caves",
            "terralith:diorite_caves", "terralith:frostfire_caves", "terralith:fungal_caves",
            "terralith:granite_caves", "terralith:ice_caves", "terralith:infested_caves",
            "terralith:mantle_caves", "terralith:thermal_caves", "terralith:tuff_caves",
            "terralith:underground_jungle", "terralith:desert_caves", "terralith:glacial_chasm"
        )
        val pool = tracks[MusicContext.AMBIENCE].orEmpty()
            .filter { track -> track.biomeKeys.any { it in caveBiomes } }
        val alternatives = excludeTrackId
            ?.let { excluded -> pool.filter { it.id != excluded } }
            .orEmpty()
        return (if (alternatives.isNotEmpty()) alternatives else pool).randomOrNull()
    }

    // low hp sfx skips music fades
    private val LOW_HP_SOUND_EVENT: net.minecraft.sound.SoundEvent by lazy {
        net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of(MOD_ID, "effect.lowhp"))
    }

    fun lowHpSoundEvent(): net.minecraft.sound.SoundEvent = LOW_HP_SOUND_EVENT

    private fun registerVanillaStructures() {
        fun vs(category: String, trackName: String) = registerStructureCategoryTrack(
            category,
            MusicTrack("vanilla_${category}_$trackName", soundEvent("ambience.vanilla.$category.$trackName"), loop = true)
        )

        vs("ancient_city", "cerulean_cave")
        vs("ancient_city", "distortion_world")
        vs("ancient_city", "dragonspiral_tower")
        vs("ancient_city", "mt_silver")
        vs("ancient_city", "pokemon_mansion")

        vs("bastion_remnant", "hideout")
        vs("bastion_remnant", "reversal_mountain")

        vs("bca_village_large", "celadon_city")
        vs("bca_village_large", "cerulean_city")
        vs("bca_village_large", "cinnabar")
        vs("bca_village_large", "fallarbor")
        vs("bca_village_large", "rustboro")
        vs("bca_village_large", "verdanturf")
        vs("bca_village_large", "violet")

        vs("bca_village_mid", "cerulean_city")
        vs("bca_village_mid", "cinnabar")
        vs("bca_village_mid", "fallarbor")
        vs("bca_village_mid", "rustboro")
        vs("bca_village_mid", "verdanturf")
        vs("bca_village_mid", "violet")
        vs("bca_village_mid", "celadon_city")

        vs("bca_village_small", "rustboro")
        vs("bca_village_small", "verdanturf")
        vs("bca_village_small", "violet")
        vs("bca_village_small", "celadon_city")
        vs("bca_village_small", "cerulean_city")
        vs("bca_village_small", "cinnabar")
        vs("bca_village_small", "fallarbor")

        vs("buried_treasure", "undella_town")
        vs("buried_treasure", "lacunosa_town")
        vs("buried_treasure", "route109")

        vs("desert_pyramid", "relic_castle")
        vs("desert_pyramid", "desert_ruins")
        vs("desert_pyramid", "mirage_tower")

        vs("end_city", "hall_of_origin")
        vs("end_city", "mt_chimney")
        vs("end_city", "n_castle")
        vs("end_city", "spear_pillar")

        vs("fortress", "rocket_hq")
        vs("fortress", "stark_mountain")
        vs("fortress", "hideout")
        vs("fortress", "mt_ember")

        vs("igloo", "shoal_cave")
        vs("igloo", "cold_storage")
        vs("igloo", "ice_path")

        vs("jungle_pyramid", "ilex_forest")
        vs("jungle_pyramid", "island_cave")
        vs("jungle_pyramid", "pinwheel_forest")
        vs("jungle_pyramid", "safari_zone")

        vs("mansion", "poke_mansion")
        vs("mansion", "strange_house")
        vs("mansion", "old_chateau")

        vs("mineshaft", "mt_moon")
        vs("mineshaft", "oreburgh_mine")
        vs("mineshaft", "union_cave")

        vs("mineshaft_mesa", "chargestone_cave")
        vs("mineshaft_mesa", "reversal_mountain")
        vs("mineshaft_mesa", "stark_mountain")

        vs("monument", "cave_origin")
        vs("monument", "whirl_islands")

        vs("nether_fossil", "lavender_town")
        vs("nether_fossil", "mt_pyre")
        vs("nether_fossil", "poke_tower")
        vs("nether_fossil", "strange_house")

        vs("ocean_ruin", "dive_theme")
        vs("ocean_ruin", "marine_tube")
        vs("ocean_ruin", "underwater")

        vs("pillager_outpost", "radio_tower_takeover")
        vs("pillager_outpost", "rocket_hideout")
        vs("pillager_outpost", "team_aqua_magma_hideout")

        vs("ruined_portal", "distorn_world")
        vs("ruined_portal", "radio_signal")

        vs("shipwreck", "plasma_frigate")
        vs("shipwreck", "ss_aqua")
        vs("shipwreck", "abandoned_ship")

        vs("stronghold", "spear_pillar")
        vs("stronghold", "dragonpsiral")
        vs("stronghold", "mt_coronet")

        vs("swamp_hut", "losrtlorn_forest")
        vs("swamp_hut", "ecruteak_city")
        vs("swamp_hut", "great_marsh")

        vs("trail_ruins", "sevault_canyon")
        vs("trail_ruins", "solaceon_ruins")
        vs("trail_ruins", "abyssal_ruins")
        vs("trail_ruins", "ruins_of_alph")

        vs("trial_chambers", "teamplasma_hq")
        vs("trial_chambers", "victory_road")
        vs("trial_chambers", "silph_co")
        vs("trial_chambers", "team_galactic")

        vs("village_desert", "rustboro_city")
        vs("village_desert", "fallarbor_town")
        vs("village_desert", "lentimas_town")
        vs("village_desert", "route_111")

        vs("village_plains", "pewter_city")
        vs("village_plains", "rustboro_city")
        vs("village_plains", "twinleaf_town")
        vs("village_plains", "cherrygrove_city")
        vs("village_plains", "littleroot_town")
        vs("village_plains", "newbark_town")
        vs("village_plains", "nuvema_town")
        vs("village_plains", "pallet_town")

        vs("village_savanna", "verdantuf_town")
        vs("village_savanna", "azalea_town")
        vs("village_savanna", "floccesy_town")
        vs("village_savanna", "goldenrod_town")

        vs("village_snowy", "snowpoint_city")
        vs("village_snowy", "icirrus_city")
        vs("village_snowy", "mahogany_town")

        vs("village_taiga", "fortree_city")
        vs("village_taiga", "canalave_city")
        vs("village_taiga", "celestic_town")
        vs("village_taiga", "eterna_city")
    }
}
