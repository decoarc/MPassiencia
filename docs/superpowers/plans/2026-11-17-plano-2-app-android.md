# Plano 2: app Android — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o módulo `app` (Jetpack Compose) que transforma o motor `core` num jogo jogável: menu de feltro verde, mesa responsiva no celular deitado, toque e arrastar, animações, dica, fim automático, vitória, opções, salvamento e sons.

**Architecture:** Activity única, travada em paisagem e em tela cheia. A navegação usa Navigation 3 (Menu, Jogo, Como Jogar, Opções), e a vitória é um painel sobre a mesa. O `GameViewModel` guarda a `Game` do `core`, o cronômetro e o estado da dica, e salva a partida após cada jogada. A geometria da mesa é uma função pura (`TableLayout`), testada sem Android. Cada carta é desenhada uma única vez com a chave da própria `Card`, e sua posição vem da geometria, então mover uma carta entre pilhas anima sozinho.

**Tech Stack:** AGP 9.5.1 (Kotlin embutido), Kotlin 2.4.21 com o plugin do compilador Compose, Compose BOM 2026.09.00 (Material 3), Navigation 3 1.2.1, `lifecycle-viewmodel-compose` / `lifecycle-viewmodel-navigation3` 2.11.0, `activity-compose` 1.13.0, DataStore Preferences 1.3.0, `kotlinx-serialization-json` 1.13.0. Testes: JUnit + `kotlin.test`, `kotlinx-coroutines-test` 1.11.0, Robolectric 4.18, `compose-ui-test-junit4`, Roborazzi 1.79.0.

**Spec:** `docs/superpowers/specs/2026-11-17-paciencia-triplo-base-design.md` (seções 2, 3, 6.2 a 6.11 e 8). O motor está pronto no `core` (Plano 1).

## Global Constraints

- `namespace` e `applicationId` provisórios: `io.github.decarc.mpaciencia` (spec §9). O nome do app sai só de `@string/app_name` = "Paciência Triplo".
- `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`. `screenOrientation="sensorLandscape"` na Activity.
- Tela cheia: barras do sistema escondidas, reaparecendo com um gesto (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). O conteúdo respeita o recorte da câmera (`WindowInsets.displayCutout`).
- Todos os textos em `res/values/strings.xml`, em pt-BR. Nenhum texto fixo no código.
- Sem internet (sem a permissão `INTERNET`), sem anúncios, sem login, sem coleta de dados.
- Nenhuma arte, som ou texto de terceiros, e nenhuma menção ao site de referência do jogo. As cartas são desenhadas no código, os sons são gerados pelo próprio projeto e tudo fica registrado em `docs/CREDITS.md`.
- **Cores** (tokens em `ui/theme/Colors.kt`):

| Token | Cor |
|---|---|
| `FeltCenter` | `#2F8A4C` |
| `FeltEdge` | `#145A2C` |
| `Cream` | `#F3E3B3` |
| `CreamShadow` | `#B9A46A` |
| `Gold` | `#FFD34D` |
| `GoldShadow` | `#C79A12` |
| `Ink` | `#1D4A2A` |
| `TitleCream` | `#FFF6D8` |
| `CardRed` | `#C4161C` |
| `CardBlack` | `#111111` |
| `BackRed` | `#B8322A` |
| `BackRedLight` | `#D9473D` |
| `SlotLine` | branco com 28% de opacidade |
| `HintOutline` | `Gold` |

- **Pontuação, regras, desfazer, dica e salvamento vêm sempre do `core`.** O `app` não reimplementa regra nenhuma.
- **Comandos de verificação**, sempre sem `rtk`, conferindo `; echo "exit=$?"`:
  - `./gradlew :app:testDebugUnitTest` (unitários e Robolectric)
  - `./gradlew :app:verifyRoborazziDebug` (prints de tela, depois de gravados com `recordRoborazziDebug`)
  - `./gradlew :app:assembleDebug`
  - `./gradlew :core:test`
- O SDK fica em `C:\Users\ander\AppData\Local\Android\Sdk`. Escreva `local.properties` com `sdk.dir` (já está no `.gitignore`). Há um emulador `Pixel_6`.

## Review Focus

1. **O app é morto no meio da partida** (falta de memória ou fechado pelo sistema): ao reabrir, "Continuar" traz a mesa idêntica e o tempo de antes. Teste na Task 5 (`restores game and elapsed time from the store`).
2. **Dois toques rápidos na mesma carta**, ou um toque durante a animação: cada toque é calculado sobre o estado atual, sem travar e sem jogada fantasma. Teste na Task 5 (`two quick taps on the same card play at most two legal moves`).
3. **Soltar uma carta arrastada fora de qualquer pilha ou num destino inválido:** a carta volta, o número de movimentos não muda e nada é salvo. Testes nas Tasks 3 (`hitTest outside every pile is null`) e 5 (`illegal drop changes nothing`).
4. **Coluna longa no fim do jogo num celular pequeno** (640 × 360 dp, 12 cartas viradas para baixo e 13 para cima): nada sai da largura, o canto de cada carta virada para cima continua visível e a área das colunas passa a rolar. Teste na Task 3.
5. **Vitória e app em segundo plano:** depois da vitória, o salvamento é apagado e "Continuar" some do menu. O cronômetro para em segundo plano e não conta o tempo fora do app. Testes na Task 6.

