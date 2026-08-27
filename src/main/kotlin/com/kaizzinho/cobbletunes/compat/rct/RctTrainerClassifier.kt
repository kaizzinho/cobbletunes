package com.kaizzinho.cobbletunes.compat.rct

import java.util.Locale

enum class TrainerRole(val routeId: String) {
    NORMAL("normal"),
    GYM_LEADER("leader"),
    ELITE_FOUR("e4"),
    CHAMPION("champ"),
    RIVAL("rival"),
    FRONTIER_BRAIN("frontier"),
    FACTION_GRUNT("faction_grunt"),
    FACTION_ADMIN("faction_admin"),
    FACTION_BOSS("faction_boss")
}

enum class TrainerFaction(val id: String, val homeRegion: String?) {
    TEAM_ROCKET("team_rocket", null),
    TEAM_AQUA("team_aqua", "hoenn"),
    TEAM_MAGMA("team_magma", "hoenn"),
    TEAM_GALACTIC("team_galactic", "sinnoh"),
    TEAM_PLASMA("team_plasma", "unova"),
    TEAM_FLARE("team_flare", "kalos"),
    TEAM_SKULL("team_skull", "alola"),
    AETHER_FOUNDATION("aether_foundation", "alola"),
    ULTRA_RECON_SQUAD("ultra_recon_squad", "alola")
}

enum class FactionRank {
    GRUNT,
    ADMIN,
    BOSS
}

enum class TrainerDetectionSource {
    EXACT_OVERRIDE,
    FACTION_TYPE,
    FACTION_ID,
    RCT_TYPE,
    CUSTOM_TYPE,
    TRAINER_ID,
    REGIONAL_PROGRESSION,
    ORDINARY
}

data class RawRctTrainer(
    val trainerId: String,
    val typeId: String,
    val optional: Boolean
)

data class TrainerOverride(
    val role: TrainerRole,
    val region: String? = null,
    val faction: TrainerFaction? = null,
    val factionRank: FactionRank? = null,
    val factionTheme: String? = null,
    val battleTrackId: String? = null,
    val battlePoolId: String? = null
)

data class TrainerClassification(
    val role: TrainerRole,
    val region: String?,
    val trainerId: String,
    val rawType: String,
    val optional: Boolean,
    val source: TrainerDetectionSource,
    val faction: TrainerFaction? = null,
    val factionRank: FactionRank? = null,
    val factionTheme: String? = null,
    val battleTrackId: String? = null,
    val battlePoolId: String? = null
) {
    fun route(): String {
        battlePoolId?.takeIf { it.isNotBlank() }?.let { poolId ->
            val route = "specialpool:$poolId:${role.routeId}"
            return region?.let { "$route|$it" } ?: route
        }

        battleTrackId?.takeIf { it.isNotBlank() }?.let { trackId ->
            val route = "special:$trackId:${role.routeId}"
            return region?.let { "$route|$it" } ?: route
        }

        factionTheme?.let { theme ->
            val route = "faction:$theme"
            return region?.let { "$route|$it" } ?: route
        }

        if (role == TrainerRole.NORMAL && region == null) return ""
        return region?.let { "${role.routeId}|$it" } ?: role.routeId
    }
}

object RctTrainerClassifier {
    private val regions = setOf(
        "kanto", "johto", "hoenn", "sinnoh", "unova",
        "kalos", "alola", "galar", "hisui", "paldea"
    )

    private data class FactionMatch(
        val faction: TrainerFaction,
        val rank: FactionRank,
        val theme: String,
        val source: TrainerDetectionSource
    )

