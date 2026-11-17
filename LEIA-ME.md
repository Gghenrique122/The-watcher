# CAMINHO FÁCIL (sem instalar Java nem Gradle)

## Parte 1 — Gerar o .jar na nuvem (GitHub, grátis)
1. Crie conta em github.com e clique em **New repository** (nome qualquer, público ou privado) → **Create repository**.
2. Extraia este zip. Abra a pasta `stalker_fabric` e **arraste o CONTEÚDO dela** (src, build.gradle, gradle.properties, settings.gradle, .github, LEIA-ME.md) para a página do repositório (**uploading an existing file**) → **Commit changes**.
   - Se a pasta `.github` não subir (no Mac ela fica oculta): clique **Add file → Create new file**, digite o nome `.github/workflows/build.yml`, cole o conteúdo do arquivo `build.yml` e faça Commit.
3. Abra a aba **Actions**. A compilação começa sozinha (3 a 6 min). Quando aparecer o ✅ verde, clique nela.
4. Em **Artifacts**, baixe `stalker_horror-mod`. Dentro do zip está o `stalker_horror-1.0.0.jar`.
   - Se aparecer ❌ vermelho: abra o log, copie o erro e me mande.

## Parte 2 — Jogar usando o Modrinth App (só arrastar)
1. Instale o **Modrinth App** (modrinth.com/app).
2. **Create instance** → Minecraft **1.20.1** → Loader **Fabric**.
3. Na instância, **Add content** → procure **Fabric API** → Install.
4. **Arraste o `stalker_horror-1.0.0.jar`** para a janela da instância (ou Add content → From file).
5. **Play.** Crie um mundo com cheats ligados e use `/stalker summon` para testar.
(Funciona igual no CurseForge app e no Prism Launcher.)

---
# CAMINHO MANUAL (para quem tem JDK 17)
## The Watcher — Terror psicológico (Fabric 1.20.1)

Uma presença te observa à distância. Ela nunca chega perto, aparece raramente e **aprende com o que você faz**.
(Funcionalidades iguais à versão Forge: Observador adaptativo, Sanidade, sustos sonoros, Amuleto de Proteção, noite escura e neblina.)

## COMO JOGAR (sem compilar nada, quando você tiver o .jar)
Você precisa de 3 coisas na pasta do Minecraft Fabric 1.20.1:
1. **Fabric Loader 1.20.1** — o jeito mais fácil: no **CurseForge app**, **Modrinth app** ou **Prism Launcher**, crie um perfil/instância novo: Minecraft **1.20.1** + loader **Fabric**. Eles instalam tudo sozinhos.
2. **Fabric API** — baixe pelo próprio app (aba Mods → procure "Fabric API") e instale na instância.
3. **O mod**: `stalker_horror-1.0.0.jar` dentro da pasta `mods` da instância.
Abra o jogo. Teste com `/stalker summon` (precisa de cheats ligados).

## COMO GERAR O .jar
1. Instale o **JDK 17** (ex.: Adoptium Temurin 17).
2. Baixe o template oficial: github.com/FabricMC/fabric-example-mod (branch **1.20**) → Code → Download ZIP, e extraia.
3. Dentro da pasta extraída: **apague a pasta `src`** do template e copie para lá a `src` deste projeto.
   Substitua também `build.gradle`, `gradle.properties` e `settings.gradle` pelos deste projeto.
   (Mantenha `gradlew`, `gradlew.bat` e a pasta `gradle/` do template.)
4. No terminal, dentro da pasta: `./gradlew build` (Windows: `gradlew.bat build`).
5. O arquivo sai em `build/libs/stalker_horror-1.0.0.jar` (NÃO use o `-sources.jar`).
   Para testar direto: `./gradlew runClient`.

## Configuração
Arquivo `config/stalker_horror.json` (criado no primeiro uso). Exemplos:
`nightDarkness` (0 a 0.97), `fogDistance`, `graceMinutes`, `minCooldownMinutes`, `minDistance`, `enableHaunts`, `hauntSnuffTorches`...

## Comandos (op)
`/stalker summon [watcher|peripheral|tunnel|pursuit|far|window]` · `/stalker haunt` · `/stalker sanity <0-100>` · `/stalker info` · `/stalker reset`

## Publicar
CurseForge: Author Console → Create Project → Mods → envie o .jar → Game version **1.20.1**, loader **Fabric**, Java 17.
Em "Relations", marque **Fabric API** como dependência obrigatória.
