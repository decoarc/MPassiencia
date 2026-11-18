# Plano 1: motor do jogo (`core`) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o módulo `core` em Kotlin puro com todas as regras do Paciência Triplo (distribuição por seed, jogadas, desfazer, pontuação, toque para mover, dica, jogo travado, fim automático e salvamento), totalmente coberto por testes.

**Architecture:** O projeto Gradle tem um módulo `core` Kotlin/JVM, sem nenhuma dependência de Android. O `GameState` é imutável e o `Engine.apply` produz o próximo estado. A partida (`Game`) guarda a seed e a lista de jogadas, e o desfazer reconstrói o estado repetindo as jogadas desde a distribuição. O salvamento serializa só a seed, o modo e as jogadas. O módulo `app` (Android) é o Plano 2 e não faz parte deste plano.

**Tech Stack:** Kotlin (JVM, alvo 17), Gradle Kotlin DSL com version catalog, `kotlinx.serialization` JSON e `kotlin.test` com JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-17-paciencia-triplo-base-design.md` (seções 5, 6.1, 6.3, 6.4, 6.10 e 7). Regras: `docs/regras-de-referencia.md`.

## Global Constraints

- `core` não pode depender de Android. Só pode usar a biblioteca padrão do Kotlin e `kotlinx-serialization-json`.
- Não usar `java.util.Random`, `kotlin.random` nem nada de `java.*` na lógica do jogo. Isso mantém o código pronto para Kotlin Multiplatform.
- Pacote base do código: `io.github.decarc.mpaciencia.core`.
- 3 baralhos, 156 cartas, 13 colunas com 1 a 13 cartas, 12 fundações e 65 cartas no monte.
- Número do jogo de 1 a 99999. Modos: vira 1 e vira 3.
- Pontos: +10 por carta na fundação, +5 por carta virada para cima numa coluna, +5 do descarte para uma coluna, −15 da fundação para uma coluna. O total nunca fica abaixo de 0.
- Movimentos: cada jogada aceita conta 1, inclusive virar o monte e reciclar o descarte.
- Nenhum arquivo, comentário ou mensagem de commit cita o site de referência do jogo.
- Para verificar, rode `./gradlew :core:test` **sem** o prefixo `rtk` e confie no código de saída (`; echo "exit=$?"`).

## Review Focus

1. **Tocar num Rei que já está no fundo de uma coluna:** o toque para mover não pode mandá-lo para outra coluna vazia, porque isso não muda nada e o jogo ficaria trocando a carta de lugar. Teste na Task 6.
2. **Vira 3 com menos de 3 cartas no monte:** vira as que restam. Reciclar com o monte e o descarte vazios é jogada ilegal e não conta movimento. Teste na Task 4.
3. **Salvamento corrompido ou com jogada impossível:** `GameCodec.decode` devolve `null` e nunca lança exceção. Teste na Task 5.
4. **Muitas idas e voltas da fundação para a coluna:** a pontuação nunca fica negativa, e desfazer volta exatamente aos pontos de antes. Teste na Task 5.
5. **Tocar em carta virada para baixo, ou no meio de uma sequência inválida:** não é jogada legal, e o toque para mover devolve `null`. Teste nas Tasks 3 e 6.

---

## Estrutura de arquivos

```
settings.gradle.kts                 inclui :core
build.gradle.kts                    raiz (plugins apply false)
gradle/libs.versions.toml           versões
gradlew, gradlew.bat, gradle/wrapper/*
core/build.gradle.kts
core/src/main/kotlin/io/github/decarc/mpaciencia/core/
  model/Card.kt                     Suit, CardColor, Card, fullDeck()
  model/GameState.kt                TableauCard, DrawMode, GameState
  model/Move.kt                     PileRef, Move
  random/Pcg32.kt                   PRNG
  random/Shuffle.kt                 Fisher–Yates com Pcg32
  engine/Deal.kt                    newGame(gameNumber, drawMode)
  engine/Rules.kt                   isLegal
  engine/Engine.kt                  apply (estado + pontos + movimentos)
  engine/Game.kt                    Game, GameSnapshot, undo por replay
  engine/GameCodec.kt               JSON
  assist/TapToMove.kt               melhor destino
  assist/Hints.kt                   dica e jogo travado
  assist/AutoComplete.kt            fim automático
core/src/test/kotlin/io/github/decarc/mpaciencia/core/...  (um arquivo de teste por arquivo acima)
```

---

### Task 1: Projeto Gradle, módulo `core` e o baralho

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper do Gradle, `core/build.gradle.kts`
- Create: `core/src/main/kotlin/io/github/decarc/mpaciencia/core/model/Card.kt`
- Test: `core/src/test/kotlin/io/github/decarc/mpaciencia/core/model/CardTest.kt`

**Interfaces:**
- Produces:
  - `enum class CardColor { RED, BLACK }`
  - `@Serializable enum class Suit(val color: CardColor) { HEARTS(RED), DIAMONDS(RED), SPADES(BLACK), CLUBS(BLACK) }`
  - `@Serializable data class Card(val suit: Suit, val rank: Int, val deck: Int)`: `rank` vai de 1 (Ás) a 13 (Rei) e `deck` de 0 a 2. O construtor valida as faixas com `require`.
  - `fun fullDeck(): List<Card>`: 156 cartas na ordem deck → suit → rank.

- [ ] **Step 1: Montar o Gradle.** O Gradle não está instalado e o JDK 21 está no PATH. Gere o wrapper com a última versão estável do Gradle 9.x (baixe a distribuição oficial de `services.gradle.org` uma vez e rode `gradle wrapper`). Crie `settings.gradle.kts` com `rootProject.name = "MPaciencia"` e `include(":core")`. Em `libs.versions.toml`, coloque as versões estáveis atuais de Kotlin e `kotlinx-serialization-json`. `core/build.gradle.kts` aplica `kotlin("jvm")` e `kotlin("plugin.serialization")`, com `jvmToolchain(17)` (ou `jvmTarget` 17 rodando no JDK 21), e as dependências `kotlinx-serialization-json` e `testImplementation(kotlin("test"))`, com `useJUnitPlatform()`.

- [ ] **Step 2: Escrever o teste que falha**

```kotlin
class CardTest {
    @Test fun `full deck has 156 unique cards`() {
        val deck = fullDeck()
        assertEquals(156, deck.size)
        assertEquals(156, deck.toSet().size)
        assertEquals(12, deck.count { it.rank == 1 })          // 12 ases
        assertEquals(39, deck.count { it.suit == Suit.HEARTS }) // 13 x 3
    }
    @Test fun `suit colors`() {
        assertEquals(CardColor.RED, Suit.DIAMONDS.color)
        assertEquals(CardColor.BLACK, Suit.CLUBS.color)
    }
    @Test fun `invalid rank is rejected`() {
        assertFailsWith<IllegalArgumentException> { Card(Suit.SPADES, 14, 0) }
        assertFailsWith<IllegalArgumentException> { Card(Suit.SPADES, 1, 3) }
    }
}
```

- [ ] **Step 3: Rodar e ver falhar.** `./gradlew :core:test; echo "exit=$?"`. Esperado: erro de compilação (`fullDeck` não existe), `exit` diferente de 0.
- [ ] **Step 4: Implementar `Card.kt`.**
- [ ] **Step 5: Rodar e ver passar.** `./gradlew :core:test; echo "exit=$?"`. Esperado: `BUILD SUCCESSFUL`, `exit=0`.
- [ ] **Step 6: Commit.** Adicione `/build`, `.gradle/`, `*/build/` e `local.properties` ao `.gitignore`. `git add` dos arquivos de Gradle, de `core/` e do `.gitignore`, e depois `git commit -m "feat(core): projeto Gradle e modelo de cartas"`.

---

### Task 2: Gerador aleatório, embaralhamento e distribuição

**Files:**
- Create: `random/Pcg32.kt`, `random/Shuffle.kt`, `model/GameState.kt`, `engine/Deal.kt`
- Test: `random/Pcg32Test.kt`, `engine/DealTest.kt`

**Interfaces:**
- Consumes: `Card`, `fullDeck()` (Task 1)
- Produces:
  - `class Pcg32(seed: ULong, sequence: ULong) { fun nextUInt(): UInt; fun nextInt(bound: Int): Int }`
  - `fun <T> List<T>.shuffledWith(rng: Pcg32): List<T>`: Fisher–Yates, de `i = size-1` até 1, trocando com `rng.nextInt(i + 1)`.
  - `@Serializable data class TableauCard(val card: Card, val faceUp: Boolean)`
  - `@Serializable enum class DrawMode(val count: Int) { ONE(1), THREE(3) }`
  - `@Serializable data class GameState(val gameNumber: Int, val drawMode: DrawMode, val stock: List<Card>, val waste: List<Card>, val foundations: List<List<Card>>, val tableau: List<List<TableauCard>>, val score: Int = 0, val moves: Int = 0)`. Em todas as listas, o **último elemento é o topo**. Inclui `val isWon: Boolean get() = foundations.sumOf { it.size } == 156`.
  - `object Deal { const val MIN_GAME = 1; const val MAX_GAME = 99999; fun newGame(gameNumber: Int, drawMode: DrawMode): GameState }`

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
class Pcg32Test {
    // Vetor de referência do pcg32-demo do pcg-c: pcg32_srandom(42, 54)
    @Test fun `matches pcg32 reference vector`() {
        val rng = Pcg32(42u, 54u)
        val expected = listOf(0xa15c02b7u, 0x7b47f409u, 0xba1d3330u, 0x83d2f293u, 0xbfa4784bu, 0xcbed606eu)
        assertEquals(expected, List(6) { rng.nextUInt() })
    }
    @Test fun `bounded ints stay in range`() {
        val rng = Pcg32(7u, 1u)
        repeat(10_000) { assertTrue(rng.nextInt(13) in 0..12) }
    }
}

class DealTest {
    @Test fun `layout of a new game`() {
        val s = Deal.newGame(220, DrawMode.ONE)
        assertEquals((1..13).toList(), s.tableau.map { it.size })
        s.tableau.forEach { col ->
            assertTrue(col.last().faceUp)
            assertTrue(col.dropLast(1).none { it.faceUp })
        }
        assertEquals(65, s.stock.size)
        assertTrue(s.waste.isEmpty())
        assertEquals(12, s.foundations.size)
        assertTrue(s.foundations.all { it.isEmpty() })
        val all = s.stock + s.tableau.flatten().map { it.card }
        assertEquals(fullDeck().toSet(), all.toSet())
        assertEquals(0, s.score); assertEquals(0, s.moves)
    }
    @Test fun `same number gives same deal, different numbers differ`() {
        assertEquals(Deal.newGame(5, DrawMode.ONE), Deal.newGame(5, DrawMode.ONE))
        assertNotEquals(Deal.newGame(5, DrawMode.ONE).stock, Deal.newGame(6, DrawMode.ONE).stock)
    }
    @Test fun `draw mode does not change the deal`() {
        assertEquals(Deal.newGame(9, DrawMode.ONE).tableau, Deal.newGame(9, DrawMode.THREE).tableau)
    }
    @Test fun `game number outside 1-99999 is rejected`() {
        assertFailsWith<IllegalArgumentException> { Deal.newGame(0, DrawMode.ONE) }
        assertFailsWith<IllegalArgumentException> { Deal.newGame(100_000, DrawMode.ONE) }
    }
    @Test fun `golden deal for game 1 never changes`() {
        val s = Deal.newGame(1, DrawMode.ONE)
        // Step 4: preencha com os valores reais na primeira execução e não altere mais.
        assertEquals(GOLDEN_GAME1_TABLEAU_TOPS, s.tableau.map { it.last().card })
        assertEquals(GOLDEN_GAME1_STOCK_TOP3, s.stock.takeLast(3))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar.** `./gradlew :core:test; echo "exit=$?"`. Esperado: erro de compilação.
- [ ] **Step 3: Implementar.** O algoritmo do `Pcg32` é o do pcg-c (`pcg32_srandom_r` / `pcg32_random_r` / `pcg32_boundedrand_r`), com `ULong` e `UInt`:

```kotlin
class Pcg32(seed: ULong, sequence: ULong) {
    private var state = 0uL
    private val inc = (sequence shl 1) or 1uL
    init { nextUInt(); state += seed; nextUInt() }
    fun nextUInt(): UInt {
        val old = state
        state = old * 6364136223846793005uL + inc
        val xorshifted = (((old shr 18) xor old) shr 27).toUInt()
        val rot = (old shr 59).toInt()
        return (xorshifted shr rot) or (xorshifted shl ((-rot) and 31))
    }
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        val b = bound.toUInt()
        val threshold = (0u - b) % b
        while (true) { val r = nextUInt(); if (r >= threshold) return (r % b).toInt() }
    }
}
```

  `Deal.newGame`: `require(gameNumber in 1..99999)`. Embaralha `fullDeck()` com `Pcg32(seed = gameNumber.toULong(), sequence = 0x5041434Eu /* "PACN" */)`. A coluna `i` (de 0 a 12) recebe as próximas `i + 1` cartas da lista embaralhada, na ordem, e só a última fica virada para cima. As 65 restantes formam o `stock`, na ordem da lista.
- [ ] **Step 4: Fixar o golden.** Rode uma vez, copie os valores reais das 13 cartas do topo das colunas e das 3 do topo do monte para as constantes `GOLDEN_*` no teste e rode de novo. Esperado: `exit=0`. Se o teste do vetor de referência do PCG falhar, confira a implementação contra o pcg-c antes de mexer no vetor.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): PRNG PCG32 e distribuição por número do jogo"`

