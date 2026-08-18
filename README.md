# CobbleTunes — Music Framework 🎵🎮

![Version](https://img.shields.io/badge/version-1.0-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-Loader%200.17.2%2B-DBB69B?logo=minecraft&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Fabric%20Language%20Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.3-3E8E41)
![License](https://img.shields.io/badge/license-MIT-blue)
![Audio](https://img.shields.io/badge/audio-not%20included-lightgrey)

*Read this in [English](#english) | Leia em [Português](#português)*

---

## English

### Overview

**CobbleTunes** is a dynamic music framework for Cobblemon on Fabric. It replaces Minecraft music with context-aware battle themes, regional ambience, structure music, Victory themes, Battle Tower music, Game Corner zones, title-screen tracks, and low-HP cues.

The mod ships the routing and playback system only. It does **not** include, download, or generate Pokémon OST files. Music is supplied by a normal Minecraft resource pack under `assets/cobbletunes/sounds/`.

The current source defines **419 sound events** across battle, Victory, Battle Tower, Game Corner, ambience, menu, and effects. The complete list is in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### What it does

Battle classification runs on the server and playback runs on the client. CobbleTunes inspects the opponent, resolves a role and region, sends a compact route, then chooses the matching registered `SoundEvent`.

```text
Cobblemon battle
→ classify wild trainer pvp boss faction or facility battle
→ resolve role and region
→ apply species form and regional variant overrides
→ send route to the client
→ play the matching music context
```

World music uses a similar priority model:

```text
battle
↓
victory and loot menu
↓
structure trigger and battle tower zone
↓
biome ambience
```

Lower-priority world detection keeps running while battle or Victory music owns the audio slot, so the current zone or biome is ready when the higher-priority music ends.

### Key Features

- [x] **Regional battle music** for Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, and Paldea.
- [x] **Wild, trainer, Gym Leader, Elite Four, Champion, rival, PvP, faction, Frontier Brain, Battle Tower, and Legendary/Mythical contexts.**
- [x] **Regional form routing** so Alolan, Galarian, Hisuian, and Paldean forms use their form region instead of the base species region.
- [x] **Form-aware Legendary routing** for encounters such as Kyurem, Necrozma, Eternatus, Calyrex, Terapagos, and the Galarian birds.
- [x] **Dynamic RCT classification** with role, region, faction, rank, trainer ID, and progression-aware routing.
- [x] **Villain faction themes** for Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine, and Ultra Recon Squad routes.
- [x] **Frontier Brain music** with a dedicated battle context.
- [x] **WildBosses integration** with tier-weighted regional PvP, generic Legendary, and BW World Tournament pools.
- [x] **Species-safe Boss pools** that keep unique Legendary encounter themes out of unrelated Boss fights.
- [x] **Victory + Cobblemon Loot Menu integration** with instant Victory start, fast battle-to-Victory fade, menu confirmation grace, looping while the loot screen is open, and current-zone resume afterward.
- [x] **Battle Tower floor pools** with low, mid, high, and final tiers plus dedicated Battle Tower battle music.
- [x] **Biome ambience memory and rotation** with silence windows and biome-transition debounce.
- [x] **Underground ambience detection** using sky light and player height.
- [x] **Cobbleverse exact structures**, **vanilla structures**, **BCA village pools**, and **Repurposed Structures** reuse mappings.
- [x] **Hand-placed music zones** for Poké Centers, Poké Marts, Gyms, Game Corners/Casinos, special locations, and Battle Tower floors.
- [x] **Game Corner pool** with 15 FRLG, Emerald, HGSS, and Platinum tracks played as a shuffled no-repeat playlist.
- [x] **Title-screen music** that stays active across submenus and stops when a world loads.
- [x] **Low-HP cue** for the active battle Pokémon with a boosted effect volume.
- [x] **Player-death handling** and safe world/zone reset.
- [x] **Client and server debug logging**, disabled by default.
- [x] **Mod Menu integration** with a native CobbleTunes config screen for client music settings.

### Requirements

#### Required

- Minecraft `1.21.1`
- Fabric Loader `0.17.2+`
- Fabric API
- Fabric Language Kotlin `1.13.3+`
- Cobblemon `1.7.3`
- Java `21`

For multiplayer, install CobbleTunes on both the client and server. The server performs battle and structure classification while the client owns playback.

#### Optional integrations

- **Mod Menu** — opens a native CobbleTunes configuration screen for client music settings.
- **Radical Cobblemon Trainers** — trainer role, region, faction, and progression-aware battle routing.
- **WildBosses** — Boss-specific weighted regional battle pools.
- **Cobblemon Loot Menu** — post-battle Victory music while the loot screen is active.
- **CobblemonAdditions / BCA structures** — additional village-size structure pools when those structures exist in the world.
- **Repurposed Structures** — reuses the existing vanilla/BCA music pools for all 107 worldgen structure IDs present in `7.5.21+1.21.1`.

Mod Menu, RCT, WildBosses, Cobblemon Loot Menu, and Repurposed Structures are soft integrations. Missing optional mods do not prevent CobbleTunes from loading.

### Battle routing

#### Standard regional contexts

CobbleTunes keeps the battle role separate from the region. A Kanto Gym Leader route looks like:

```text
leader|kanto
```

A Sinnoh Frontier Brain route looks like:

```text
frontier|sinnoh
```

A faction route keeps the exact theme ID:

```text
faction:team_galactic_commander|sinnoh
```

When trainer metadata contains a region, that region wins. If it does not, the opposing roster votes by region.

#### Regional variants

National Dex numbers alone are not enough for regional forms. CobbleTunes also sends regional-form metadata with the battle packet.

Examples:

```text
alolan exeggutor → alola
alolan grimer and muk → alola
galarian weezing → galar
hisuian growlithe → hisui
paldean wooper → paldea
```

The regional variant affects wild themes, trainer roster voting, WildBosses regional pools, Legendary fallback routing, and Victory region selection.

#### RCT trainer roles and factions

RCT integration is reflection-based. CobbleTunes reads the trainer ID and type when available, then classifies the encounter without making RCT a hard dependency.

Recognized role routes include:

```text
normal
leader
e4
champ
rival
frontier
```

Recognized faction families include Team Rocket, Team Aqua, Team Magma, Team Galactic, Team Plasma, Team Flare, Team Skull, Aether Foundation, and Ultra Recon Squad.

Faction matching runs before broad role matching. This keeps cases such as Rocket Giovanni separate from a normal regional Gym Leader Giovanni route.

### Legendary and Mythical routing

Dedicated encounter tracks are selected by species and, where needed, form. Generic regional Legendary tracks remain available as fallbacks.

Current form-sensitive cases include:

- Galarian Articuno, Zapdos, and Moltres;
- base vs Black/White Kyurem;
- base, fused, and Ultra Necrozma;
- Eternatus vs Eternamax Eternatus;
- Calyrex vs Ice Rider / Shadow Rider Calyrex;
- Terapagos vs Stellar Terapagos.

The resource pack also contains dedicated encounter themes for major Legendary/Mythical groups across the supported regions. See the manifest for every mapped file.

### WildBosses integration

WildBosses is optional. When a battle is identified as an actual Boss encounter, CobbleTunes uses the Boss species region and tier to build a weighted pool.

| Tier | Regional rival/PvP | Generic Legendary | BW World Tournament |
|---|---:|---:|---:|
| Uncommon | 80% | 15% | 5% |
| Rare | 70% | 20% | 10% |
| Epic | 60% | 30% | 10% |
| Legendary | 50% | 35% | 15% |
| Mythic | 45% | 40% | 15% |

Species-specific Legendary themes are excluded from generic Boss pools. If another valid option exists, CobbleTunes also avoids immediately repeating the previous Boss track for that region.

### Victory and Cobblemon Loot Menu

Victory music is a soft integration with `cobblemon_loot_menu`.

When a supported battle is won:

```text
battle victory
→ Victory track starts immediately
→ battle fades out in about 0.25 s
→ Victory fades in in about 0.20 s
→ CobbleTunes waits up to 3 s for the loot screen to appear
```

The three-second window does **not** delay the music. Victory is already playing during that time. If the Loot Menu opens, Victory keeps looping until the screen closes. If no loot screen appears, Victory ends and world music resumes.

Biome and structure detection continue while Victory owns playback. When the loot screen closes, CobbleTunes restores the **latest** valid structure, Battle Tower floor, or biome instead of returning to stale pre-battle ambience.

Hisui intentionally has no traditional Victory theme in this pack.

### Battle Tower

Battle Tower ambience uses four pools:

```text
battle_tower_low
battle_tower_mid
battle_tower_high
battle_tower_final
```

Floor trigger IDs such as `cobbletunes:battle_tower_floor_1` through `cobbletunes:battle_tower_floor_10` resolve into those pools. Nearby floor triggers are checked nearest-first so vertically overlapping floors do not steal each other's music.

A trainer battle that begins while the current zone is `BATTLE_TOWER` is routed to the dedicated Galar Battle Tower battle theme.

### Game Corner and Casino zones

A `MusicTriggerBlock` can turn a custom build into a Game Corner or Casino. Use either of these zone IDs:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Entering the zone starts a shuffled Game Corner playlist. The first track is random, and when it finishes CobbleTunes automatically advances to another track without looping the same file. Every track is played once before the pool reshuffles, and the reshuffle avoids immediately repeating the track that just finished. Battle and Victory music still have priority; when they end, the active Game Corner playlist continues while the zone remains active.

The pool has 15 expected files under `assets/cobbletunes/sounds/gamecorner/`, grouped across FRLG, Emerald, HGSS, and Platinum. The resource pack audio is still supplied separately; the source only registers the events and routing. See [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) for the exact filenames and source-theme suggestions.

### Ambience and structures

Regional biome ambience is currently defined for Kanto, Johto, Hoenn, Sinnoh, and Unova. Tracks are mapped to vanilla and Terralith biome groups such as plains, forests, caves, oceans, mountains, snow, deserts, and volcanic areas.

Biome tracks use memory, rotation budgets, silence ranges, and transition debounce. Crossing a tiny biome does not immediately force a new track if the biome changes again during the debounce window.

Structure music has higher priority than biome ambience. Supported sources include:

- Cobbleverse Gym and League structures;
- exact named Cobbleverse structures and Legendary locations;
- vanilla structures such as Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals, and more;
- BCA small, mid, and large village pools;
- Repurposed Structures variants mapped back into the closest existing vanilla/BCA pool;
- hand-placed `MusicTriggerBlock` zones.

Most fixed zone music loops until the player leaves the zone. Game Corner and Casino zones are the exception and advance through their shuffled 15-track playlist instead. Battle and Victory music temporarily take priority without discarding the current zone state. Repurposed Structures needs no new soundtrack files because its structures alias existing pools such as Ancient City, Fortress, Mansion, Mineshaft, Monument, Outpost, Pyramid, Village, and Witch Hut music.

### Low HP cue

The active battle Pokémon is checked periodically. When a living active Pokémon reaches 25% HP or lower, CobbleTunes plays a two-beep alert sequence.

The effect uses `1.35x` the configured music volume, capped at `1.5`, so it remains audible over battle music.

### Configuration

CobbleTunes creates two JSON files in `config/`. When Mod Menu is installed, the Configure button opens a native CobbleTunes screen for all client-side options below. The server debug option remains in the server JSON.

#### `cobbletunes-client.json`

| Option | Default |
|---|---:|
| `replaceAmbience` | `true` |
| `replaceMenuMusic` | `true` |
| `replaceBattleMusic` | `true` |
| `musicVolume` | `1.0` |
| `crossfadeSeconds` | `2.5` |
| `shuffleAmbienceTracks` | `true` |
| `worldJoinSilenceSeconds` | `10` |
| `trackEndSilenceMinSeconds` | `90` |
| `trackEndSilenceMaxSeconds` | `180` |
| `biomeTransitionSilenceMinSeconds` | `4` |
| `biomeTransitionSilenceMaxSeconds` | `8` |
| `debugLogging` | `false` |

#### `cobbletunes-server.json`

| Option | Default |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

CobbleTunes does not include soundtrack files. Put your `.ogg` files under:

```text
assets/cobbletunes/sounds/
```

The included `sounds.json` defines all 419 expected sound events. [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) mirrors those entries and explains their routing.

Missing audio is handled as silence instead of crashing the music system. The new `gamecorner/` entries are intentionally empty slots until the matching OGG files are added to the resource pack.

### Project layout

- **`src/main/kotlin`** — common/server entrypoint, battle events, RCT classification, WildBosses bridge, structure detection including Repurposed Structures aliases, trigger blocks, configs, and networking.
- **`src/client/kotlin`** — packet routing, region selection, Victory/Loot Menu bridge, ambience watching, music state, fades, menu music, low-HP handling, and the optional Mod Menu config screen.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — all sound keys and resource-pack paths.

### Building

Java 21 is required. From the complete project root:

```powershell
.\gradlew clean build
```

If your project uses the audio-exclusion build property:

```powershell
.\gradlew clean build -PexcludeAudio
```

The built JAR is placed in `build/libs/`.

### Audio and licensing

No Pokémon OST audio is distributed with CobbleTunes. Soundtrack files belong to their respective rights holders and must be supplied separately by the resource-pack user or pack maintainer.

### License

CobbleTunes source code is available under the MIT license.

---

## Português

### Visão geral

**CobbleTunes** é um framework de música dinâmica para Cobblemon em Fabric. Ele substitui a música do Minecraft por temas de batalha, ambientação regional, músicas de estruturas, temas de vitória, Battle Tower, Game Corner, menu e alerta de HP baixo de acordo com o contexto atual.

O mod contém apenas a lógica de roteamento e reprodução. Ele **não** inclui, baixa ou gera arquivos de OST de Pokémon. As músicas são fornecidas por um resource pack normal dentro de `assets/cobbletunes/sounds/`.

O código atual define **419 eventos de som** entre batalhas, vitória, Battle Tower, Game Corner, ambientação, menu e efeitos. A lista completa está em [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### O que ele faz

A classificação das batalhas acontece no servidor e a reprodução acontece no cliente. O CobbleTunes analisa o adversário, resolve função e região, envia uma rota compacta e escolhe o `SoundEvent` correspondente.

```text
batalha do Cobblemon
→ classifica selvagem treinador pvp boss facção ou facility
→ resolve função e região
→ aplica forma e variante regional
→ envia a rota ao cliente
→ toca o contexto musical correspondente
```

A prioridade da música no mundo funciona assim:

```text
batalha
↓
vitória e loot menu
↓
estrutura trigger e battle tower
↓
ambientação de bioma
```

As detecções de prioridade menor continuam atualizando em segundo plano enquanto batalha ou vitória controlam o áudio. Assim o CobbleTunes já sabe qual zona ou bioma deve voltar quando a música prioritária termina.

### Principais recursos

- [x] **Música regional de batalha** para Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui e Paldea.
- [x] **Contextos de selvagem, treinador, Líder de Ginásio, Elite Four, Campeão, rival, PvP, facção, Frontier Brain, Battle Tower e Lendário/Mítico.**
- [x] **Roteamento de formas regionais** para que formas de Alola, Galar, Hisui e Paldea usem a região da forma em vez da região da espécie base.
- [x] **Roteamento de lendários por forma** para casos como Kyurem, Necrozma, Eternatus, Calyrex, Terapagos e as aves de Galar.
- [x] **Classificação dinâmica do RCT** usando função, região, facção, rank, ID do treinador e progressão.
- [x] **Temas de facções** para Rocket, Aqua, Magma, Galactic, Plasma, Flare, Skull, Aether Foundation, Lusamine e Ultra Recon Squad.
- [x] **Música de Frontier Brain** com contexto próprio.
- [x] **Integração com WildBosses** usando pools regionais ponderados por tier.
- [x] **Pools seguros para Bosses** sem usar temas lendários específicos em encontros aleatórios.
- [x] **Integração de vitória com Cobblemon Loot Menu** com início imediato, fade rápido, janela de confirmação, loop durante o menu e retorno para a zona atual.
- [x] **Pools de Battle Tower** para andares baixos, médios, altos e finais com tema de batalha dedicado.
- [x] **Memória e rotação de ambientação por bioma** com intervalos de silêncio e debounce.
- [x] **Detecção subterrânea** usando luz do céu e altura do jogador.
- [x] **Estruturas exatas do Cobbleverse**, **estruturas vanilla**, **pools de vilas BCA** e reaproveitamento para **Repurposed Structures**.
- [x] **Zonas manuais de música** para Centros Pokémon, Poké Marts, Ginásios, Game Corners/Cassinos, locais especiais e andares da Battle Tower.
- [x] **Pool de Game Corner** com 15 faixas de FRLG, Emerald, HGSS e Platinum tocadas como uma playlist embaralhada sem repetição imediata.
- [x] **Música de menu** contínua entre os submenus da tela inicial.
- [x] **Alerta de HP baixo** com volume reforçado.
- [x] **Tratamento de morte do jogador** e limpeza segura de estado do mundo.
- [x] **Logs de debug no cliente e servidor**, desligados por padrão.
- [x] **Integração com Mod Menu** com tela nativa do CobbleTunes para as configurações de música do cliente.

### Requisitos

#### Obrigatórios

- Minecraft `1.21.1`
- Fabric Loader `0.17.2+`
- Fabric API
- Fabric Language Kotlin `1.13.3+`
- Cobblemon `1.7.3`
- Java `21`

Em multiplayer, instale o CobbleTunes no cliente e no servidor. O servidor classifica batalhas e estruturas enquanto o cliente controla a reprodução.

#### Integrações opcionais

- **Mod Menu** — abre uma tela nativa do CobbleTunes para as configurações de música do cliente.
- **Radical Cobblemon Trainers** — melhora a detecção de função, região, facção e progressão.
- **WildBosses** — ativa pools musicais próprios para Bosses.
- **Cobblemon Loot Menu** — ativa temas de vitória enquanto a tela de loot está aberta.
- **Repurposed Structures** — reaproveita os pools vanilla/BCA existentes para todos os 107 IDs de estruturas de worldgen presentes na versão `7.5.21+1.21.1`.
- **CobblemonAdditions / estruturas BCA** — adiciona pools para tamanhos de vila quando essas estruturas existem no mundo.

Mod Menu, RCT, WildBosses, Cobblemon Loot Menu e Repurposed Structures são integrações leves. A ausência desses mods não impede o CobbleTunes de carregar.

### Roteamento de batalha

#### Contextos regionais

O CobbleTunes mantém a função separada da região. Um Líder de Ginásio de Kanto usa:

```text
leader|kanto
```

Um Frontier Brain de Sinnoh usa:

```text
frontier|sinnoh
```

Uma facção mantém o ID exato do tema:

```text
faction:team_galactic_commander|sinnoh
```

Quando os metadados do treinador informam uma região, ela tem prioridade. Caso contrário, a equipe adversária vota pela região.

#### Variantes regionais

O número da Pokédex Nacional não é suficiente para formas regionais. O pacote de batalha também envia os dados da variante regional.

Exemplos:

```text
exeggutor de alola → alola
grimer e muk de alola → alola
weezing de galar → galar
growlithe de hisui → hisui
wooper de paldea → paldea
```

A variante regional afeta temas selvagens, votação da equipe de treinadores, pools do WildBosses, fallback de lendários e seleção da região da vitória.

#### Funções e facções do RCT

A integração com RCT usa reflexão. O CobbleTunes lê ID e tipo do treinador quando disponíveis sem transformar o RCT em dependência obrigatória.

Rotas reconhecidas incluem:

```text
normal
leader
e4
champ
rival
frontier
```

As famílias de facção reconhecidas incluem Team Rocket, Team Aqua, Team Magma, Team Galactic, Team Plasma, Team Flare, Team Skull, Aether Foundation e Ultra Recon Squad.

A detecção de facção acontece antes das regras amplas de função. Isso mantém Giovanni da Rocket separado de uma rota normal de Líder de Ginásio regional.

### Lendários e míticos

Temas dedicados são escolhidos por espécie e por forma quando necessário. Temas lendários regionais genéricos continuam disponíveis como fallback.

Os casos sensíveis à forma incluem:

- Articuno, Zapdos e Moltres de Galar;
- Kyurem base e Black/White Kyurem;
- Necrozma base, fundido e Ultra Necrozma;
- Eternatus e Eternamax Eternatus;
- Calyrex e suas formas Ice Rider / Shadow Rider;
- Terapagos e Stellar Terapagos.

O resource pack também possui temas dedicados para vários grupos lendários e míticos das regiões suportadas. O manifest contém o mapeamento completo.

### Integração com WildBosses

O WildBosses é opcional. Quando uma batalha é realmente identificada como Boss, o CobbleTunes usa a região da espécie e o tier para montar um pool ponderado.

| Tier | Rival/PvP regional | Lendário genérico | BW World Tournament |
|---|---:|---:|---:|
| Uncommon | 80% | 15% | 5% |
| Rare | 70% | 20% | 10% |
| Epic | 60% | 30% | 10% |
| Legendary | 50% | 35% | 15% |
| Mythic | 45% | 40% | 15% |

Temas de encontros lendários específicos ficam fora dos pools genéricos de Boss. Quando existe outra opção válida, o mod também evita repetir imediatamente a última música de Boss usada naquela região.

### Vitória e Cobblemon Loot Menu

A música de vitória é uma integração leve com `cobblemon_loot_menu`.

Quando uma batalha suportada é vencida:

```text
vitória da batalha
→ música de vitória começa na hora
→ batalha sai em cerca de 0.25 s
→ vitória entra em cerca de 0.20 s
→ CobbleTunes espera até 3 s pela tela de loot
```

Os três segundos **não** atrasam a música. O tema de vitória já está tocando durante esse período. Se o Loot Menu abrir, a música continua em loop até a tela fechar. Se nenhuma tela aparecer, o tema termina e a música do mundo volta.

A detecção de bioma e estrutura continua funcionando durante a vitória. Quando o menu fecha, o CobbleTunes volta para a **última** estrutura, andar da Battle Tower ou bioma válido em vez de usar uma ambientação antiga salva antes da batalha.

Hisui não possui tema tradicional de vitória neste pack.

### Battle Tower

A ambientação da Battle Tower usa quatro pools:

```text
battle_tower_low
battle_tower_mid
battle_tower_high
battle_tower_final
```

IDs de trigger como `cobbletunes:battle_tower_floor_1` até `cobbletunes:battle_tower_floor_10` são convertidos nesses pools. Triggers próximos são verificados do mais próximo para o mais distante para evitar conflito vertical entre andares.

Uma batalha de treinador iniciada enquanto a zona atual é `BATTLE_TOWER` usa o tema de batalha da Battle Tower de Galar.

### Game Corner e zonas de Cassino

Um `MusicTriggerBlock` pode transformar uma construção própria em Game Corner ou Cassino. Use um destes IDs de zona:

```text
cobbletunes:game_corner
cobbletunes:casino
```

Ao entrar na zona o mod inicia uma playlist embaralhada do Game Corner. A primeira faixa é aleatória e quando ela termina o CobbleTunes avança automaticamente para outra sem manter o mesmo arquivo em loop. Todas as faixas passam uma vez antes do pool ser embaralhado novamente e o novo ciclo evita repetir imediatamente a música que acabou de tocar. Batalha e vitória continuam com prioridade e depois delas a playlist do Game Corner continua enquanto a zona permanecer ativa.

O pool possui 15 arquivos esperados dentro de `assets/cobbletunes/sounds/gamecorner/`, divididos entre FRLG, Emerald, HGSS e Platinum. O áudio continua sendo fornecido separadamente pelo resource pack. Veja [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) para os nomes exatos e as sugestões de temas de origem.

### Ambientação e estruturas

A ambientação regional por bioma está definida para Kanto, Johto, Hoenn, Sinnoh e Unova. As faixas são agrupadas entre biomas vanilla e Terralith como planícies, florestas, cavernas, oceanos, montanhas, neve, desertos e regiões vulcânicas.

As músicas de bioma usam memória, orçamento de rotação, intervalos de silêncio e debounce. Cruzar um bioma muito pequeno não força imediatamente uma nova música caso o bioma mude novamente durante a janela de debounce.

Música de estrutura tem prioridade sobre ambientação de bioma. As fontes suportadas incluem:

- Ginásios e Ligas do Cobbleverse;
- estruturas exatas e locais lendários do Cobbleverse;
- estruturas vanilla como Ancient Cities, Strongholds, Mansions, Trial Chambers, Villages, Shipwrecks, Ruined Portals e outras;
- pools de vilas BCA pequenas, médias e grandes;
- variantes do Repurposed Structures redirecionadas para o pool vanilla/BCA mais próximo;
- zonas manuais com `MusicTriggerBlock`.

A maioria das músicas fixas de zona fica em loop até o jogador sair. Game Corner e Cassino são a exceção e avançam pela playlist embaralhada de 15 faixas. Batalha e vitória assumem temporariamente o áudio sem apagar o estado atual da zona. Repurposed Structures não precisa de novas músicas porque suas estruturas reutilizam pools já existentes como Ancient City, Fortress, Mansion, Mineshaft, Monument, Outpost, Pyramid, Village e Witch Hut.

### Alerta de HP baixo

O Pokémon ativo é verificado periodicamente durante a batalha. Quando um Pokémon ativo e vivo chega a 25% de HP ou menos, o CobbleTunes toca uma sequência de dois alertas.

O efeito usa `1.35x` o volume configurado para música, limitado a `1.5`, para continuar audível durante a batalha.

### Configuração

O CobbleTunes cria dois JSONs dentro de `config/`. Quando o Mod Menu está instalado, o botão Configure abre uma tela nativa do CobbleTunes com todas as opções do cliente abaixo. O debug do servidor continua no JSON do servidor.

#### `cobbletunes-client.json`

| Opção | Padrão |
|---|---:|
| `replaceAmbience` | `true` |
| `replaceMenuMusic` | `true` |
| `replaceBattleMusic` | `true` |
| `musicVolume` | `1.0` |
| `crossfadeSeconds` | `2.5` |
| `shuffleAmbienceTracks` | `true` |
| `worldJoinSilenceSeconds` | `10` |
| `trackEndSilenceMinSeconds` | `90` |
| `trackEndSilenceMaxSeconds` | `180` |
| `biomeTransitionSilenceMinSeconds` | `4` |
| `biomeTransitionSilenceMaxSeconds` | `8` |
| `debugLogging` | `false` |

#### `cobbletunes-server.json`

| Opção | Padrão |
|---|---:|
| `debugLogging` | `false` |

### Resource pack

O CobbleTunes não inclui arquivos de soundtrack. Coloque os `.ogg` dentro de:

```text
assets/cobbletunes/sounds/
```

O `sounds.json` incluído define todos os 419 eventos esperados. [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) espelha essas entradas e explica o roteamento.

Áudio ausente vira silêncio sem derrubar o sistema de música. As novas entradas de `gamecorner/` ficam como espaços vazios até os OGGs correspondentes serem adicionados ao resource pack.

### Organização do projeto

- **`src/main/kotlin`** — inicialização comum/servidor, eventos de batalha, classificação do RCT, ponte com WildBosses, estruturas incluindo aliases do Repurposed Structures, trigger blocks, configs e rede.
- **`src/client/kotlin`** — roteamento dos pacotes, região, ponte de Victory/Loot Menu, observação de biomas, estado musical, fades, menu, HP baixo e a tela opcional de configuração do Mod Menu.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — todas as chaves e caminhos do resource pack.

### Compilação

Java 21 é obrigatório. Na raiz do projeto completo:

```powershell
.\gradlew clean build
```

Se o projeto estiver usando a opção de exclusão de áudio:

```powershell
.\gradlew clean build -PexcludeAudio
```

O JAR compilado fica em `build/libs/`.

### Áudio e licença

Nenhuma OST de Pokémon é distribuída com o CobbleTunes. As faixas pertencem aos respectivos detentores de direitos e devem ser fornecidas separadamente pelo usuário ou mantenedor do resource pack.

### Licença

O código-fonte do CobbleTunes está disponível sob a licença MIT.