---

## Estrutura de arquivos

```
settings.gradle.kts                    + include(":app") e repositório google()
gradle/libs.versions.toml              + versões Android
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml, themes.xml
app/src/main/res/raw/flip.wav, place.wav, foundation.wav, win.wav   (gerados pela Task 10)
app/src/main/kotlin/io/github/decarc/mpaciencia/
  MainActivity.kt                      tela cheia + setContent { App() }
  App.kt                               Navigation 3: Menu, Game, Rules, Settings
  ui/theme/Colors.kt, Theme.kt, Felt.kt   tokens e fundo de feltro (degradê radial)
  ui/cards/CardFace.kt                 desenho da carta, do verso e do espaço vazio + rankLabel()
  ui/game/TableLayout.kt               geometria pura da mesa + hitTest()
  ui/game/GameViewModel.kt             estado da partida, cronômetro, dica, fim automático, eventos de som
  ui/game/GameUiState.kt
  ui/game/GameScreen.kt                barra superior + mesa + diálogos + painel de vitória
  ui/game/TableView.kt                 cartas posicionadas, toque e arrastar, destaque da dica
  ui/game/WinOverlay.kt
  ui/menu/MenuScreen.kt, NewGameDialog.kt
  ui/rules/RulesScreen.kt
  ui/settings/SettingsScreen.kt
  data/GameStore.kt                    salvamento da partida em arquivo
  data/SettingsStore.kt                opções no DataStore
  audio/SoundPlayer.kt                 SoundPool
app/src/test/kotlin/io/github/decarc/mpaciencia/...   (JVM + Robolectric + Roborazzi)
tools/gen_sounds.py                    gera os .wav
docs/CREDITS.md
```

---

### Task 1: Módulo `app` que abre em tela cheia e em paisagem