---

### Task 3: Jogadas entre pilhas (regras e aplicação)

**Files:**
- Create: `model/Move.kt`, `engine/Rules.kt`, `engine/Engine.kt`
- Test: `engine/RulesTest.kt`, `engine/EngineTransferTest.kt`, mais um helper de testes `TestStates.kt` para montar `GameState` à mão

**Interfaces:**
- Consumes: `GameState`, `TableauCard`, `Card` (Tasks 1 e 2)
- Produces:
  - `@Serializable sealed interface PileRef` com `data object Stock`, `data object Waste`, `data class Foundation(val index: Int)` e `data class Tableau(val index: Int)`, cada um com `@SerialName` curto (`"s"`, `"w"`, `"f"`, `"t"`).
  - `@Serializable sealed interface Move` com `data object Draw`, `data object Recycle` e `data class Transfer(val from: PileRef, val to: PileRef, val count: Int = 1)`. Em `count`, as cartas são tiradas do topo de `from`.
  - `object Rules { fun isLegal(state: GameState, move: Move): Boolean }`
  - `object Engine { fun apply(state: GameState, move: Move): GameState? }`, que devolve `null` se a jogada for ilegal.
  - `object Scoring { const val TO_FOUNDATION = 10; const val FLIP = 5; const val WASTE_TO_TABLEAU = 5; const val FOUNDATION_TO_TABLEAU = -15 }`

