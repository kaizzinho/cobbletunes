# CobbleTunes — Music Framework 🎵🎮

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

### What is CobbleTunes?

**CobbleTunes** is a dynamic music framework for [Cobblemon](https://cobblemon.com/) on Fabric/Kotlin. It watches battles, biomes, structures, menus, and a few other game states, then picks the right music context for each one.

The repository contains the music logic, not the soundtrack. It doesn't ship, download, or generate copyrighted OST files. A resource pack provides the `.ogg` files, while CobbleTunes decides when each one should play.

### What it can detect

CobbleTunes currently handles:

- wild and legendary/mythical battles;
- regular NPC trainer battles;
- Gym Leaders, Elite Four, Champions, and RCT rivals;
- real player-versus-player battles;
- biome and underground ambience;
- Cobbleverse, vanilla, and BCA structure proximity;
- hand-placed Poké Center and Poké Mart music zones;
- title-screen music, low-HP alerts, and player death transitions.

### How battle routing works

Battle classification happens on the server. The client only receives a compact result and turns it into a music context.

A normal flow looks like this:

```text
Cobblemon battle event
    -> inspect the opposing actors
    -> classify the battle and trainer role
    -> resolve the trainer region when possible
    -> send the route to the client
    -> pick a registered SoundEvent from TrackRegistry
```

The route keeps the role and region separate. A Kanto Gym Leader, for example, becomes:

```text
leader|kanto
```

The client reads that as `GYM_LEADER_BATTLE` with Kanto as the preferred region, then chooses the Kanto Gym Leader `SoundEvent`. Brock doesn't need a custom `brock.ogg`; he uses the shared regional path documented in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### Dynamic RCT detection

[Radical Cobblemon Trainers](https://modrinth.com/mod/rctmod) is optional. When it is installed, CobbleTunes reads the opposing RCT trainer through Cobblemon's entity-backed battle actor. The integration is reflection-based, so RCT classes are never required for CobbleTunes to start.

The classifier doesn't rely on a hardcoded list of trainer names. It checks the available data in this order:

1. standard RCT roles such as `leader`, `e4`, `champ`, and `rival`;
2. regional custom types such as `kanto_league` or `sinnoh_champion`;
3. role hints inside the trainer ID, such as `hoenn_league_fosco`;
4. the current Cobbleverse progression pattern, where a non-optional regional trainer such as `kanto_brock` is treated as a Gym Leader;
5. regular trainer music if none of the stronger signals match.

The region comes from the RCT type or trainer ID whenever possible. If no region is available, the client falls back to a majority vote using the opposing Pokémon's Pokédex regions. Ties are picked randomly between the tied regions.

If RCT is missing or its API changes, CobbleTunes logs the compatibility issue once and safely falls back to regular trainer music.

### PvP and rivals

Real PvP battles do not use RCT. They go straight to the `PVP_BATTLE` context, and the opposing roster is used to choose a regional pool.

RCT rivals also use the PvP/rival context, but their region can come directly from trainer metadata instead of the roster vote.

### Structure music and battle resume

`StructureZoneDetector` checks nearby worldgen structures and `MusicTriggerBlock`s on the server. It confirms proximity against the structure's real bounding box before sending a zone update to the client.

The client keeps the active zone in its own state instead of tying it to whatever sound is currently audible. This matters when a battle starts inside a Gym, Poké Center, Poké Mart, or another tracked structure:

- battle music temporarily takes over;
- zone packets received during the battle update the saved zone without interrupting the battle theme;
- after a victory or forfeit, the active zone theme is restored;
- if the player left the zone during the battle, biome ambience resumes instead.

### Project layout

- **`src/main/kotlin`** — server/common entrypoint, Cobblemon battle events, RCT compatibility, trainer classification, structure detection, trigger blocks, configs, and network payloads.
- **`src/client/kotlin`** — packet handling, track routing, biome watching, menu music, low-HP logic, fading sounds, and all client playback state.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — every registered sound key and its expected resource-pack path.

### Main features

- [x] **Context-based battle music** for wild, legendary, trainer, Gym Leader, Elite Four, Champion, rival, and PvP battles.
- [x] **Dynamic RCT trainer classification** using type, trainer ID, region, and progression metadata.
- [x] **Regional track routing** with trainer metadata first and roster voting as fallback.
- [x] **Biome ambience memory** so revisiting a biome can bring back its previous track.
- [x] **Natural track rotation and silence windows** instead of immediately chaining songs together.
- [x] **Biome transition debounce** to avoid changing music for tiny biomes the player crosses in a few seconds.
- [x] **Underground cave detection** based on sky light and height, not only the surface biome.
- [x] **Structure and trigger-block ambience** for Gyms, special structures, villages, Poké Centers, and Poké Marts.
- [x] **Zone restoration after battles**, including forfeits inside tracked structures.
- [x] **Menu music**, **low-HP cue**, and **player-death handling**.
- [x] **Optional RCT integration** with a safe ordinary-trainer fallback.
- [x] **Client and server debug logs**, disabled by default.

### Config files

CobbleTunes creates two files inside the instance's `config` folder:

- `cobbletunes-client.json` — music replacement toggles, volume, crossfade time, silence ranges, shuffle, and client debug logs;
- `cobbletunes-server.json` — server-side battle/zone debug logs.

A full restart is recommended after changing server-side settings.

### Audio and licensing

No Pokémon OST files are included. Those tracks are copyrighted by their respective owners, and this project does not host or redistribute them.

To add music, build a resource pack with `.ogg` files under:

```text
assets/cobbletunes/sounds/
```

Every expected key and path is listed in [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md). Missing files are safe: that specific event stays silent instead of crashing the game.

### Building

Java 21 is required. From the project root:

```powershell
./gradlew clean build -PexcludeAudio
```

The built JAR will be placed in `build/libs/`.

### Roadmap

- [ ] Add post-Sinnoh Cobbleverse proximity mappings as those structures become available.
- [ ] Expand post-Unova biome ambience coverage.
- [ ] Add more vanilla/BCA structure pools.
- [ ] Move exceptional trainer-role overrides to a small external config if future datapacks need them.

### License

The code is available under the MIT license. Audio packs are separate works and keep their own licensing terms.

---

## Português

### O que é o CobbleTunes?

**CobbleTunes** é um framework de música dinâmica para [Cobblemon](https://cobblemon.com/) em Fabric/Kotlin. Ele acompanha batalhas, biomas, estruturas, menus e alguns outros estados do jogo para escolher o contexto musical certo em cada situação.

O repositório contém a lógica musical, não a trilha sonora. Ele não distribui, baixa nem gera arquivos de OST protegidos por direitos autorais. O resource pack fornece os arquivos `.ogg`, e o CobbleTunes decide quando cada um deve tocar.

### O que ele detecta

Atualmente, o CobbleTunes lida com:

- batalhas selvagens e lendárias/míticas;
- batalhas contra treinadores NPC comuns;
- Líderes de Ginásio, Elite Four, Campeões e rivais do RCT;
- batalhas reais entre jogadores;
- ambientação por bioma e ambiente subterrâneo;
- proximidade de estruturas do Cobbleverse, vanilla e BCA;
- zonas manuais de Centro Pokémon e Poké Mart;
- música de menu, alerta de HP baixo e transições após a morte do jogador.

### Como a rota de batalha funciona

A classificação da batalha acontece no servidor. O cliente recebe apenas um resultado compacto e transforma isso em um contexto musical.

O fluxo normal é este:

```text
evento de batalha do Cobblemon
    -> analisa os atores adversários
    -> classifica a batalha e o papel do treinador
    -> resolve a região quando possível
    -> envia a rota ao cliente
    -> escolhe um SoundEvent registrado no TrackRegistry
```

A rota mantém o papel e a região separados. Um Líder de Ginásio de Kanto, por exemplo, vira:

```text
leader|kanto
```

O cliente interpreta isso como `GYM_LEADER_BATTLE`, com Kanto como região preferida, e escolhe o `SoundEvent` regional. O Brock não precisa de um arquivo `brock.ogg`; ele usa o tema compartilhado de Líder de Ginásio de Kanto descrito no [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md).

### Detecção dinâmica do RCT

O [Radical Cobblemon Trainers](https://modrinth.com/mod/rctmod) é opcional. Quando ele está instalado, o CobbleTunes encontra o treinador adversário através do ator de batalha do Cobblemon e lê os dados do RCT por reflexão. Por isso, as classes do RCT não são necessárias para o CobbleTunes iniciar.

O classificador não depende de uma lista fixa de nomes. Ele verifica os dados disponíveis nesta ordem:

1. papéis padrão do RCT, como `leader`, `e4`, `champ` e `rival`;
2. tipos regionais personalizados, como `kanto_league` ou `sinnoh_champion`;
3. indícios no ID do treinador, como `hoenn_league_fosco`;
4. o padrão de progressão atual do Cobbleverse, no qual um treinador regional não opcional, como `kanto_brock`, é tratado como Líder de Ginásio;
5. música de treinador comum quando nenhum sinal mais forte combina.

A região vem do tipo ou do ID do treinador sempre que possível. Quando ela não está disponível, o cliente faz uma votação pela região de origem dos Pokémon adversários. Em caso de empate, uma das regiões empatadas é escolhida aleatoriamente.

Se o RCT não estiver instalado ou mudar sua API, o CobbleTunes registra o problema de compatibilidade uma vez e volta para a música comum de treinador sem derrubar o jogo.

### PvP e rivais

Batalhas reais entre jogadores não dependem do RCT. Elas entram diretamente no contexto `PVP_BATTLE`, e a equipe adversária é usada para escolher o pool regional.

Rivais do RCT também usam o contexto de rival/PvP, mas a região pode vir diretamente dos dados do treinador em vez da votação da equipe.

### Música de estrutura e retorno após a batalha

O `StructureZoneDetector` procura estruturas de worldgen e `MusicTriggerBlock`s próximos no servidor. Antes de enviar a mudança de zona, ele confirma a distância usando a caixa delimitadora real da estrutura.

O cliente guarda a zona ativa separadamente da música que está tocando naquele momento. Isso é importante quando uma batalha começa dentro de um Ginásio, Centro Pokémon, Poké Mart ou outra estrutura rastreada:

- a música de batalha assume temporariamente;
- pacotes de zona recebidos durante a batalha atualizam o estado salvo sem cortar o tema da batalha;
- após uma vitória ou desistência, o tema da zona ativa volta;
- se o jogador sair da zona durante a batalha, a ambientação do bioma retorna.

### Organização do projeto

- **`src/main/kotlin`** — inicialização comum/servidor, eventos do Cobblemon, compatibilidade com RCT, classificação de treinadores, detecção de estruturas, blocos de zona, configs e payloads de rede.
- **`src/client/kotlin`** — recebimento de pacotes, escolha das faixas, observação de biomas, música de menu, HP baixo, fades e todo o estado de reprodução no cliente.
- **`src/main/resources/assets/cobbletunes/sounds.json`** — todas as chaves de som e os caminhos esperados no resource pack.

### Funcionalidades principais

- [x] **Música de batalha por contexto** para selvagem, lendário, treinador, Líder de Ginásio, Elite Four, Campeão, rival e PvP.
- [x] **Classificação dinâmica de treinadores do RCT** usando tipo, ID, região e dados de progressão.
- [x] **Escolha regional de faixa** com metadados do treinador como prioridade e votação da equipe como fallback.
- [x] **Memória de ambientação por bioma**, permitindo retomar a faixa usada anteriormente.
- [x] **Rotação natural e intervalos de silêncio**, sem emendar uma música na outra o tempo todo.
- [x] **Debounce na troca de bioma** para evitar mudanças causadas por biomas muito estreitos.
- [x] **Detecção de cavernas** por luz do céu e altura, independente do bioma da superfície.
- [x] **Ambientação por estrutura e bloco de gatilho** para Ginásios, estruturas especiais, vilas, Centros Pokémon e Poké Marts.
- [x] **Retorno da zona após batalhas**, incluindo desistências dentro de estruturas rastreadas.
- [x] **Música de menu**, **alerta de HP baixo** e **tratamento de morte do jogador**.
- [x] **Integração opcional com RCT**, com fallback seguro para treinador comum.
- [x] **Logs de depuração no cliente e servidor**, desativados por padrão.

### Arquivos de configuração

O CobbleTunes cria dois arquivos dentro da pasta `config` da instância:

- `cobbletunes-client.json` — opções de substituição de música, volume, crossfade, intervalos de silêncio, shuffle e logs do cliente;
- `cobbletunes-server.json` — logs do servidor para classificação de batalha e mudanças de zona.

É recomendado reiniciar o jogo por completo após alterar opções do servidor.

### Áudio e licenciamento

Nenhuma faixa oficial de Pokémon é incluída. Essas músicas pertencem aos seus respectivos detentores de direitos, e o projeto não hospeda nem redistribui esse conteúdo.

Para adicionar música, crie um resource pack com os arquivos `.ogg` dentro de:

```text
assets/cobbletunes/sounds/
```

Todas as chaves e caminhos esperados estão listados no [`SOUND_MANIFEST.md`](./SOUND_MANIFEST.md). Arquivos ausentes são seguros: apenas aquele evento fica em silêncio, sem causar crash.

### Compilação

É necessário usar Java 21. Na raiz do projeto:

```powershell
./gradlew clean build -PexcludeAudio
```

O JAR compilado será gerado em `build/libs/`.

### Roadmap

- [ ] Adicionar estruturas de proximidade pós-Sinnoh conforme elas forem disponibilizadas pelo Cobbleverse.
- [ ] Expandir a ambientação por bioma após Unova.
- [ ] Criar mais pools para estruturas vanilla/BCA.
- [ ] Mover exceções futuras de papel de treinador para uma pequena configuração externa, caso novos datapacks precisem disso.

### Licença

O código está disponível sob a licença MIT. Resource packs de áudio são trabalhos separados e mantêm suas próprias condições de licença.