**Files:**
- Modify: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`, `AndroidManifest.xml`, `strings.xml`, `themes.xml`, `MainActivity.kt`, `App.kt` (por enquanto mostra só o fundo de feltro com o título), `ui/theme/Colors.kt`, `Theme.kt`, `Felt.kt`, `local.properties`
- Test: `app/src/test/.../MainActivitySmokeTest.kt`

**Interfaces:**
- Produces:
  - `@Composable fun MPacienciaTheme(content: @Composable () -> Unit)`
  - `fun Modifier.feltBackground(): Modifier`: degradê radial de `FeltCenter` (centro, a 40% da altura) até `FeltEdge`, cobrindo a tela.
  - `@Composable fun App()`

- [ ] **Step 1: Gradle.**
  - Em `settings.gradle.kts`, adicione `google()` aos repositórios de plugins e de dependências, e `include(":app")`.
  - Em `libs.versions.toml`, adicione as versões do cabeçalho e os plugins `com.android.application` e `org.jetbrains.kotlin.plugin.compose`.
  - `app/build.gradle.kts` aplica `android.application`, `kotlin.compose` e `kotlin.serialization`. O AGP 9 já embute o Kotlin, então **não** aplique `kotlin-android`. Se o build reclamar disso, siga a skill `agp-9-upgrade`.
  - Dependências: `implementation(project(":core"))`, o Compose BOM, Material 3, `activity-compose`, `lifecycle-viewmodel-compose`, Navigation 3 (`runtime` e `ui`) e `lifecycle-viewmodel-navigation3`. Para testes: Robolectric, `compose-ui-test-junit4`, `compose-ui-test-manifest` (só debug), Roborazzi e `kotlinx-coroutines-test`.
  - Plugin Roborazzi aplicado e `testOptions.unitTests.isIncludeAndroidResources = true`.
- [ ] **Step 2: Escrever o teste que falha.**

```kotlin
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class MainActivitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun `app opens showing the title`() {
        rule.onNodeWithText("Paciência Triplo").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Rodar e ver falhar.** `./gradlew :app:testDebugUnitTest; echo "exit=$?"`. Esperado: falha de compilação (`MainActivity` não existe).
- [ ] **Step 4: Implementar.**
  - Manifest: Activity única com `screenOrientation="sensorLandscape"`, tema `Theme.Material3.DayNight.NoActionBar` com fundo `FeltEdge`, sem permissão `INTERNET`.
  - `MainActivity.onCreate`: `enableEdgeToEdge()`, depois `WindowCompat.getInsetsController(window, window.decorView)` com `hide(WindowInsetsCompat.Type.systemBars())` e `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, depois `setContent { MPacienciaTheme { App() } }`.
- [ ] **Step 5: Rodar e ver passar.** Rode `./gradlew :app:testDebugUnitTest; echo "exit=$?"` e depois `./gradlew :app:assembleDebug; echo "exit=$?"`. Esperado: `exit=0` nos dois.
- [ ] **Step 6: Commit.** `git commit -m "feat(app): módulo Android em paisagem e tela cheia"`

---

### Task 2: Desenho das cartas

**Files:**
- Create: `ui/cards/CardFace.kt`
- Test: `ui/cards/CardFaceTest.kt` (JVM), `ui/cards/CardScreenshotTest.kt` (Roborazzi)

**Interfaces:**
- Consumes: `Card`, `Suit`, `CardColor` (core)
- Produces:
  - `fun rankLabel(rank: Int): String`: 1 → "A", de 2 a 10 → o número, 11 → "J", 12 → "Q", 13 → "K".
  - `fun suitSymbol(suit: Suit): String`: ♥ ♦ ♠ ♣
  - `@Composable fun CardFace(card: Card, modifier: Modifier = Modifier)`: fundo branco, cantos com 8% da largura, borda cinza `#9A9A9A`. No canto superior esquerdo, o valor e o naipe empilhados, cada um com 26% da largura da carta. No centro, o naipe grande (de A a 10, com 45% da largura) ou a letra grande com o naipe (J, Q e K, com 42%). Cor `CardRed` ou `CardBlack`.
  - `@Composable fun CardBack(modifier: Modifier = Modifier)`: listras diagonais `BackRed`/`BackRedLight` e moldura branca de 6% da largura.
  - `@Composable fun EmptySlot(label: String, modifier: Modifier = Modifier)`: contorno `SlotLine` de 2 dp, fundo preto com 8% de opacidade e `label` ("A" ou "K") centralizado em `SlotLine`.
  - Todas as três desenham na proporção altura = 1,4 × largura, que vem do `modifier`.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
class CardFaceTest {
    @Test fun `rank labels`() {
        assertEquals(listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K"), (1..13).map(::rankLabel))
    }
    @Test fun `suit symbols`() { assertEquals("♥♦♠♣", Suit.entries.joinToString("") { suitSymbol(it) }) }
}
// CardScreenshotTest (Robolectric + Roborazzi, qualifiers "w869dp-h411dp-land-xxhdpi"):
// renderiza numa Row: A♥, 10♣, Q♦, K♠, CardBack, EmptySlot("A"), EmptySlot("K"), largura de 56 dp cada,
// sobre feltBackground; captureRoboImage("cards.png")
```

- [ ] **Step 2: Rodar e ver falhar.** Esperado: erro de compilação.
- [ ] **Step 3: Implementar `CardFace.kt`.**
- [ ] **Step 4: Gravar e conferir o print.** Rode `./gradlew :app:recordRoborazziDebug; echo "exit=$?"` e **abra** `app/build/outputs/roborazzi/cards.png` com a ferramenta Read. Confira que os cantos são legíveis e as cores corretas. Depois rode `./gradlew :app:testDebugUnitTest; echo "exit=$?"`. Esperado: `exit=0`.
- [ ] **Step 5: Commit.** Inclua o PNG de referência que o Roborazzi grava em `app/src/test/snapshots` (configure `roborazzi { outputDir.set(file("src/test/snapshots")) }`). `git commit -m "feat(app): desenho das cartas"`

---

### Task 3: Geometria da mesa (responsiva)

**Files:**
- Create: `ui/game/TableLayout.kt`
- Test: `ui/game/TableLayoutTest.kt` (JVM puro)

**Interfaces:**
- Consumes: `GameState`, `PileRef` (core)
- Produces:
  - `data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) { fun contains(px: Float, py: Float): Boolean }` (em dp)
  - `data class ColumnSteps(val faceDown: Float, val faceUp: Float)`
  - `data class TableGeometry(val cardW: Float, val cardH: Float, val stock: Rect, val waste: Rect, val wasteFanStep: Float, val foundations: List<Rect>, val columns: List<Rect>, val steps: List<ColumnSteps>, val tableauTop: Float, val tableauContentHeight: Float, val scrolls: Boolean)`. `columns[i]` é o retângulo do topo da coluna (a posição da primeira carta). Em `steps`, há um item por coluna.
  - `object TableLayout { const val TOP_BAR = 40f; const val MARGIN = 8f; const val GAP_RATIO = 0.10f; const val ASPECT = 1.4f; const val MAX_CARD_W = 96f; const val DOWN_COMFY = 0.15f; const val UP_COMFY = 0.30f; const val DOWN_MIN = 4f; const val UP_MIN = 18f; fun compute(widthDp: Float, heightDp: Float, state: GameState): TableGeometry; fun cardTop(g: TableGeometry, column: Int, indexInColumn: Int, state: GameState): Float; fun hitTest(g: TableGeometry, state: GameState, x: Float, y: Float, scrollY: Float = 0f): PileRef? }`

Regras (spec 6.2):
- **Largura da carta:** `w = min(largura, altura, MAX_CARD_W)`, onde:
  - `largura = (W − 2·MARGIN) / (14.5 + 13·GAP_RATIO)`. A fileira de cima tem monte, descarte em leque (1,5 carta) e 12 fundações.
  - `altura = ((H − TOP_BAR − 3·MARGIN) / (2 + 4·UP_COMFY)) / ASPECT`, para caber uma carta na fileira de cima e uma carta mais 4 tiras confortáveis nas colunas.
  - `h = w·ASPECT` e `gap = w·GAP_RATIO`.
- **Fileira de cima:** começa em `y = TOP_BAR + MARGIN`.
  - O monte fica em `x = MARGIN`, e o descarte, em `x = MARGIN + w + gap`, com largura `1.5·w` e `wasteFanStep = 0.25·w`.
  - As fundações começam em `MARGIN + 2.5·w + 2·gap` e seguem com passo `w + gap`.
  - Todo o conjunto fica centralizado na largura.
- **Colunas:** 13, com passo `w + gap`, centralizadas, começando em `tableauTop = y da fileira de cima + h + MARGIN`.
- **Sobreposição por coluna**, com `A = H − tableauTop − MARGIN`, `d` cartas viradas para baixo e `u` cartas viradas para cima:
  - `necessário(sd, su) = h + d·sd + max(0, u − 1)·su`
  - Tente `sd = DOWN_COMFY·h` e `su = UP_COMFY·h`.
  - Se não couber, reduza `sd` até `DOWN_MIN`.
  - Se ainda não couber, reduza `su` até `UP_MIN`.
  - Se ainda não couber, fique nos mínimos, marque `scrolls = true` e faça `tableauContentHeight` igual à maior coluna.
- **`hitTest`:** devolve a pilha cujo retângulo contém o ponto.
  - Para uma coluna, o retângulo vai do topo até a última carta mais `h`.
  - Pontos na área das colunas são ajustados por `scrollY`.
  - Uma coluna vazia usa o retângulo de uma carta.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
class TableLayoutTest {
    private val note10 = 869f to 411f
    private val small = 640f to 360f
    private val tablet = 1280f to 800f
    @Test fun `new game on a Galaxy Note 10 fits comfortably`() {
        val g = TableLayout.compute(869f, 411f, Deal.newGame(1, DrawMode.ONE))
        assertTrue(g.cardW in 54f..57f)                       // ~55.7 dp
        assertEquals(12, g.foundations.size); assertEquals(13, g.columns.size)
        assertTrue(g.foundations.last().let { it.x + it.w } <= 869f - TableLayout.MARGIN + 0.5f)
        assertFalse(g.scrolls)
        assertTrue(g.steps.all { it.faceUp == TableLayout.UP_COMFY * g.cardH })
    }
    @Test fun `top row and columns never exceed the width on any screen`() { /* note10, small, tablet */ }
    @Test fun `tablet cards are capped`() { assertEquals(TableLayout.MAX_CARD_W, TableLayout.compute(1280f, 800f, Deal.newGame(1, DrawMode.ONE)).cardW) }
    @Test fun `long column on a small phone compresses then scrolls`() {
        // estado com a coluna 0 = 12 viradas para baixo + 13 viradas para cima (K..A alternando)
        val g = TableLayout.compute(640f, 360f, longColumnState())
        assertEquals(TableLayout.DOWN_MIN, g.steps[0].faceDown)
        assertEquals(TableLayout.UP_MIN, g.steps[0].faceUp)
        assertTrue(g.scrolls)
        assertTrue(g.tableauContentHeight > 360f - g.tableauTop)
    }
    @Test fun `medium column compresses face-down cards first`() {
        // 12 viradas para baixo + 5 para cima no Note 10: faceUp continua confortável, faceDown < DOWN_COMFY·h
    }
    @Test fun `hitTest finds stock, waste, a foundation and a column`()
    @Test fun `hitTest outside every pile is null`()
    @Test fun `hitTest on the area below a short column still hits that column only within its cards`()
}
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar `TableLayout`.**
- [ ] **Step 4: Rodar e ver passar.** `./gradlew :app:testDebugUnitTest; echo "exit=$?"`. Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): geometria responsiva da mesa"`

---

### Task 4: Salvamento e opções

**Files:**
- Create: `data/GameStore.kt`, `data/SettingsStore.kt`
- Test: `data/FileGameStoreTest.kt` (JVM, pasta temporária), `data/SettingsStoreTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `Game`, `GameCodec` (core)
- Produces:
  - `data class SavedGame(val game: Game, val elapsedMs: Long)`
  - `interface GameStore { suspend fun load(): SavedGame?; suspend fun save(saved: SavedGame); suspend fun clear() }`
  - `class FileGameStore(private val dir: File) : GameStore`. Grava `save.json` no formato `@Serializable data class SaveFile(val version: Int = 1, val elapsedMs: Long, val game: String)`, em que `game` é o texto de `GameCodec.encode`.
    - A escrita é atômica: grava em `save.json.tmp` e depois renomeia.
    - `load` devolve `null` se o arquivo não existir, estiver corrompido ou se `GameCodec.decode` devolver `null`.
    - Tudo roda em `Dispatchers.IO`.
  - `enum class AnimationSpeed(val moveMillis: Int) { NORMAL(220), FAST(110), OFF(0) }`
  - `data class Settings(val defaultDrawMode: DrawMode = DrawMode.ONE, val soundOn: Boolean = true, val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL)`
  - `interface SettingsStore { val settings: Flow<Settings>; suspend fun update(transform: (Settings) -> Settings) }` e `class DataStoreSettingsStore(context: Context) : SettingsStore`, com as chaves `draw_mode`, `sound_on` e `animation_speed`.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
@Test fun `save then load returns the same game and time`()
@Test fun `load with no file is null`()
@Test fun `corrupted file loads as null and does not throw`()
@Test fun `clear removes the save`()
@Test fun `a leftover tmp file does not break load`()
// SettingsStoreTest
@Test fun `defaults are draw one, sound on, normal speed`()
@Test fun `update persists across a new store instance`()
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): salvamento da partida e opções"`

---

### Task 5: `GameViewModel`: jogar, desfazer, cronômetro e salvar

**Files:**
- Create: `ui/game/GameUiState.kt`, `ui/game/GameViewModel.kt`
- Test: `ui/game/GameViewModelTest.kt` (JVM + `kotlinx-coroutines-test`, com `FakeGameStore` e `FakeSettingsStore` em memória)

**Interfaces:**
- Consumes: `Game`, `Move`, `PileRef`, `TapToMove`, `Rules` (core); `GameStore`, `SettingsStore` (Task 4)
- Produces:
  - `data class GameUiState(val game: Game? = null, val elapsedMs: Long = 0, val hint: Hint? = null, val canFinish: Boolean = false, val finishing: Boolean = false, val won: Boolean = false, val stuck: Boolean = false, val hasSavedGame: Boolean = false)`
  - `sealed interface SoundEvent { data object Flip; data object Place; data object Foundation; data object Win }`
  - `class GameViewModel(private val store: GameStore, private val settings: SettingsStore, private val clock: () -> Long = SystemClock::elapsedRealtime) : ViewModel`, com:
    - `val state: StateFlow<GameUiState>` e `val sounds: SharedFlow<SoundEvent>`
    - `fun newGame(number: Int, mode: DrawMode)`, `fun continueSaved()`, `fun restart()`
    - `fun tapStock()`
    - `fun tapCard(from: PileRef, count: Int)`, que usa `TapToMove.bestMove`
    - `fun drop(from: PileRef, count: Int, to: PileRef?)`, que é `Move.Transfer`; um `to` nulo ou uma jogada ilegal não faz nada
    - `fun undo()`
    - `fun pause()` e `fun resume()`, chamados pela tela em `ON_STOP`/`ON_START` e ao abrir ou fechar o menu
    - `companion fun factory(store, settings): ViewModelProvider.Factory`
  - Ao ser criado, o ViewModel lê o `store` e preenche `hasSavedGame`.

Comportamento:
- **Toda jogada aceita:**
  - limpa `hint` e `stuck`;
  - atualiza `canFinish = AutoComplete.canFinish`;
  - emite um som: `Foundation` se o destino for uma fundação, `Flip` se uma carta foi virada (o número de cartas viradas para cima aumentou fora da carta movida) ou no `tapStock`, e `Place` nos outros casos;
  - chama `store.save(SavedGame(game, elapsedMs))`.
- **Jogada ilegal:** não muda nada e não salva.
- **Cronômetro:** só começa na primeira jogada aceita. `elapsedMs` soma o tempo entre `resume` e `pause` usando `clock`, e um tick de 1 s atualiza a barra enquanto está rodando.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
@Test fun `new game starts with no time running`()
@Test fun `tap on a card plays the best move and saves`()
@Test fun `tap on a card with no destination changes nothing and does not save`()
@Test fun `two quick taps on the same card play at most two legal moves`()
@Test fun `illegal drop changes nothing`()
@Test fun `drop onto a legal pile plays the move`()
@Test fun `undo goes back and saves`()
@Test fun `tapStock draws and recycles`()
@Test fun `timer counts only between resume and pause, after the first move`() // relógio falso: avança 5 s rodando, 60 s em pausa → 5 s
@Test fun `restores game and elapsed time from the store`() // novo ViewModel com o mesmo FakeGameStore → continueSaved() → mesmo state e elapsedMs
@Test fun `foundation move emits Foundation sound, column move emits Place`()
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): GameViewModel com jogadas, desfazer, cronômetro e salvamento"`

