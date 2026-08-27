package com.kaizzinho.cobbletunes.compat.rct

object RctTrainerOverrides {
    val exact: Map<String, TrainerOverride> = mapOf(
        "pokemon_trainer_barry" to TrainerOverride(
            TrainerRole.RIVAL, "sinnoh", battlePoolId = "barry"
        ),
        "pokemon_trainer_gold" to TrainerOverride(
            TrainerRole.NORMAL, "johto", battlePoolId = "gold"
        ),
        "pokemon_trainer_green" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battlePoolId = "green"
        ),
        "pokemon_trainer_kris" to TrainerOverride(
            TrainerRole.NORMAL, "johto", battlePoolId = "kris"
        ),
        "pokemon_trainer_may" to TrainerOverride(
            TrainerRole.NORMAL, "hoenn", battlePoolId = "may"
        ),
        "pokemon_trainer_morimoto" to TrainerOverride(
            TrainerRole.NORMAL, "unova", battlePoolId = "morimoto"
        ),
        "pokemon_trainer_oak" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battlePoolId = "oak"
        ),
        "pokemon_trainer_red" to TrainerOverride(
            TrainerRole.NORMAL, "kanto", battlePoolId = "red"
        ),
        "pokemon_trainer_silver" to TrainerOverride(
            TrainerRole.RIVAL, "johto", battlePoolId = "silver"
        ),
        "pokemon_trainer_steven" to TrainerOverride(
            TrainerRole.CHAMPION, "hoenn", battlePoolId = "steven"
        )
    )
}
