package com.kaizzinho.cobbletunes.compat.rct

object RctTrainerOverrides {
    val exact: Map<String, TrainerOverride> = mapOf(
        "pokemon_trainer_barry" to TrainerOverride(
            TrainerRole.RIVAL, "sinnoh", battleTrackId = "sinnoh_rival_pvp"
        ),
        "pokemon_trainer_gold" to TrainerOverride(
            TrainerRole.NORMAL, "johto", battleTrackId = "unova_pvp_champion_johto"
        ),
        "pokemon_trainer_green" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battleTrackId = "unova_pvp_champion_kanto"
        ),
        "pokemon_trainer_kris" to TrainerOverride(
            TrainerRole.NORMAL, "johto", battleTrackId = "unova_pvp_champion_johto"
        ),
        "pokemon_trainer_may" to TrainerOverride(
            TrainerRole.NORMAL, "hoenn", battleTrackId = "unova_pvp_champion_hoenn"
        ),
        "pokemon_trainer_morimoto" to TrainerOverride(
            TrainerRole.NORMAL, "unova", battleTrackId = "tower_b2w2_pwt_final"
        ),
        "pokemon_trainer_oak" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battleTrackId = "unova_pvp_champion_kanto"
        ),
        "pokemon_trainer_red" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battleTrackId = "unova_pvp_champion_kanto"
        ),
        "pokemon_trainer_silver" to TrainerOverride(
            TrainerRole.RIVAL, "johto", battleTrackId = "johto_rival_pvp"
        ),
        "pokemon_trainer_steven" to TrainerOverride(
            TrainerRole.CHAMPION, "hoenn", battleTrackId = "hoenn_champion_wallace"
        )
    )
}