Regras de `Transfer`:
- Origem `Waste` ou `Foundation`: `count == 1` e a pilha não está vazia.
- Origem `Tableau`: as `count` cartas do topo estão viradas para cima e formam uma sequência decrescente com cores alternadas.
- Destino `Foundation`: `count == 1`. Fundação vazia aceita só Ás. Senão, o naipe tem de ser o mesmo e o valor, um acima do topo.
- Destino `Tableau`: coluna vazia aceita só um grupo que comece com Rei. Senão, o topo está virado para cima, tem cor diferente e o valor é o da primeira carta do grupo + 1.
- Ilegal: `from == to`, `Foundation` → `Foundation`, qualquer `Transfer` com `Stock` e qualquer jogada para `Waste` ou `Stock`.
- Índices fora da faixa e `count < 1` são ilegais (devolvem `false`/`null`, nunca exceção).

Efeitos em `apply`: se a coluna de origem ficar com uma carta virada para baixo no topo, ela vira para cima (`+FLIP`). Os pontos seguem a tabela. `score = max(0, score + delta)`. `moves + 1`.

- [ ] **Step 1: Escrever os testes que falham.** Um teste para cada regra acima (legal e ilegal), mais estes:

```kotlin
@Test fun `group move flips the exposed card and scores`() {
    // coluna 0: [7♣ virada para baixo, 9♥, 8♠]; coluna 1: [10♣]
    val s = state(tableau = listOf(col(down(7, CLUBS), up(9, HEARTS), up(8, SPADES)), col(up(10, CLUBS))))
    val r = Engine.apply(s, Move.Transfer(PileRef.Tableau(0), PileRef.Tableau(1), count = 2))!!
    assertEquals(listOf(10, 9, 8), r.tableau[1].map { it.card.rank })
    assertTrue(r.tableau[0].single().faceUp)
    assertEquals(5, r.score); assertEquals(1, r.moves)
}
@Test fun `face-down card cannot be moved`() { /* count incluindo carta virada para baixo → isLegal false */ }
@Test fun `broken sequence cannot move as group`() { /* 9♥ sobre 8♥ → count=2 ilegal */ }
@Test fun `any empty foundation takes any ace`() { /* Ás de espadas para Foundation(7) vazia */ }
@Test fun `foundation to tableau costs 15 but score never below 0`() {
    val s = state(score = 10, foundations = foundationWith(0, ace(SPADES), two(SPADES)), tableau = listOf(col(up(3, HEARTS))))
    val r = Engine.apply(s, Move.Transfer(PileRef.Foundation(0), PileRef.Tableau(0)))!!
    assertEquals(0, r.score)
}
@Test fun `out of range index is illegal, not a crash`() {
    assertNull(Engine.apply(Deal.newGame(1, DrawMode.ONE), Move.Transfer(PileRef.Tableau(13), PileRef.Tableau(0))))
}
```

