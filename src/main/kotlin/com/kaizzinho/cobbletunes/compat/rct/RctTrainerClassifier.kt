package com.kaizzinho.cobbletunes.compat.rct

import java.util.Locale

enum class TrainerRole(val routeId: String) {
    NORMAL("normal"),
    GYM_LEADER("leader"),
    ELITE_FOUR("e4"),
    CHAMPION("champ"),
    RIVAL("rival")
}

enum class TrainerDetectionSource {
    EXACT_OVERRIDE,
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
    val region: String? = null
)

data class TrainerClassification(
    val role: TrainerRole,
    val region: String?,
    val trainerId: String,
    val rawType: String,
    val optional: Boolean,
    val source: TrainerDetectionSource
) {
    fun route(): String {
        if (role == TrainerRole.NORMAL && region == null) return ""
        return region?.let { "${role.routeId}|$it" } ?: role.routeId
    }
}

object RctTrainerClassifier {
    private val regions = setOf(
        "kanto", "johto", "hoenn", "sinnoh", "unova",
        "kalos", "alola", "galar", "hisui", "paldea"
    )

    fun classify(
        raw: RawRctTrainer,
        exactOverrides: Map<String, TrainerOverride> = emptyMap()
    ): TrainerClassification {
        val trainerId = normalize(raw.trainerId)
        val typeId = normalize(raw.typeId)
        val override = exactOverrides[trainerId]

        // datapack-specific fixes get first dibs
        if (override != null) {
            return result(
                raw = raw,
                role = override.role,
                region = normalizeRegion(override.region) ?: regionFrom(typeId) ?: regionFrom(trainerId),
                source = TrainerDetectionSource.EXACT_OVERRIDE
            )
        }

        val typeRegion = regionFrom(typeId)
        val idRegion = regionFrom(trainerId)
        val region = typeRegion ?: idRegion

        // native rct roles are the safest signal
        roleFromStandardType(typeId)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.RCT_TYPE)
        }

        // cobbleverse also uses stuff like kanto_league / kanto_champion
        roleFromCustomType(typeId, typeRegion)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.CUSTOM_TYPE)
        }

        // some hoenn/sinnoh e4 files only expose the role in their id
        roleFromTrainerId(trainerId, idRegion)?.let { role ->
            return result(raw, role, region, TrainerDetectionSource.TRAINER_ID)
        }

        // current cobbleverse gyms are regional, non-optional progression trainers
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

    private fun roleFromStandardType(typeId: String): TrainerRole? = when (typeId) {
        "leader", "gym_leader", "gymleader" -> TrainerRole.GYM_LEADER
        "e4", "elite_four", "elitefour", "elite_4" -> TrainerRole.ELITE_FOUR
        "champ", "champion" -> TrainerRole.CHAMPION
        "rival" -> TrainerRole.RIVAL
        else -> null
    }

    private fun roleFromCustomType(typeId: String, region: String?): TrainerRole? {
        if (region == null) return null
        return when (typeId) {
            "${region}_leader", "${region}_gym_leader" -> TrainerRole.GYM_LEADER
            "${region}_league", "${region}_e4", "${region}_elite_four" -> TrainerRole.ELITE_FOUR
            "${region}_champ", "${region}_champion" -> TrainerRole.CHAMPION
            "${region}_rival" -> TrainerRole.RIVAL
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

        if (
            trainerId.startsWith("rival_") ||
            trainerId.contains("_rival_")
        ) {
            return TrainerRole.RIVAL
        }

        return null
    }

    private fun result(
        raw: RawRctTrainer,
        role: TrainerRole,
        region: String?,
        source: TrainerDetectionSource
    ) = TrainerClassification(
        role = role,
        region = region,
        trainerId = raw.trainerId,
        rawType = raw.typeId,
        optional = raw.optional,
        source = source
    )

    private fun regionFrom(value: String): String? =
        regions.firstOrNull { value == it || value.startsWith("${it}_") }

    private fun normalizeRegion(value: String?): String? =
        value?.let(::normalize)?.takeIf { it in regions }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