---

### Task 6: `GameViewModel`: dica, jogo travado, fim automático e vitória

**Files:**
- Modify: `ui/game/GameViewModel.kt`
- Test: `ui/game/GameViewModelAssistTest.kt`

**Interfaces:**
- Consumes: `Hints.all(state, lastMove)`, `AutoComplete` (core)
- Produces, no `GameViewModel`: `fun hint()`, `fun finish()`, `fun dismissStuck()`.
  - `hint()` mostra `Hints.all(state, game.moves.lastOrNull())[i % size]`, avançando `i` a cada toque. O índice volta a 0 após qualquer jogada.
  - Se a dica for `Hint.Stuck`, faz `stuck = true` (a tela mostra o diálogo "Sem jogadas disponíveis" com Desfazer, Reiniciar e Novo Jogo).
  - `finish()` faz `finishing = true` e aplica `AutoComplete.nextMove` em sequência, com intervalo de `settings.animationSpeed.moveMillis` (mínimo de 30 ms quando `OFF`), salvando a cada jogada.
  - **Vitória:** com `state.isWon`, o ViewModel para o cronômetro, faz `won = true`, emite `SoundEvent.Win`, chama `store.clear()` e faz `hasSavedGame = false`.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
@Test fun `hint cycles through hints and resets after a move`()
@Test fun `hint on a stuck game sets stuck`()
@Test fun `hint never suggests reversing the last move`()
@Test fun `finish plays all remaining cards and wins`() // a partir de um Game reconstruído por um snapshot quase vencido; avance o tempo virtual
@Test fun `win clears the save and hides Continuar`()
@Test fun `timer stops at the win and does not run in background`()
@Test fun `no moves are accepted after the win`()
```

  Para montar uma partida quase vencida, use `GameSnapshot` + `Game.fromSnapshot` com as jogadas de uma partida real. Uma forma é deixar um "jogador" de teste resolver uma seed fácil, com `Hints` e `AutoComplete`, e guardar o snapshot em `src/test/resources/near_win.json` (helper só de teste). Se nenhuma seed entre 1 e 2000 for resolvida pelo jogador simples, use um `FakeGameStore` com o `GameState` do `AutoCompleteTest` do core, expondo um construtor `internal` de teste. Registre a escolha no ledger.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): dica, jogo travado, fim automático e vitória"`

