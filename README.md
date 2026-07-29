# CobbleTunes

<details>
<summary>🇺🇸 <b>Click here for the English version</b></summary>

## English
**Author:** kaizzinho  
**Package:** `com.kaizzinho.cobbletunes`  
**Platform:** Fabric, Minecraft 1.21.1  
**Dependencies:** Cobblemon 1.7.3+, Fabric API, Fabric Language Kotlin  
**Soft dependencies:** Radical Cobblemon Trainers (RCT) : Enhances trainer tier detection if present, fully optional  

## What it does
CobbleTunes is a Cobblemon addon that replaces all of Minecraft's ambient and battle music with original Pokémon OST tracks. Music is fully context-aware, the mod classifies what the player is doing and where they are, then selects the appropriate regional or situation-specific theme automatically. It supports both singleplayer and dedicated servers via a client/server packet architecture.

## Architecture
The mod is split across two source sets:

* **Server-side (main/kotlin)** : `CobblemonBattleListener` subscribes to Cobblemon's real battle events, classifies each battle (wild/trainer/legendary/PvP, plus RCT tier if available), and sends typed network payloads to the relevant players. `StructureZoneDetector` runs a slow server tick that detects which Cobbleverse worldgen structure or `MusicTriggerBlock` the player is near, and sends zone-change packets when it changes.
* **Client-side (client/kotlin)** : `CobbleTunesClient` receives all server packets and routes them to `ClientMusicPlayer`. `ClientMusicPlayer` owns all playback state: it drives `FadingSoundInstance` for fade-in/fade-out crossfades, suppresses vanilla's `MusicTracker`, manages per-biome ambience memory and rotation, and handles all silence gating logic. `TrackRegistry` bootstraps every registered track on client init.

## Music systems

### Battle music
Every battle resolves to exactly one `MusicContext` at the moment it starts. The classifier runs server-side and sends raw facts to the client, which applies its own config on top:

* **Wild battles** : Resolved by the opposing Pokémon's National Dex number → RegionOfOrigin → regional wild theme. Every region from Kanto to Paldea (including Hisui as its own entry) has a dedicated track.
* **Legendary/mythical Encounters** : wild-encounter-only (matching the mainline games). Species-specific overrides take priority over regional defaults; shared multi-species tracks (Regis, weather trio, Tapus, etc.) are supported. A trainer's legendary ace plays the normal trainer theme, never the legendary theme.
* **Trainer battles** : Full opposing roster sent server-side; client majority-votes the team's region and picks that region's trainer theme. Ties re-roll randomly each battle.
* **Gym Leader / Elite Four / Champion** : Detected via RCT's `TrainerMobData.getType().id()` soft dependency. Routes to dedicated `GYM_LEADER_BATTLE`, `ELITE_FOUR_BATTLE`, and `CHAMPION_BATTLE` contexts, each with per-region tracks. Degrades gracefully to `TRAINER_BATTLE` if RCT is absent.
* **PvP and rivals** : RCT rivals and real player-vs-player battles both route to a shared `PVP_BATTLE` flat pool (rival and champion themes from all regions).

### Ambience
Biome-driven, region-blind. A client-tick watcher (throttled to ~1s) reads the player's current biome ID and feeds it to `updateAmbienceBiome()`. Key behaviours:

