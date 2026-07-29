package com.kaizzinho.cobbletunes.client.sound

/**
 * The set of situations CobbleTunes cares about. Pillar 8's classifier model:
 * a battle start fires a single classification pass that lands on exactly one
 * of these — no layered priority/stack, since Cobblemon never fires a second
 * BATTLE_STARTED while one is already active, so only one battle context is
 * ever "live" at a time. Ambience is just what plays when no battle context is.
 *
 * ELITE_FOUR_BATTLE added because Hoenn and Sinnoh both have genuinely distinct
 * E4 themes (unlike Kanto/Johto, which just reuse GYM_LEADER_BATTLE) — every
 * region registers something into this pool, either its own unique track or a
 * duplicate of its gym leader track, so the pool is never empty regardless of
 * which pattern that region follows.
 *
 * GYM_LEADER_BATTLE / ELITE_FOUR_BATTLE / CHAMPION_BATTLE are now correctly
 * routed via Pillar 4's RCT tier detection — TrainerMobData.getType().id()
 * maps "leader"/"e4"/"champ" to these contexts at battle start.
 *
 * PVP_BATTLE is routed by Pillar 6 — battles that are neither isWild nor
 * isTrainer land here, plus RCT rivals (type "rival") by Pillar 4.
 *
 * Pillar 9 — GYM_AMBIENCE / POKECENTER / POKEMART replace AMBIENCE while the
 * player is inside a detected structure or trigger-block zone. GYM_AMBIENCE
 * resolves by RegionOfOrigin so each region's gym plays its own theme.
 * POKECENTER and POKEMART are flat pools — no region filtering, matching the
 * games where these themes are shared across all regions.
 */
enum class MusicContext {
    AMBIENCE,
    WILD_BATTLE,
    TRAINER_BATTLE,
    GYM_LEADER_BATTLE,
    ELITE_FOUR_BATTLE,
    CHAMPION_BATTLE,
    PVP_BATTLE,
    LEGENDARY_BATTLE,
    GYM_AMBIENCE,
    POKECENTER,
    POKEMART,
    // Each special structure has exactly one dedicated track registered against
    // its structure ID — no pool/random-pick, just a direct 1:1 lookup via
    // TrackRegistry.specialStructureTrackFor(structureId). Covers legendary
    // shrines, villain team bases, and notable named buildings across all four
    // regions currently in the Cobbleverse datapacks.
    SPECIAL_STRUCTURE,
    // Pillar 10: title screen music. Independent of everything else — no
    // biome, no zone, no battle state. Just flat-pool random pick, loops
    // while the title screen is open, stops the instant a world loads.
    MENU
}
