# CobbleTunes — Framework 🎵🎮

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-Loom-DBB69B?logo=fabric&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?logo=kotlin&logoColor=white)
![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.3-3E8E41)
![License](https://img.shields.io/badge/license-MIT-blue)
![Audio](https://img.shields.io/badge/audio-not%20included-lightgrey)

*Read this in [English](#english) | Leia em [Português](#português)*

---

## English

### Overview
**CobbleTunes** is a context-aware dynamic music **framework** for [Cobblemon](https://cobblemon.com/) (Fabric/Kotlin). It replaces Minecraft's ambient and battle music with a fully classified, server-driven soundtrack system — wild battles, trainer battles, gym/E4/champion tiers, PvP, biome ambience, structure proximity, and menu music — all resolved automatically based on what the player is doing and where they are.

**This repository ships as a framework, not a soundtrack.** No audio files are distributed here — see [Audio & Licensing](#audio--licensing) below for why, and for how to supply your own.

### The problem
Vanilla Minecraft's music system has no concept of "what kind of battle is this" or "which in-game region does this biome represent" — it's driven by biome tags and a flat combat/peaceful split. A Pokémon-themed server has no way to make its soundtrack actually feel like the games it's inspired by without either hardcoding a fragile one-off solution or building the whole classification pipeline from scratch.

### The solution
A soft-dependent Cobblemon addon that classifies every battle and every location into a precise music context server-side, streams that classification to the client over a lightweight packet protocol, and drives all actual playback (crossfades, silence pacing, per-biome memory, rotation) through a single client-side player — leaving the actual track selection entirely up to whatever resource pack the server operator supplies.

### Architecture
Split-source-set Fabric mod (Fabric Loom, Yarn mappings, Fabric Language Kotlin):

- **Server-side (`main/kotlin`)** — `CobblemonBattleListener` subscribes to Cobblemon's real battle events, classifies each battle (wild/trainer/legendary/PvP, plus RCT tier if available), and sends typed network payloads to the relevant players. `StructureZoneDetector` runs a periodic server tick that detects which worldgen structure or `MusicTriggerBlock` the player is near and sends zone-change packets when it changes.
- **Client-side (`client/kotlin`)** — `CobbleTunesClient` receives all server packets and routes them to `ClientMusicPlayer`. `ClientMusicPlayer` owns all playback state: it drives `FadingSoundInstance` for fade-in/fade-out crossfades, suppresses vanilla's `MusicTracker`, manages per-biome ambience memory and rotation, and handles all silence-gating logic. `TrackRegistry` bootstraps every registered sound event on client init.

### Key Features

- [x] **Fully classified battle music** — wild, trainer (majority-vote by regional roster), legendary/mythical (species-specific overrides), gym leader/elite four/champion (via optional RCT integration), and PvP/rival, each resolving to its own dedicated context.
- [x] **Biome-driven ambience** — every vanilla and Terralith biome mapped to a themed pool, with per-biome memory (re-entering a biome resumes the same track), real-length rotation (detected via the sound engine, not a guessed timer), and underground cave detection independent of the surface biome.
- [x] **Three-tier silence design** — world-join/dimension-change silence, natural track-end silence, and a debounce window on biome transitions that cuts audio immediately and only commits once the new biome has been stable — so a thin biome like a river never triggers a theme the player has already walked past.
- [x] **Structure & proximity music** — worldgen structures (gyms, dungeons, villages, and dozens more) and hand-placed `MusicTriggerBlock`s (Poké Centers, Poké Marts) both drive dedicated ambience contexts, verified against each structure's real bounding box rather than a coarse chunk-reference guess.
- [x] **Menu music** — plays continuously across the title screen and all submenus, stops instantly the moment a world loads.
- [x] **Low HP alert** — detects the player's active battle Pokémon dropping to critical HP and plays a configurable alert cue.
- [x] **Player-death handling** — battle music and ambience both cut cleanly on death, with a configurable silence window before ambience resumes at the respawn location.
- [x] **Zero hardcoded audio** — every sound is a standard Minecraft `SoundEvent` resolved through `sounds.json`; supplying your own resource pack is the entire integration surface.
- [x] **Soft dependencies only** — Radical Cobblemon Trainers (RCT) enhances trainer-tier detection when present; the mod runs cleanly without it.
- [x] **Full debug logging mode** — a single config flag on both the client and server side surfaces every state transition (silence countdowns, rotation, zone changes, battle classification) for bug reports, off by default.

### Audio & Licensing
CobbleTunes is built to work with an authentic Pokémon-style soundtrack, but **the OST tracks themselves are not distributed with this mod** — they're copyrighted material owned by Nintendo/Game Freak/Creatures Inc., and this project does not host, bundle, or redistribute them.

What you get from this repository is the complete framework: every `SoundEvent` is registered and every `sounds.json` entry is defined, pointing at expected file paths under `assets/cobbletunes/sounds/`. To actually hear music, pair this mod with a resource pack that supplies `.ogg` files at those paths — the full expected file/path reference is documented in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) *(generate and link this alongside your resource pack)*.

### Roadmap
- [ ] Post-Sinnoh gym/structure proximity coverage (Unova, Kalos, Alola, Galar, Paldea) once those Cobbleverse datapacks exist
- [ ] Post-Unova ambience biome coverage
- [ ] Vanilla/BCA structure category expansion as new pools are curated

### Setup
For setup instructions, see the [Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) page for the IDE you're using. Requires Cobblemon 1.7.3+ and Fabric Language Kotlin. See [Audio & Licensing](#audio--licensing) above before expecting any sound.

### License
The **code** in this project is available under the MIT license. This license does **not** extend to any audio assets — none are included, and any resource pack you use alongside this mod is subject to its own licensing (or lack thereof).

---

## Português

### Visão geral
**CobbleTunes** é um **framework** de música dinâmica e sensível ao contexto para o [Cobblemon](https://cobblemon.com/) (Fabric/Kotlin). Ele substitui a música ambiente e de batalha do Minecraft por um sistema de trilha sonora totalmente classificado e orientado pelo servidor — batalhas selvagens, batalhas de treinador, tiers de ginásio/E4/campeão, PvP, ambientação por bioma, proximidade de estrutura e música de menu — tudo resolvido automaticamente com base no que o jogador está fazendo e onde está.

**Este repositório é distribuído como um framework, não como uma trilha sonora.** Nenhum arquivo de áudio é distribuído aqui — veja [Áudio e Licenciamento](#áudio-e-licenciamento) abaixo para entender o porquê, e como fornecer o seu próprio.

### O problema
O sistema de música padrão do Minecraft não tem noção de "que tipo de batalha é essa" ou "qual região do jogo esse bioma representa" — ele é orientado por tags de bioma e uma divisão simples entre combate/paz. Um servidor temático de Pokémon não tem como fazer sua trilha sonora realmente parecer com a dos jogos que o inspiram, a menos que crie uma solução frágil e pontual ou construa todo o pipeline de classificação do zero.

### A solução
Um addon do Cobblemon com dependência opcional que classifica cada batalha e cada localização em um contexto musical preciso no lado do servidor, transmite essa classificação para o cliente através de um protocolo de pacotes leve, e controla toda a reprodução real (crossfades, ritmo de silêncio, memória por bioma, rotação) através de um único player do lado do cliente — deixando a seleção real das faixas inteiramente a cargo do resource pack que o operador do servidor fornecer.

### Arquitetura
Mod Fabric com conjuntos de código-fonte divididos (Fabric Loom, mappings Yarn, Fabric Language Kotlin):

- **Lado do servidor (`main/kotlin`)** — `CobblemonBattleListener` se inscreve nos eventos reais de batalha do Cobblemon, classifica cada batalha (selvagem/treinador/lendário/PvP, além do nível do RCT, se disponível) e envia cargas de rede tipadas para os jogadores relevantes. `StructureZoneDetector` executa um tick periódico de servidor que detecta qual estrutura de worldgen ou `MusicTriggerBlock` o jogador está perto, e envia pacotes de mudança de zona quando isso muda.
- **Lado do cliente (`client/kotlin`)** — `CobbleTunesClient` recebe todos os pacotes do servidor e os roteia para `ClientMusicPlayer`. `ClientMusicPlayer` possui todo o estado de reprodução: controla `FadingSoundInstance` para transições de fade-in/fade-out, suprime o `MusicTracker` vanilla, gerencia a memória e rotação de ambientação por bioma, e lida com toda a lógica de bloqueio por silêncio. `TrackRegistry` inicializa cada evento de som registrado na inicialização do cliente.

### Funcionalidades principais

- [x] **Música de batalha totalmente classificada** — selvagem, treinador (votação por maioria com base na composição regional da equipe), lendário/mítico (substituições específicas por espécie), líder de ginásio/elite four/campeão (via integração opcional com RCT), e PvP/rival, cada um resolvendo para seu próprio contexto dedicado.
- [x] **Ambientação orientada por bioma** — todo bioma vanilla e do Terralith mapeado para um pool temático, com memória por bioma (reentrar em um bioma retoma a mesma faixa), rotação de duração real (detectada pelo motor de som, não por um temporizador estimado), e detecção de cavernas subterrâneas independente do bioma de superfície.
- [x] **Design de silêncio em três camadas** — silêncio ao entrar no mundo/mudar de dimensão, silêncio natural ao fim de uma faixa, e uma janela de debounce nas transições de bioma que corta o áudio imediatamente e só confirma quando o novo bioma se mantém estável — assim um bioma fino como um rio nunca dispara um tema que o jogador já passou andando.
- [x] **Música de estrutura e proximidade** — estruturas de worldgen (ginásios, masmorras, vilas e dezenas de outras) e `MusicTriggerBlock`s colocados manualmente (Centros Pokémon, Poké Marts) acionam contextos de ambientação dedicados, verificados contra a caixa delimitadora real de cada estrutura em vez de uma estimativa grosseira por referência de chunk.
- [x] **Música de menu** — toca continuamente pela tela de título e todos os submenus, parando instantaneamente assim que um mundo é carregado.
- [x] **Alerta de HP baixo** — detecta quando o Pokémon ativo do jogador em batalha cai para HP crítico e toca um aviso sonoro configurável.
- [x] **Tratamento de morte do jogador** — tanto a música de batalha quanto a ambientação são cortadas de forma limpa ao morrer, com uma janela de silêncio configurável antes que a ambientação retorne no local de respawn.
- [x] **Zero áudio embutido no código** — cada som é um `SoundEvent` padrão do Minecraft resolvido através do `sounds.json`; fornecer seu próprio resource pack é toda a superfície de integração necessária.
- [x] **Apenas dependências opcionais** — Radical Cobblemon Trainers (RCT) melhora a detecção de tier de treinador quando presente; o mod funciona normalmente sem ele.
- [x] **Modo completo de log de depuração** — uma única flag de configuração, tanto no lado do cliente quanto no servidor, expõe cada transição de estado (contagens regressivas de silêncio, rotação, mudanças de zona, classificação de batalha) para relatórios de bugs, desativada por padrão.

### Áudio e Licenciamento
CobbleTunes foi construído para funcionar com uma trilha sonora autêntica no estilo Pokémon, mas **as faixas da OST em si não são distribuídas com este mod** — são material protegido por direitos autorais pertencente à Nintendo/Game Freak/Creatures Inc., e este projeto não hospeda, empacota ou redistribui esse conteúdo.

O que você recebe deste repositório é o framework completo: cada `SoundEvent` está registrado e cada entrada do `sounds.json` está definida, apontando para caminhos de arquivo esperados dentro de `assets/cobbletunes/sounds/`. Para realmente ouvir música, combine este mod com um resource pack que forneça arquivos `.ogg` nesses caminhos — a referência completa de arquivos/caminhos esperados está documentada em [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md) *(gere e vincule este arquivo junto com seu resource pack)*.

### Roadmap
- [ ] Cobertura de proximidade de ginásio/estrutura pós-Sinnoh (Unova, Kalos, Alola, Galar, Paldea) assim que esses datapacks do Cobbleverse existirem
- [ ] Cobertura de ambientação de bioma pós-Unova
- [ ] Expansão de categorias de estrutura vanilla/BCA conforme novos pools forem curados

### Configuração
Para instruções de configuração, veja a página de [Documentação do Fabric](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) referente à IDE que você está usando. Requer Cobblemon 1.7.3+ e Fabric Language Kotlin. Veja [Áudio e Licenciamento](#áudio-e-licenciamento) acima antes de esperar qualquer som.

### Licença
O **código** deste projeto está disponível sob a licença MIT. Esta licença **não** se estende a nenhum recurso de áudio — nenhum está incluído, e qualquer resource pack que você usar junto com este mod está sujeito à sua própria licença (ou à ausência dela).