* **Per-biome memory** : Re-entering a biome resumes the same track that was playing before, no re-roll.
* **Real-length rotation** : Tracks play non-looping; rotation is triggered by `SoundManager.isPlaying()` returning false (the track's actual end), not an artificial timer.
* **Silence gating** : Three distinct silence behaviours: world-join/dimension-change (fixed 10s, ignores biome changes during the wait); track-end (random 1:30–3:00, ignores biome changes); biome-transition debounce (random 4–8s, resets on every biome flicker, cuts audio immediately — prevents thin biomes like rivers from ever starting a theme the player has already walked past).
* **Unmapped biomes** : Fully transparent; the previous biome's track continues until the player settles somewhere with registered coverage.
* **Battle interaction** : Battle music takes over instantly, ambience resumes instantly on battle end (no silence gap on battle-end by design).
* All timers respect `MinecraftClient.isPaused()` : No music starts during the pause menu.

### Structure / proximity music
A server-side tick (~5 seconds) detects two types of zones in priority order:

* **MusicTriggerBlock (higher priority)** : A custom block with a block entity carrying a `zoneId` string. Place one inside any hand-built structure (Poké Center, Poké Mart). Scanned within a 24-block radius. Place with `/setblock ~ ~ ~ cobbletunes:music_trigger{ZoneId:"cobbletunes:pokecenter"}`.
* **Worldgen structures** : All Cobbleverse gym and special structures detected via `ServerWorld.locateStructure()` within a 5-chunk radius. No manual block placement required.

Three proximity `MusicContext` values:

* **GYM_AMBIENCE** : One track per region (Kanto→Sinnoh), resolved by which gym structure the player is near.
* **POKECENTER** : Flat pool of 5 regional center themes, random pick.
* **POKEMART** : Flat pool of 3 mart themes, random pick.
* **SPECIAL_STRUCTURE** : 29 dedicated 1:1 tracks for legendary shrines, villain bases, and notable named locations across all four regions (Articuno/Zapdos/Moltres caves, Ash's house, Crown Cemetery, Bell Tower, Burned Tower, Celebi Shrine, Whirl Island, Sky Pillar, Regi caves, Deoxys, Jirachi, Spear Pillar, Snowpoint Temple, lake trio temples, Eterna Building, and more).

Zone music plays immediately on detection, resumes normal biome ambience immediately on leaving (no silence gap on zone-exit by design).

## Config (cobbletunes-client.json)
Generated on first run at `.minecraft/config/cobbletunes-client.json`. All values are per-client:

| Key | Default | Description |
| :--- | :--- | :--- |
| `replaceAmbience` | `true` | Enable biome/structure ambience replacement |
| `replaceBattleMusic` | `true` | Enable battle music replacement |
| `musicVolume` | `1.0` | Master volume for all CobbleTunes tracks |
| `crossfadeSeconds` | `2.5` | Fade-in and fade-out duration for all transitions |
| `shuffleAmbienceTracks` | `true` | Random pick vs. first-registered for ambience pools |
| `worldJoinSilenceSeconds` | `10.0` | Silence after joining a world or changing dimension |
| `trackEndSilenceMinSeconds` | `90.0` | Minimum silence after a track finishes naturally |
| `trackEndSilenceMaxSeconds` | `180.0` | Maximum silence after a track finishes naturally |
| `biomeTransitionSilenceMinSeconds` | `4.0` | Minimum debounce window on biome change |
| `biomeTransitionSilenceMaxSeconds` | `8.0` | Maximum debounce window on biome change |

## Sound asset layout
assets/cobbletunes/sounds/  
  ambience/  
    <biome_tracks_per_region>/  ← wild ambience, tagged by biome key sets  
    gym/                        ← one per region: kanto_gym, johto_gym, etc.  
    pokecenter/                 ← five regional center themes  
    pokemart/                   ← three mart themes  
    structure/                  ← 29 dedicated legendary/special structure themes  
  battle/  
    wild/                       ← per-region wild battle themes  
    trainer/                    ← per-region trainer themes  
    gym_leader/                 ← per-region gym leader themes  
    elite_four/                 ← per-region E4 themes  
    champion/                   ← per-region champion themes  
    pvp/                        ← rival + champion tracks for the PvP pool  
    legendary/                  ← species overrides + regional defaults  

## Covered regions
* **All battle contexts:** Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, Paldea
* **Ambience:** Kanto, Johto, Hoenn, Sinnoh, Unova (by design, no ambience gathered for post-Unova regions)
* **Gym/structure proximity:** Kanto, Johto, Hoenn, Sinnoh (all regions currently in the Cobbleverse datapacks)

## Known limitations / future work
* Gym Leader/E4/Champion themes requires RCT to be installed, without it, all NPC trainer battles play the regional trainer theme.
* Biome resume on re-entry restarts the track from the beginning of the file rather than the exact playback position — Minecraft's sound API offers no seek capability.
* Pokémon Center/Mart trigger blocks must be manually placed inside each WorldEdit-pasted schematic via `/setblock`.
* Post-Sinnoh gyms (Unova, Kalos, Alola, Galar, Paldea) are not yet in the Cobbleverse datapacks — the architecture supports them, just add entries to `STRUCTURE_TO_REGION` and `GYM_STRUCTURES` when those datapacks arrive.
* Post-Unova ambience was intentionally not gathered — the biome coverage system supports adding it, no code changes needed, just new track registrations and OGG files.
</details>

<details>

<summary>🇧🇷 <b>Clique aqui para a versão em Português</b></summary>

**Autor:** kaizzinho  
**Pacote:** `com.kaizzinho.cobbletunes`  
**Plataforma:** Fabric, Minecraft 1.21.1  
**Dependências:** Cobblemon 1.7.3+, Fabric API, Fabric Language Kotlin  
**Dependências opcionais:** Radical Cobblemon Trainers (RCT) — melhora a detecção de nível de treinadores se presente, totalmente opcional  

## O que ele faz
CobbleTunes é um addon para Cobblemon que substitui todas as músicas de ambiente e de batalha do Minecraft por faixas originais da OST de Pokémon. A música é totalmente sensível ao contexto — o mod classifica o que o jogador está fazendo e onde ele está, e então seleciona automaticamente o tema regional ou específico da situação adequado. Possui suporte tanto para singleplayer quanto para servidores dedicados através de uma arquitetura de pacotes cliente/servidor.

## Arquitetura
O mod é dividido em dois conjuntos de código-fonte:

* **Lado do servidor (main/kotlin)** — `CobblemonBattleListener` se inscreve nos eventos reais de batalha do Cobblemon, classifica cada batalha (selvagem/treinador/lendário/PvP, além do nível do RCT, se disponível) e envia cargas de rede tipadas (network payloads) para os jogadores relevantes. `StructureZoneDetector` executa um tick de servidor lento que detecta de qual estrutura de worldgen do Cobbleverse ou `MusicTriggerBlock` o jogador está perto, e envia pacotes de mudança de zona quando isso muda.
* **Lado do cliente (client/kotlin)** — `CobbleTunesClient` recebe todos os pacotes do servidor e os roteia para `ClientMusicPlayer`. `ClientMusicPlayer` possui todo o estado de reprodução: ele controla `FadingSoundInstance` para transições de fade-in/fade-out, suprime o `MusicTracker` vanilla, gerencia a memória e rotação de ambientação por bioma, e lida com toda a lógica de bloqueio por silêncio. `TrackRegistry` inicializa cada faixa registrada na inicialização do cliente.

## Sistemas de música

### Música de batalha 
Cada batalha é resolvida para exatamente um `MusicContext` no momento em que começa. O classificador roda no lado do servidor e envia fatos brutos para o cliente, que aplica sua própria configuração por cima:

* **Batalhas selvagens** — resolvidas pelo número da Pokédex Nacional do Pokémon oponente → RegionOfOrigin (Região de Origem) → tema selvagem regional. Toda região de Kanto a Paldea (incluindo Hisui como sua própria entrada) tem uma faixa dedicada.
* **Encontros lendários/míticos** — apenas para encontros selvagens (correspondendo aos jogos da série principal). Substituições específicas de espécies têm prioridade sobre os padrões regionais; faixas compartilhadas por várias espécies (Regis, trio do clima, Tapus, etc.) são suportadas. O ás lendário de um treinador toca o tema normal de treinador, nunca o tema lendário.
* **Batalhas contra treinadores** — equipe adversária completa é enviada pelo lado do servidor; o cliente faz uma votação por maioria para a região da equipe e escolhe o tema de treinador dessa região. Empates são re-rolados aleatoriamente a cada batalha.
* **Líder de Ginásio / Elite Four / Campeão** — detectado através da dependência opcional `TrainerMobData.getType().id()` do RCT. Roteia para contextos dedicados `GYM_LEADER_BATTLE`, `ELITE_FOUR_BATTLE` e `CHAMPION_BATTLE`, cada um com faixas por região. Degrada graciosamente para `TRAINER_BATTLE` se o RCT estiver ausente.
* **PvP e rivais** — Rivais do RCT e batalhas reais jogador-contra-jogador (PvP) são roteados para um pool compartilhado `PVP_BATTLE` (temas de rivais e campeões de todas as regiões).

### Ambientação 
Orientado por bioma, independente de região. Um observador de tick do cliente (limitado a ~1s) lê o ID do bioma atual do jogador e o alimenta para `updateAmbienceBiome()`. Comportamentos principais:

* **Memória por bioma** — reentrar em um bioma retoma a mesma faixa que estava tocando antes, sem rolar novamente.
* **Rotação de duração real** — as faixas tocam sem loop; a rotação é acionada por `SoundManager.isPlaying()` retornando falso (o fim real da faixa), não por um temporizador artificial.
* **Bloqueio por silêncio** — três comportamentos distintos de silêncio: entrar no mundo/mudar de dimensão (10s fixos, ignora mudanças de bioma durante a espera); fim da faixa (aleatório 1:30–3:00, ignora mudanças de bioma); debounce de transição de bioma (aleatório 4–8s, reseta a cada oscilação de bioma, corta o áudio imediatamente — evita que biomas finos, como rios, iniciem um tema que o jogador já passou andando).
* **Biomas não mapeados** — totalmente transparentes; a faixa do bioma anterior continua até que o jogador se estabeleça em algum lugar com cobertura registrada.
* **Interação de batalha** — a música de batalha assume imediatamente, a ambientação retorna imediatamente ao final da batalha (sem intervalo de silêncio no final da batalha, por design).
* Todos os temporizadores respeitam `MinecraftClient.isPaused()` — nenhuma música inicia durante o menu de pausa.

### Música de estrutura / proximidade 
Um tick do lado do servidor (~5 segundos) detecta dois tipos de zonas em ordem de prioridade:

* **MusicTriggerBlock (maior prioridade)** — um bloco customizado com uma block entity carregando uma string `zoneId`. Coloque um dentro de qualquer estrutura construída manualmente (Centro Pokémon, Poké Mart). Escaneado dentro de um raio de 24 blocos. Coloque com `/setblock ~ ~ ~ cobbletunes:music_trigger{ZoneId:"cobbletunes:pokecenter"}`.
* **Estruturas de worldgen** — todos os ginásios e estruturas especiais do Cobbleverse são detectados via `ServerWorld.locateStructure()` dentro de um raio de 5 chunks. Não é necessário colocar blocos manualmente.

Três valores de `MusicContext` de proximidade:

* **GYM_AMBIENCE** — uma faixa por região (Kanto→Sinnoh), resolvida por qual estrutura de ginásio o jogador está perto.
* **POKECENTER** — pool plano de 5 temas de centro regionais, escolha aleatória.
* **POKEMART** — pool plano de 3 temas de mart, escolha aleatória.
* **SPECIAL_STRUCTURE** — 29 faixas 1:1 dedicadas para santuários lendários, bases de vilões e locais nomeados notáveis nas quatro regiões (cavernas de Articuno/Zapdos/Moltres, casa de Ash, Crown Cemetery, Bell Tower, Burned Tower, Celebi Shrine, Whirl Island, Sky Pillar, cavernas dos Regis, Deoxys, Jirachi, Spear Pillar, Snowpoint Temple, templos do trio do lago, Eterna Building e mais).

A música de zona toca imediatamente na detecção e retoma a ambientação normal do bioma imediatamente ao sair (sem intervalo de silêncio na saída da zona, por design).

## Configuração (cobbletunes-client.json)
Gerado na primeira execução em `.minecraft/config/cobbletunes-client.json`. Todos os valores são por cliente:

| Chave | Padrão | Descrição |
| :--- | :--- | :--- |
| `replaceAmbience` | `true` | Ativa a substituição da ambientação de bioma/estrutura |
| `replaceBattleMusic` | `true` | Ativa a substituição de música de batalha |
| `musicVolume` | `1.0` | Volume mestre para todas as faixas do CobbleTunes |
| `crossfadeSeconds` | `2.5` | Duração do fade-in e fade-out para todas as transições |
| `shuffleAmbienceTracks` | `true` | Escolha aleatória vs. primeiro registrado para pools de ambientação |
| `worldJoinSilenceSeconds` | `10.0` | Silêncio após entrar em um mundo ou mudar de dimensão |
| `trackEndSilenceMinSeconds` | `90.0` | Silêncio mínimo após uma faixa terminar naturalmente |
| `trackEndSilenceMaxSeconds` | `180.0` | Silêncio máximo após uma faixa terminar naturalmente |
| `biomeTransitionSilenceMinSeconds` | `4.0` | Janela mínima de debounce na mudança de bioma |
| `biomeTransitionSilenceMaxSeconds` | `8.0` | Janela máxima de debounce na mudança de bioma |

## Estrutura de arquivos de som (assets)
assets/cobbletunes/sounds/  
ambience/  
<biome_tracks_per_region>/  ← ambientação selvagem, marcada por conjuntos de chaves de bioma  
gym/                        ← uma por região: kanto_gym, johto_gym, etc.  
pokecenter/                 ← cinco temas de centro regionais  
pokemart/                   ← três temas de mart  
structure/                  ← 29 temas dedicados a lendários/estruturas especiais  
battle/  
wild/                       ← temas de batalha selvagem por região  
trainer/                    ← temas de treinador por região  
gym_leader/                 ← temas de líder de ginásio por região  
elite_four/                 ← temas da E4 por região  
champion/                   ← temas de campeão por região  
pvp/                        ← faixas de rival + campeão para o pool PvP  
legendary/                  ← substituições de espécies + padrões regionais  

## Regiões cobertas
* **Todos os contextos de batalha:** Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Hisui, Paldea
* **Ambientação:** Kanto, Johto, Hoenn, Sinnoh, Unova (por design — nenhuma ambientação coletada para regiões pós-Unova)
* **Proximidade de ginásio/estrutura:** Kanto, Johto, Hoenn, Sinnoh (todas as regiões atualmente nos datapacks do Cobbleverse)

## Limitações conhecidas / trabalhos futuros
* Suporte a Líder de Ginásio/E4/Campeão requer a instalação do RCT : Sem ele, todas as batalhas contra treinadores NPC tocam o tema de treinador regional.
* A retomada de bioma ao reentrar reinicia a faixa do início do arquivo em vez da posição exata de reprodução — a API de som do Minecraft não oferece recurso de busca (seek).
* Blocos de gatilho de Centro Pokémon/Mart devem ser colocados manualmente dentro de cada schematic colada com WorldEdit via `/setblock`.
* Ginásios pós-Sinnoh (Unova, Kalos, Alola, Galar, Paldea) ainda não estão nos datapacks do Cobbleverse — a arquitetura os suporta, basta adicionar entradas em `STRUCTURE_TO_REGION` e `GYM_STRUCTURES` quando esses datapacks chegarem.
* A ambientação pós-Unova intencionalmente não foi coletada: O sistema de cobertura de biomas suporta sua adição, não são necessárias alterações de código, apenas novos registros de faixa e arquivos OGG.
</details>