---

### Task 7: Tela de jogo (mesa, barra, toque, arrastar e animações)

**Files:**
- Create: `ui/game/GameScreen.kt`, `ui/game/TableView.kt`
- Test: `ui/game/GameScreenTest.kt` (Robolectric + Compose), `ui/game/GameScreenshotTest.kt` (Roborazzi)

**Interfaces:**
- Consumes: `GameViewModel` (Tasks 5 e 6), `TableLayout` (Task 3), `CardFace`/`CardBack`/`EmptySlot` (Task 2), `Settings.animationSpeed`
- Produces: `@Composable fun GameScreen(vm: GameViewModel, onOpenMenu: () -> Unit, onNewGame: () -> Unit, onExitToMenu: () -> Unit)`

Comportamento (spec 6.2, 6.3 e 6.5):
- **Barra superior** (`TOP_BAR` dp), em `TitleCream`: à esquerda, "#220 · Pontos 340 · Mov. 41 · 03:12", com o tempo em `mm:ss` ou `h:mm:ss`. À direita, os botões creme ↶ Desfazer, 💡 Dica e ☰ Menu.
- **Botão "Terminar":** botão dourado sobre a mesa, centralizado embaixo, visível só com `canFinish && !finishing`.
- **Cartas:** cada carta da mesa é desenhada uma vez, dentro de `key(card)`, com `Modifier.offset` animado (`animateOffsetAsState` com `tween(animationSpeed.moveMillis)`) para a posição dada pela geometria.
  - Ordem de desenho: as pilhas, de baixo para cima.
  - Monte: só `CardBack` no topo. Mostre a contagem pequena embaixo se for maior que 0. Monte vazio mostra `EmptySlot("↻")`.
  - Descarte: no vira 3, as 3 cartas do topo em leque (`wasteFanStep`); no vira 1, só a do topo.
  - Fundações vazias mostram `EmptySlot("A")` e colunas vazias, `EmptySlot("K")`.