- [ ] **Step 2: Rodar e ver falhar.** `./gradlew :core:test; echo "exit=$?"`. Esperado: erro de compilação.
- [ ] **Step 3: Implementar `Move.kt`, `Rules.isLegal`, `Scoring` e `Engine.apply`** só para `Transfer`. `Draw` e `Recycle` devolvem `null` por enquanto.
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): regras e aplicação de jogadas entre pilhas"`

---

### Task 4: Monte e descarte

**Files:**
- Modify: `engine/Rules.kt`, `engine/Engine.kt`
- Test: `engine/EngineStockTest.kt`

**Interfaces:**
- Consumes/Produces: os mesmos `Move.Draw` e `Move.Recycle` da Task 3.

Regras:
- `Draw` exige monte não vazio. Tira `min(drawMode.count, stock.size)` cartas, **uma por vez**, do topo do monte para o topo do descarte. No vira 3, a terceira carta tirada fica no topo.
- `Recycle` exige monte vazio **e** descarte não vazio. `stock = waste.reversed()` e `waste = []`, para a próxima passada sair na mesma ordem da primeira.
- As duas somam 1 movimento e 0 pontos.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `draw one moves stock top to waste`()
@Test fun `draw three puts the third drawn card on top`() {
    val s = state(drawMode = DrawMode.THREE, stock = listOf(c1, c2, c3, c4)) // c4 = topo
    val r = Engine.apply(s, Move.Draw)!!
    assertEquals(listOf(c4, c3, c2), r.waste); assertEquals(listOf(c1), r.stock)
}
@Test fun `draw three with two cards left draws two`()
@Test fun `recycle restores original order`() {
    var s = Deal.newGame(3, DrawMode.ONE); val original = s.stock
    repeat(65) { s = Engine.apply(s, Move.Draw)!! }
    s = Engine.apply(s, Move.Recycle)!!
    assertEquals(original, s.stock); assertTrue(s.waste.isEmpty())
    assertEquals(66, s.moves) // 65 viradas + 1 reciclagem
}
@Test fun `recycle with empty stock and empty waste is illegal`() { assertNull(...) }
@Test fun `draw with empty stock is illegal`() { assertNull(...) }
@Test fun `recycle with cards still in stock is illegal`() { assertNull(...) }
```- [ ] **Step 2: Rodar e ver falhar.** Esperado: os testes novos falham porque `apply` devolve `null`.
- [ ] **Step 3: Implementar `Draw` e `Recycle`** em `Rules` e `Engine`.
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): virar monte e reciclar descarte"`