    fun classify(
        raw: RawRctTrainer,
        exactOverrides: Map<String, TrainerOverride> = emptyMap()
    ): TrainerClassification {
        val trainerId = normalize(raw.trainerId)
        val typeId = normalize(raw.typeId)

        // Repetitive Battle Tower floor trainers intentionally use the ordinary
        // trainer route so the opposing roster votes for the regional theme.
        if (isGenericBattleTowerFloorTrainer(trainerId)) {
            return result(
                raw = raw,
                role = TrainerRole.NORMAL,
                region = null,
                source = TrainerDetectionSource.ORDINARY
            )
        }

        val override = exactOverrides[trainerId]
            ?: trainerId.removePrefix("rctmod_").takeIf { it != trainerId }?.let(exactOverrides::get)

        if (override != null) {
            return result(
                raw = raw,
                role = override.role,
                region = normalizeRegion(override.region) ?: regionFrom(typeId) ?: regionFrom(trainerId),
                source = TrainerDetectionSource.EXACT_OVERRIDE,
                faction = override.faction,
                factionRank = override.factionRank,
                factionTheme = override.factionTheme,
                battleTrackId = override.battleTrackId,
                battlePoolId = override.battlePoolId
            )
        }

        val typeRegion = regionFrom(typeId)
        val idRegion = regionFrom(trainerId)
        val region = typeRegion ?: idRegion

        // facility bosses beat broad role rules
        if (isFrontierBrain(trainerId, typeId)) {
            return result(
                raw,
                TrainerRole.FRONTIER_BRAIN,
                region ?: "sinnoh",
                TrainerDetectionSource.TRAINER_ID
            )
        }

        // factions beat generic role matching
        factionMatch(trainerId, typeId)?.let { match ->
            return result(
                raw = raw,
                role = roleFor(match.rank),
                region = region ?: match.faction.homeRegion,
                source = match.source,
                faction = match.faction,
                factionRank = match.rank,
                factionTheme = match.theme
            )
        }

        roleFromStandardType(typeId)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.RCT_TYPE)
        }

        roleFromCustomType(typeId, typeRegion)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.CUSTOM_TYPE)
        }

        roleFromTrainerId(trainerId, idRegion)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.TRAINER_ID)
        }

        val isRegionalProgressionTrainer =
            typeRegion != null &&
                typeId == typeRegion &&
                idRegion == typeRegion &&
                !raw.optional

        if (isRegionalProgressionTrainer) {
            return result(
                raw,
                TrainerRole.GYM_LEADER,
                region,
                TrainerDetectionSource.REGIONAL_PROGRESSION
            )
        }

        return result(raw, TrainerRole.NORMAL, region, TrainerDetectionSource.ORDINARY)
    }

    private fun isFrontierBrain(trainerId: String, typeId: String): Boolean =
        typeId in setOf("frontier", "frontier_brain", "battle_frontier", "facility_brain") ||
            typeId.endsWith("_frontier_brain") ||
            trainerId.startsWith("frontier_brain_") ||
            trainerId.contains("_frontier_brain_") ||
            trainerId.endsWith("_frontier_brain") ||
            trainerId.startsWith("battle_frontier_brain_")

    private fun factionMatch(trainerId: String, typeId: String): FactionMatch? {
        val fromType = factionFromType(typeId)
        val faction = fromType ?: factionFromTrainerId(trainerId) ?: return null
        val source = if (fromType != null) {
            TrainerDetectionSource.FACTION_TYPE
        } else {
            TrainerDetectionSource.FACTION_ID
        }

        return when (faction) {
            TrainerFaction.TEAM_ROCKET -> classifyRocket(trainerId, source)
            TrainerFaction.TEAM_AQUA -> classifyAquaMagma(trainerId, faction, source)
            TrainerFaction.TEAM_MAGMA -> classifyAquaMagma(trainerId, faction, source)
            TrainerFaction.TEAM_GALACTIC -> classifyGalactic(trainerId, source)
            TrainerFaction.TEAM_PLASMA -> classifyPlasma(trainerId, source)
            TrainerFaction.TEAM_FLARE -> classifyFlare(trainerId, source)
            TrainerFaction.TEAM_SKULL -> classifySkull(trainerId, source)
            TrainerFaction.AETHER_FOUNDATION -> classifyAether(trainerId, source)
            TrainerFaction.ULTRA_RECON_SQUAD -> FactionMatch(
                TrainerFaction.ULTRA_RECON_SQUAD,
                FactionRank.ADMIN,
                "ultra_recon_squad",
                source
            )
        }
    }

    private fun classifyRocket(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val rank = when {
            trainerId.contains("giovanni") ||
                trainerId.startsWith("boss_") ||
                trainerId.startsWith("leader_giovanni") -> FactionRank.BOSS

            trainerId.contains("admin") ||
                containsAny(
                    trainerId,
                    "archer", "ariana", "apollo", "atena",
                    "proton", "petrel", "executive", "general", "officer"
                ) -> FactionRank.ADMIN

            else -> FactionRank.GRUNT
        }

        return FactionMatch(
            faction = TrainerFaction.TEAM_ROCKET,
            rank = rank,
            theme = "team_rocket",
            source = source
        )
    }

    private fun classifyAquaMagma(
        trainerId: String,
        faction: TrainerFaction,
        source: TrainerDetectionSource
    ): FactionMatch {
        val bossNames = when (faction) {
            TrainerFaction.TEAM_AQUA -> arrayOf("archie", "ivan")
            TrainerFaction.TEAM_MAGMA -> arrayOf("maxie", "max")
            else -> emptyArray()
        }
        val adminNames = when (faction) {
            TrainerFaction.TEAM_AQUA -> arrayOf("shelly", "matt")
            TrainerFaction.TEAM_MAGMA -> arrayOf("courtney", "tabitha")
            else -> emptyArray()
        }

        val rank = when {
            containsAny(trainerId, *bossNames) ||
                trainerId.contains("boss") ||
                trainerId.contains("leader") -> FactionRank.BOSS

            containsAny(trainerId, *adminNames) ||
                trainerId.contains("admin") ||
                trainerId.contains("commander") ||
                trainerId.contains("executive") -> FactionRank.ADMIN

            else -> FactionRank.GRUNT
        }

        val theme = if (rank == FactionRank.GRUNT) {
            "team_aqua_magma_grunt"
        } else {
            "team_aqua_magma_leaders"
        }

        return FactionMatch(faction, rank, theme, source)
    }

    private fun classifyGalactic(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val rank = when {
            trainerId.contains("cyrus") || trainerId.contains("boss") -> FactionRank.BOSS
            trainerId.contains("commander") ||
                containsAny(trainerId, "mars", "jupiter", "saturn", "charon") -> FactionRank.ADMIN
            else -> FactionRank.GRUNT
        }

        val theme = when (rank) {
            FactionRank.GRUNT -> "team_galactic_grunt"
            FactionRank.ADMIN -> "team_galactic_commander"
            FactionRank.BOSS -> "team_galactic_boss"
        }

        return FactionMatch(TrainerFaction.TEAM_GALACTIC, rank, theme, source)
    }

    private fun classifyPlasma(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val tokens = trainerId.split('_').filter { it.isNotBlank() }
        val isN = "n" in tokens
        val isColress = containsAny(trainerId, "colress", "achroma")
        val isBoss = isN || isColress || containsAny(trainerId, "ghetsis", "boss", "king")
        val isAdmin = containsAny(
            trainerId,
            "zinzolin", "sage", "shadow_triad", "admin", "commander", "executive"
        )

        val rank = when {
            isBoss -> FactionRank.BOSS
            isAdmin -> FactionRank.ADMIN
            else -> FactionRank.GRUNT
        }

        val theme = when {
            isN -> "team_plasma_n"
            isColress -> "team_plasma_colress"
            rank != FactionRank.GRUNT -> "team_plasma_colress"
            else -> "team_plasma_grunt"
        }

        return FactionMatch(TrainerFaction.TEAM_PLASMA, rank, theme, source)
    }

    private fun classifyFlare(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val isLysandre = trainerId.contains("lysandre")
        val rank = when {
            isLysandre || trainerId.contains("boss") || trainerId.contains("leader") -> FactionRank.BOSS
            containsAny(trainerId, "admin", "scientist", "aliana", "bryony", "celosia", "mable", "xerosic") -> FactionRank.ADMIN
            else -> FactionRank.GRUNT
        }
        val theme = if (isLysandre || rank == FactionRank.BOSS) {
            "team_flare_lysandre"
        } else {
            "team_flare_grunt"
        }
        return FactionMatch(TrainerFaction.TEAM_FLARE, rank, theme, source)
    }

    private fun classifySkull(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val rank = when {
            containsAny(trainerId, "guzma", "boss", "leader") -> FactionRank.BOSS
            containsAny(trainerId, "plumeria", "admin") -> FactionRank.ADMIN
            else -> FactionRank.GRUNT
        }
        // skull and guzma share one theme
        return FactionMatch(TrainerFaction.TEAM_SKULL, rank, "team_skull_guzma", source)
    }

    private fun classifyAether(
        trainerId: String,
        source: TrainerDetectionSource
    ): FactionMatch {
        val isLusamine = trainerId.contains("lusamine")
        val rank = when {
            isLusamine || trainerId.contains("president") || trainerId.contains("boss") -> FactionRank.BOSS
            containsAny(trainerId, "faba", "admin", "branch_chief") -> FactionRank.ADMIN
            else -> FactionRank.GRUNT
        }
        val theme = if (isLusamine || rank == FactionRank.BOSS) {
            "aether_president_lusamine"
        } else {
            "aether_foundation"
        }
        return FactionMatch(TrainerFaction.AETHER_FOUNDATION, rank, theme, source)
    }

    private fun factionFromType(typeId: String): TrainerFaction? = when {
        typeId == "team_rocket" || typeId.startsWith("team_rocket_") -> TrainerFaction.TEAM_ROCKET
        typeId == "team_aqua" || typeId.startsWith("team_aqua_") -> TrainerFaction.TEAM_AQUA
        typeId == "team_magma" || typeId.startsWith("team_magma_") -> TrainerFaction.TEAM_MAGMA
        typeId == "team_galactic" || typeId.startsWith("team_galactic_") -> TrainerFaction.TEAM_GALACTIC
        typeId == "team_plasma" || typeId.startsWith("team_plasma_") -> TrainerFaction.TEAM_PLASMA
        typeId == "team_flare" || typeId.startsWith("team_flare_") -> TrainerFaction.TEAM_FLARE
        typeId == "team_skull" || typeId.startsWith("team_skull_") -> TrainerFaction.TEAM_SKULL
        typeId == "aether" || typeId == "aether_foundation" || typeId.startsWith("aether_") -> TrainerFaction.AETHER_FOUNDATION
        typeId == "ultra_recon_squad" || typeId.startsWith("ultra_recon_") -> TrainerFaction.ULTRA_RECON_SQUAD
        else -> null
    }

    private fun factionFromTrainerId(trainerId: String): TrainerFaction? = when {
        trainerId.startsWith("team_rocket_") ||
            trainerId.startsWith("rocket_") ||
            trainerId.startsWith("boss_giovanni_") ||
            trainerId.startsWith("leader_giovanni_") -> TrainerFaction.TEAM_ROCKET

        trainerId.startsWith("team_aqua_") || trainerId.startsWith("aqua_") -> TrainerFaction.TEAM_AQUA
        trainerId.startsWith("team_magma_") || trainerId.startsWith("magma_") -> TrainerFaction.TEAM_MAGMA

        trainerId.startsWith("team_galactic_") ||
            trainerId.startsWith("galactic_") ||
            trainerId.startsWith("commander_mars_") ||
            trainerId.startsWith("commander_jupiter_") ||
            trainerId.startsWith("commander_saturn_") -> TrainerFaction.TEAM_GALACTIC

        trainerId.startsWith("team_plasma_") || trainerId.startsWith("plasma_") -> TrainerFaction.TEAM_PLASMA
        trainerId.startsWith("team_flare_") || trainerId.startsWith("flare_") -> TrainerFaction.TEAM_FLARE
        trainerId.startsWith("team_skull_") || trainerId.startsWith("skull_") || trainerId.startsWith("guzma_") -> TrainerFaction.TEAM_SKULL
        trainerId.startsWith("aether_") || trainerId.startsWith("lusamine_") -> TrainerFaction.AETHER_FOUNDATION
        trainerId.startsWith("ultra_recon_") || trainerId.startsWith("ultra_recon_squad_") -> TrainerFaction.ULTRA_RECON_SQUAD
        else -> null
    }

    private fun roleFor(rank: FactionRank): TrainerRole = when (rank) {
        FactionRank.GRUNT -> TrainerRole.FACTION_GRUNT
        FactionRank.ADMIN -> TrainerRole.FACTION_ADMIN
        FactionRank.BOSS -> TrainerRole.FACTION_BOSS
    }

    private fun roleFromStandardType(typeId: String): TrainerRole? = when (typeId) {
        "leader", "gym_leader", "gymleader" -> TrainerRole.GYM_LEADER
        "e4", "elite_four", "elitefour", "elite_4" -> TrainerRole.ELITE_FOUR
        "champ", "champion" -> TrainerRole.CHAMPION
        "rival" -> TrainerRole.RIVAL
        "frontier", "frontier_brain", "battle_frontier", "facility_brain" -> TrainerRole.FRONTIER_BRAIN
        else -> null
    }

    private fun roleFromCustomType(typeId: String, region: String?): TrainerRole? {
        if (region == null) return null
        return when (typeId) {
            "${region}_leader", "${region}_gym_leader" -> TrainerRole.GYM_LEADER
            "${region}_league", "${region}_e4", "${region}_elite_four" -> TrainerRole.ELITE_FOUR
            "${region}_champ", "${region}_champion" -> TrainerRole.CHAMPION
            "${region}_rival" -> TrainerRole.RIVAL
            "${region}_frontier", "${region}_frontier_brain" -> TrainerRole.FRONTIER_BRAIN
            else -> null
        }
    }

    private fun roleFromTrainerId(trainerId: String, region: String?): TrainerRole? {
        if (
            trainerId.contains("gym_leader") ||
            trainerId.startsWith("leader_") ||
            trainerId.contains("_leader_")
        ) {
            return TrainerRole.GYM_LEADER
        }

        if (
            trainerId.startsWith("e4_") ||
            trainerId.contains("_e4_") ||
            trainerId.contains("elite_four") ||
            trainerId.contains("elite4") ||
            (region != null && trainerId.startsWith("${region}_league_"))
        ) {
            return TrainerRole.ELITE_FOUR
        }

        if (
            trainerId.startsWith("champ_") ||
            trainerId.contains("_champ_") ||
            trainerId.contains("champion") ||
            (region != null && trainerId.startsWith("${region}_champion_"))
        ) {
            return TrainerRole.CHAMPION
        }

        if (trainerId.startsWith("rival_") || trainerId.contains("_rival_")) {
            return TrainerRole.RIVAL
        }

        return null
    }

    private fun result(
        raw: RawRctTrainer,
        role: TrainerRole,
        region: String?,
        source: TrainerDetectionSource,
        faction: TrainerFaction? = null,
        factionRank: FactionRank? = null,
        factionTheme: String? = null,
        battleTrackId: String? = null,
        battlePoolId: String? = null
    ) = TrainerClassification(
        role = role,
        region = region,
        trainerId = raw.trainerId,
        rawType = raw.typeId,
        optional = raw.optional,
        source = source,
        faction = faction,
        factionRank = factionRank,
        factionTheme = factionTheme,
        battleTrackId = battleTrackId,
        battlePoolId = battlePoolId
    )

    private fun regionFrom(value: String): String? =
        regions.firstOrNull { value == it || value.startsWith("${it}_") }

    private fun isGenericBattleTowerFloorTrainer(trainerId: String): Boolean {
        val localId = trainerId.removePrefix("rctmod_")
        return Regex("^f(?:10|[1-9])_trainer\\d+$").matches(localId)
    }

    private fun normalizeRegion(value: String?): String? =
        value?.let(::normalize)?.takeIf { it in regions }

    private fun containsAny(value: String, vararg parts: String): Boolean =
        parts.any { it.isNotBlank() && value.contains(it) }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