- **Toque:**
  - numa carta virada para cima de uma coluna: `vm.tapCard(Tableau(i), count = cartas daquela até o topo)`;
  - no topo do descarte ou de uma fundação: `count = 1`;
  - no monte: `vm.tapStock()`;
  - numa carta virada para baixo: nada.
  - Se o toque não gerou jogada, a carta treme: animação curta de ±4 dp, 3 vezes.
- **Arrastar:** `detectDragGestures` numa carta virada para cima arrasta o grupo dela.
  - Ao soltar, `TableLayout.hitTest` no centro da carta de cima do grupo e `vm.drop(...)`.
  - Se o resultado não for uma jogada válida, o grupo volta animado.
  - Durante o arraste, o grupo é desenhado acima de tudo.
- **Dica:** contorno `HintOutline` de 3 dp nas cartas de origem e no retângulo de destino.
- **Diálogo de jogo travado:** "Sem jogadas disponíveis", com Desfazer, Reiniciar este jogo e Novo Jogo.
- **Ciclo de vida:** `LifecycleEventObserver` chama `pause()`/`resume()`. Abrir o menu da partida também pausa.
- **Área das colunas:** com `geometry.scrolls`, rola na vertical (`verticalScroll`), enquanto a barra e a fileira de cima ficam fixas.
- **Recorte da câmera:** a mesa usa `windowInsetsPadding(WindowInsets.displayCutout)`.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
// GameScreenTest (Robolectric, qualifiers "w869dp-h411dp-land-xxhdpi", ViewModel com fakes, jogo 1 vira 1)
@Test fun `top bar shows number, points and moves`()           // "#1", "Pontos 0", "Mov. 0"
@Test fun `tapping the stock draws a card`()                   // Mov. 1 depois do clique no nó com testTag("stock")
@Test fun `tapping the ace on column 5 sends it to a foundation`() // Pontos 15
@Test fun `undo button reverts the last move`()
@Test fun `hint button outlines the source card`()             // testTag("hint-source") existe
@Test fun `finish button appears only when the game can be finished`()
// GameScreenshotTest: jogo 1 e depois um estado com coluna longa, em três qualifiers:
//   "w869dp-h411dp-land-xxhdpi" (Note 10), "w640dp-h360dp-land-xhdpi" (pequeno), "w1280dp-h800dp-land-xhdpi" (tablet)
```

  Tags de teste para os nós: `stock`, `waste`, `foundation-<i>`, `column-<i>`, `card-<rank><suit><deck>`, `hint-source`, `hint-target`, `finish`, `undo`, `hint`, `menu`.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Gravar os prints e olhar cada um.** Rode `./gradlew :app:recordRoborazziDebug` e abra os 6 PNGs com a ferramenta Read. Confira que:
  - nada passa da tela;
  - os cantos das cartas são legíveis;
  - a barra não cobre a fileira de cima;
  - no estado de coluna longa do celular pequeno, a coluna mostra o canto de cada carta virada para cima.

  Se algum print mostrar defeito, corrija antes de seguir. Depois rode `./gradlew :app:testDebugUnitTest; echo "exit=$?"`. Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): tela de jogo com toque, arrastar e animações"`