---

### Task 5: Partida com desfazer e salvamento

**Files:**
- Create: `engine/Game.kt`, `engine/GameCodec.kt`
- Test: `engine/GameTest.kt`, `engine/GameCodecTest.kt`

**Interfaces:**
- Consumes: `Deal.newGame`, `Engine.apply`, `Move`
- Produces:
  - `@Serializable data class GameSnapshot(val version: Int = 1, val gameNumber: Int, val drawMode: DrawMode, val moves: List<Move>)`
  - `class Game private constructor(val gameNumber: Int, val drawMode: DrawMode, val moves: List<Move>, val state: GameState)`, com:
    - `companion fun new(gameNumber: Int, drawMode: DrawMode): Game`
    - `companion fun fromSnapshot(s: GameSnapshot): Game?`: repete as jogadas e devolve `null` se alguma for ilegal ou o número for inválido.
    - `fun play(move: Move): Game?`: estado incremental, sem repetir as jogadas.
    - `fun undo(): Game?`: repete `moves.dropLast(1)` desde a distribuição. Devolve `null` se não houver jogadas.
    - `val canUndo: Boolean`, `fun restart(): Game` e `fun snapshot(): GameSnapshot`
  - `object GameCodec { fun encode(game: Game): String; fun decode(json: String): Game? }`, com `Json { ignoreUnknownKeys = true }`. `decode` captura qualquer exceção e devolve `null`.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `undo returns exactly the previous state including score and moves`()
