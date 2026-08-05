package com.kaizzinho.cobbletunes.client.sound

enum class MusicContext {
    AMBIENCE,
    WILD_BATTLE,
    TRAINER_BATTLE,
    GYM_LEADER_BATTLE,
    ELITE_FOUR_BATTLE,
    CHAMPION_BATTLE,
    PVP_BATTLE,
    FACTION_BATTLE,
    LEGENDARY_BATTLE,
    GYM_AMBIENCE,
    POKECENTER,
    POKEMART,
    // one fixed track per named cobbleverse structure
    SPECIAL_STRUCTURE,
    // title/menu pool, totally separate from world state
    MENU,
    // vanilla/bca structures pick from a pool per category
    VANILLA_STRUCTURE
}