---

### Task 8: Menu inicial, Novo Jogo e navegação

**Files:**
- Create: `ui/menu/MenuScreen.kt`, `ui/menu/NewGameDialog.kt`
- Modify: `App.kt` (Navigation 3 com `NavDisplay`, rotas `@Serializable data object Menu`, `Game`, `Rules` e `Settings`, e `GameViewModel` compartilhado criado no nível do `App`)
- Test: `ui/menu/MenuFlowTest.kt` (Robolectric), `ui/menu/MenuScreenshotTest.kt`

**Interfaces:**
- Consumes: `GameViewModel.state.hasSavedGame`, `newGame`, `continueSaved`; `SettingsStore`
- Produces:
  - `@Composable fun MenuScreen(hasSavedGame: Boolean, soundOn: Boolean, onContinue: () -> Unit, onNewGame: () -> Unit, onRules: () -> Unit, onSettings: () -> Unit, onToggleSound: () -> Unit)`
  - `@Composable fun NewGameDialog(defaultMode: DrawMode, gameInProgress: Boolean, onPlay: (number: Int, mode: DrawMode) -> Unit, onDismiss: () -> Unit)`
  - `fun parseGameNumber(text: String): Int?`, que devolve `null` fora de 1 a 99999 (função pura, testada)

Visual (rascunho aprovado, estilo A):
- `feltBackground`, com um leque de 4 cartas (A♥, K♠, Q♦, J♣ girados −24°, −8°, 8° e 24°, usando `CardFace`) e o título em serifa, `TitleCream`, com sombra.
- **▶ Continuar:** dourado, com sombra `GoldShadow`, só com `hasSavedGame`.
- **Novo Jogo** e **Como Jogar** lado a lado, mais **Opções**, todos creme com sombra `CreamShadow`.
- Ícones de som e de opções no canto superior direito, em círculos brancos com 18% de opacidade.
- Alvos de toque de pelo menos 48 dp.

**Novo Jogo:**
- Campo numérico preenchido com `Random.nextInt(1, 100000)` (o `kotlin.random` é permitido no `app`, só não no `core`).
- Botão 🎲 para sortear outro número.
- Rádio vira 1 (médio) ou vira 3 (difícil), começando em `defaultMode`.
- Botões Cancelar e Jogar. Jogar fica desabilitado se `parseGameNumber` devolver `null`.
- Com `gameInProgress`, Jogar pede confirmação: "Descartar a partida em andamento?" (Descartar/Voltar).

**Menu da partida** (☰ na tela de jogo): folha com Continuar, Novo Jogo, Reiniciar este jogo, Opções, Como Jogar e Sair para o menu. Fica em `GameScreen`, usando os callbacks.

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
@Test fun `parseGameNumber accepts 1 to 99999 only`()  // "1"→1, "99999"→99999, "0","100000","abc",""→null
@Test fun `menu hides Continuar without a saved game`()
@Test fun `new game from the menu opens the table`()    // clicar Novo Jogo → Jogar → nó "stock" existe
@Test fun `back to menu then Continuar restores the same game`()
@Test fun `new game over a game in progress asks for confirmation`()
// MenuScreenshotTest: com e sem Continuar, no Note 10
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Gravar os prints, olhar e rodar.** Esperado: o menu igual ao rascunho A aprovado e `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): menu inicial, novo jogo e navegação"`

---

### Task 9: Vitória, Como Jogar e Opções

**Files:**
- Create: `ui/game/WinOverlay.kt`, `ui/rules/RulesScreen.kt`, `ui/settings/SettingsScreen.kt`
- Test: `ui/WinRulesSettingsTest.kt` (Robolectric), mais prints dos três