@Test fun `undo restores an automatically flipped card`()
@Test fun `undo after recycle restores stock and waste`()
@Test fun `undo on a fresh game returns null`()
@Test fun `score clamp is replayed exactly on undo`() {
    // sobe para 10, desce para 0 (clamp), sobe para 10; desfazer volta para 0 e depois para 10
}
@Test fun `illegal play returns null and keeps the game unchanged`()
@Test fun `restart keeps number and mode with no moves`()
// GameCodecTest
@Test fun `encode then decode gives the same state`() { /* 30 jogadas legais quaisquer */ }
@Test fun `corrupted json decodes to null`() { assertNull(GameCodec.decode("{oops")) }
@Test fun `snapshot with an impossible move decodes to null`()
@Test fun `snapshot with game number 0 decodes to null`()
```

- [ ] **Step 2: Rodar e ver falhar.** Esperado: erro de compilação.
- [ ] **Step 3: Implementar `Game` e `GameCodec`.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): partida com desfazer e salvamento em JSON"`

---

### Task 6: Toque para mover

**Files:**
- Create: `assist/TapToMove.kt`
- Test: `assist/TapToMoveTest.kt`

**Interfaces:**
- Consumes: `Rules.isLegal`, `Move.Transfer`, `PileRef`
- Produces: `object TapToMove { fun bestMove(state: GameState, from: PileRef, count: Int = 1): Move.Transfer? }`. Na interface, `count` é a quantidade de cartas desde a carta tocada até o topo da coluna.

Prioridade (spec 6.3):
1. Se `count == 1` e a origem não é uma fundação: uma fundação legal. Primeiro a que já tem cartas do naipe; para um Ás, a primeira fundação vazia, da esquerda para a direita.
2. Uma coluna não vazia legal. Entre várias, a primeira da esquerda para a direita a partir da coluna de origem, para ser previsível.
3. Uma coluna vazia, só se o grupo começar com Rei **e** não for o grupo inteiro de uma coluna (um Rei no fundo de coluna não se move para outra coluna vazia).
4. Nenhum destino: devolve `null`.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `single card prefers foundation over tableau`()
@Test fun `ace goes to the leftmost empty foundation`()
@Test fun `group goes to a legal column, never to a foundation`()
@Test fun `king at the bottom of a column is not moved to another empty column`() {
    // coluna 0 = [K♠, Q♥] (todas viradas para cima), coluna 5 vazia → bestMove(Tableau(0), count = 2) == null
}
@Test fun `king with cards under it can go to an empty column`()
@Test fun `tapping a face-down card returns null`()
@Test fun `waste top goes to foundation when possible`()
@Test fun `foundation top can go back to a column`()
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): toque para mover"`

---

### Task 7: Dica e jogo travado

**Files:**
- Create: `assist/Hints.kt`
- Test: `assist/HintsTest.kt`

**Interfaces:**
- Consumes: `Rules`, `Engine`, `TapToMove`
- Produces:
  - `sealed interface Hint { data class Play(val move: Move.Transfer) : Hint; data object DrawStock : Hint; data object Recycle : Hint; data object Stuck : Hint }`
  - `object Hints { fun all(state: GameState): List<Hint>; fun isStuck(state: GameState): Boolean }`. `all` devolve as jogadas úteis em ordem de prioridade. Se não houver nenhuma `Play`, devolve `[DrawStock]` ou `[Recycle]`, conforme o monte, ou `[Stuck]` quando `isStuck` for verdadeiro. A interface mostra `all[i % size]` a cada toque em Dica.

**Jogada útil** (critérios da dica ficam a cargo de quem implementa, spec §10, desde que respeitem isto):
- Para a fundação: sempre útil.
- Entre colunas: útil se vira uma carta virada para baixo, esvazia uma coluna para um Rei que está esperando, ou libera uma carta que pode ir para a fundação. Mover um grupo entre duas colunas sem ganhar nada não é útil. Um Rei no fundo de coluna para outra coluna vazia nunca é útil.
- Do descarte para uma coluna: útil.
- Da fundação para uma coluna: só se permitir outra jogada útil logo em seguida.

