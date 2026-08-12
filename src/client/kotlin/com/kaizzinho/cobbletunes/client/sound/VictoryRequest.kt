package com.kaizzinho.cobbletunes.client.sound

enum class VictoryKind {
    WILD,
    TRAINER,
    GYM_LEADER,
    ELITE_FOUR,
    CHAMPION,
    RIVAL,
    FACTION,
    FRONTIER_BRAIN
}

data class VictoryRequest(
    val region: RegionOfOrigin,
    val kind: VictoryKind,
    val factionTheme: String? = null
)