**Interfaces:**
- Produces:
  - `@Composable fun WinOverlay(score: Int, moves: Int, elapsedMs: Long, gameNumber: Int, mode: DrawMode, animationSpeed: AnimationSpeed, onNewGame: () -> Unit, onMenu: () -> Unit)`
    - Animação: 24 cartas caindo e quicando da fileira das fundações (`rememberInfiniteTransition`), desligada se `animationSpeed == OFF`.
    - Painel creme central com "Você venceu!", pontos, movimentos, tempo, "Jogo #N · Vira 1/Vira 3" e os botões Novo Jogo e Menu.
  - `@Composable fun RulesScreen(onBack: () -> Unit)`: texto rolável com as regras da spec §5 em linguagem simples, mais o funcionamento do toque, do arrastar, da dica e do "Terminar". Tudo em `strings.xml`.
  - `@Composable fun SettingsScreen(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onBack: () -> Unit)`: rádio de modo padrão, chave de som e três opções de velocidade (Normal, Rápida, Desligada).

- [ ] **Step 1: Escrever os testes que falham.**

```kotlin
@Test fun `win overlay shows score, moves, time and game number`()
@Test fun `win overlay buttons call back`()
@Test fun `rules screen shows the empty column rule`()   // texto contém "Rei"
@Test fun `changing draw mode in settings is persisted`() // com DataStoreSettingsStore real no Robolectric
@Test fun `animation speed off is persisted`()
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar e ligar** (vitória em `GameScreen` quando `won`; rotas Rules e Settings no `App`).
- [ ] **Step 4: Gravar os prints, olhar e rodar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(app): vitória, como jogar e opções"`

---

### Task 10: Sons, créditos e verificação no emulador

**Files:**
- Create: `tools/gen_sounds.py`, `app/src/main/res/raw/{flip,place,foundation,win}.wav`, `audio/SoundPlayer.kt`, `docs/CREDITS.md`
- Modify: `App.kt` (coleta `vm.sounds` e toca, se `soundOn`)
- Test: `audio/SoundPlayerTest.kt` (Robolectric: carrega os 4 sons sem exceção; com som desligado, `play` não chama o `SoundPool`)

**Interfaces:**
- Produces: `class SoundPlayer(context: Context) { fun play(event: SoundEvent); fun release() }`, com `SoundPool` de 4 vozes e `AudioAttributes.USAGE_GAME`.

Sons gerados por `tools/gen_sounds.py` (biblioteca padrão do Python: `wave`, `math`, `random` com seed fixa), em 44,1 kHz, 16 bits, mono, com envelope de ataque de 2 ms e decaimento exponencial:

| Som | Como é feito | Duração |
|---|---|---|
| `flip` | ruído branco filtrado passa-baixa | 60 ms |
| `place` | ruído curto mais seno de 180 Hz | 80 ms |
| `foundation` | seno de 880 Hz mais 1320 Hz | 120 ms |
| `win` | arpejo de 523, 659, 784 e 1047 Hz, 120 ms cada | — |

- [ ] **Step 1: Gerar os sons.** `python -I tools/gen_sounds.py app/src/main/res/raw; echo "exit=$?"`. Esperado: 4 arquivos com menos de 200 KB no total.
- [ ] **Step 2: Escrever o teste que falha.** Rode e veja falhar.
- [ ] **Step 3: Implementar `SoundPlayer`, ligar no `App` e escrever o `CREDITS.md`.** O `CREDITS.md` diz que as cartas são desenhadas no código e os sons gerados por `tools/gen_sounds.py`, ambos do projeto, e lista as bibliotecas usadas com suas licenças (Apache 2.0).
- [ ] **Step 4: Rodar a suíte inteira.**
  - `./gradlew :core:test :app:testDebugUnitTest :app:verifyRoborazziDebug :app:assembleDebug; echo "exit=$?"`. Esperado: `exit=0`.
  - Veja uma checagem falhar de propósito: troque `UP_MIN` para 40 e confirme que `TableLayoutTest` falha. Depois desfaça a troca.
- [ ] **Step 5: Verificar no emulador** (skill `android-cli`, se disponível, ou `adb` direto):
  - Suba o AVD `Pixel_6` e instale com `./gradlew :app:installDebug`.
  - Gire o emulador para paisagem e abra o app.
  - Faça prints com `adb exec-out screencap -p > shot.png` do menu, de Novo Jogo, da mesa, de uma dica, de um desfazer e da mesa depois de "Sair para o menu" e "Continuar".
  - Abra cada print com Read e confira o mesmo checklist da Task 7.
  - Feche o app (`adb shell am kill`) no meio da partida, reabra e confirme que "Continuar" restaura a partida.
- [ ] **Step 6: Commit.** `git commit -m "feat(app): sons gerados, créditos e verificação no emulador"`

---

## Depois deste plano

- Pendências menores do Plano 1 (no PR #1): `Hints.all` com cálculo dobrado, nomes de pacotes na spec §7 e o teste de toque no meio de uma sequência inválida.
- Itens do `BACKLOG.md`: nome comercial e identificador antes de publicar, modo retrato, porte para iOS e monetização.