**`isStuck`** (spec 6.3): verdadeiro quando não há jogada útil na mesa **e**, simulando uma passada completa do monte e do descarte com `Draw` e `Recycle` a partir do estado atual (até a configuração de monte e descarte se repetir), nenhuma carta que chega ao topo do descarte tem um `Transfer` legal para uma fundação ou coluna.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `foundation move is hinted first`()
@Test fun `move that reveals a face-down card is hinted`()
@Test fun `pointless column-to-column shuffle is not hinted`()
@Test fun `no table moves but stock has cards hints DrawStock`()
@Test fun `no table moves, empty stock, playable card in waste hints Recycle`()
@Test fun `stuck when no card of a full stock pass can ever be played`()
@Test fun `not stuck when a card deep in the stock becomes playable in draw one`()
@Test fun `in draw three a card unreachable in the cycle does not prevent stuck`()
@Test fun `won game is not reported as stuck`()
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar.** Esperado: `exit=0`.
- [ ] **Step 5: Commit.** `git commit -m "feat(core): dica e detecção de jogo travado"`

---

### Task 8: Fim automático, vitória e robustez

**Files:**
- Create: `assist/AutoComplete.kt`
- Test: `assist/AutoCompleteTest.kt`, `engine/RandomPlayTest.kt`

**Interfaces:**
- Consumes: `Game`, `Engine`, `TapToMove`, `Hints`
- Produces: `object AutoComplete { fun canFinish(state: GameState): Boolean; fun nextMove(state: GameState): Move.Transfer? }`
  - `canFinish`: monte vazio, descarte vazio, todas as cartas das colunas viradas para cima e o jogo ainda não vencido.
  - `nextMove`: a carta de menor valor, entre os topos das colunas, que tenha destino legal numa fundação (empate: a coluna mais à esquerda). A interface chama até `isWon`.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `canFinish is false while stock or waste has cards`()
@Test fun `canFinish is false while any tableau card is face down`()
@Test fun `repeated nextMove wins the game`() {
    // estado com todas as cartas viradas em colunas válidas → aplicar nextMove até null; isWon == true, score cresce 10 por carta
}
// RandomPlayTest: jogador automático simples, determinístico com Pcg32
@Test fun `random legal play never breaks invariants`() {
    for (n in listOf(1, 2, 3, 220, 99999)) for (mode in DrawMode.entries) {
        var g = Game.new(n, mode); val rng = Pcg32(n.toULong(), 7u)
        repeat(1500) {
            val options = Hints.all(g.state).filterIsInstance<Hint.Play>().map { it.move } +
                listOfNotNull(Move.Draw.takeIf { Rules.isLegal(g.state, it) }, Move.Recycle.takeIf { Rules.isLegal(g.state, it) })
            if (options.isEmpty()) return@repeat
            g = g.play(options[rng.nextInt(options.size)])!!
            if (rng.nextInt(10) == 0) g = g.undo() ?: g
            assertInvariants(g.state) // 156 cartas únicas; fundações em ordem; topos de coluna virados para cima; score >= 0
        }
    }
}
```

- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar `AutoComplete` e `assertInvariants`** (este último é helper de teste).
- [ ] **Step 4: Rodar a suíte inteira.** `./gradlew :core:test; echo "exit=$?"`. Esperado: `exit=0`.
- [ ] **Step 5: Ver uma checagem falhar de propósito (regra do projeto).** Quebre temporariamente uma regra (por exemplo, aceite qualquer carta na fundação), rode, confirme que `RandomPlayTest` ou `RulesTest` falha com `exit` diferente de 0 e desfaça a quebra.
- [ ] **Step 6: Confirmar que o `core` não depende de Android.** `./gradlew :core:dependencies --configuration runtimeClasspath`. Esperado: só `kotlin-stdlib` e `kotlinx-serialization`.
- [ ] **Step 7: Commit.** `git commit -m "feat(core): fim automático, vitória e teste de robustez"`

---

## Próximo plano

**Plano 2: app Android.** Cobre o módulo `app` em Compose: menu de feltro verde, mesa responsiva com as regras de sobreposição da spec 6.2, toque e arrastar, animações, vitória, opções, salvamento com `GameCodec`, sons, tela cheia e testes de interface. Será escrito depois que este plano estiver concluído, a partir das interfaces reais do `core`.
