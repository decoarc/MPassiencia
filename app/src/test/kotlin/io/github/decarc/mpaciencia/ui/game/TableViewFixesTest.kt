package io.github.decarc.mpaciencia.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.decarc.mpaciencia.core.engine.Deal
import io.github.decarc.mpaciencia.core.engine.GameCodec
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.core.model.TableauCard
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TableViewFixesTest {
    @get:Rule
    val rule = createComposeRule()

    private val base = Deal.newGame(1, DrawMode.ONE).copy(foundations = List(12) { emptyList() })

    private fun up(rank: Int, suit: Suit, deck: Int = 0) = TableauCard(Card(suit, rank, deck), true)
    private fun down(rank: Int, suit: Suit, deck: Int = 0) = TableauCard(Card(suit, rank, deck), false)

    private fun withColumns(vararg cols: List<TableauCard>): GameState =
        base.copy(tableau = cols.toList() + List(13 - cols.size) { emptyList() })

    @Test
    @Config(qualifiers = "w869dp-h411dp-land-mdpi")
    fun `drop uses the current table, not the one from when the card last changed`() {
        // Coluna 0: 8♣ (será arrastado; sua pilha não muda). Coluna 1: só o 10♠ no começo.
        val before = withColumns(listOf(up(8, Suit.CLUBS)), listOf(up(10, Suit.SPADES)))
        // Depois, a coluna 1 cresce: 10♠ + 9♥.
        val after = withColumns(listOf(up(8, Suit.CLUBS)), listOf(up(10, Suit.SPADES), up(9, Suit.HEARTS)))
        var state by mutableStateOf(before)
        val drops = mutableListOf<PileRef?>()
        rule.setContent {
            MPacienciaTheme {
                Box(Modifier.size(869.dp, 411.dp)) {
                    TableView(state, hint = null, moveMillis = 0, onTapStock = {}, onTapCard = { _, _ -> false },
                        onDrop = { _, _, to -> drops += to; false })
                }
            }
        }
        rule.waitForIdle()
        state = after
        rule.waitForIdle()

        val g = TableLayout.compute(869f, 411f, after)
        val from = g.columns[0]
        // Solta com o centro do 8♣ na parte de baixo do 9♥, abaixo de onde o 10♠ terminava antes.
        val nineTop = TableLayout.cardTop(g, 1, 1, after)
        val targetCenter = Offset(g.columns[1].x + g.cardW / 2, nineTop + g.cardH * 0.85f)
        val start = Offset(from.x + g.cardW / 2, from.y + g.cardH / 2)
        rule.onRoot().performTouchInput {
            down(Offset(start.x * density, start.y * density))
            moveTo(Offset((start.x + 20) * density, start.y * density))
            moveTo(Offset(targetCenter.x * density, targetCenter.y * density))
            up()
        }
        rule.waitForIdle()
        assertEquals(listOf<PileRef?>(PileRef.Tableau(1)), drops)
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    fun `scrolled columns never cover the top row`() {
        val longCol = List(12) { down((it % 13) + 1, Suit.CLUBS) } +
            List(13) { i -> up(13 - i, if (i % 2 == 0) Suit.SPADES else Suit.HEARTS, 1) }
        val state = withColumns(longCol).copy(waste = listOf(Card(Suit.DIAMONDS, 5, 2)))
        val taps = mutableListOf<PileRef>()
        rule.setContent {
            MPacienciaTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    TableView(state, hint = null, moveMillis = 0, onTapStock = {}, onTapCard = { p, _ -> taps += p; true },
                        onDrop = { _, _, _ -> false })
                }
            }
        }
        rule.waitForIdle()
        // Rola tudo para cima arrastando na área vazia à direita das colunas.
        rule.onRoot().performTouchInput {
            swipe(Offset(625f * density, 330f * density), Offset(625f * density, 150f * density), durationMillis = 300)
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/snapshots/game_small_scrolled.png")
        // Toque no topo do descarte: tem de ir para o descarte, não para uma carta da coluna rolada.
        val g = TableLayout.compute(640f, 360f, state)
        rule.onRoot().performTouchInput {
            click(Offset((g.waste.x + g.cardW / 2) * density, (g.waste.y + g.cardH * 0.7f) * density))
        }
        rule.waitForIdle()
        assertEquals(PileRef.Waste, taps.lastOrNull())
    }

    @Test
    @Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
    fun `taps on the win screen do not reach the table underneath`() {
        val json = javaClass.classLoader!!.getResource("near_win.json")!!.readText()
        val fast = FakeSettingsStore(io.github.decarc.mpaciencia.data.Settings(animationSpeed = io.github.decarc.mpaciencia.data.AnimationSpeed.OFF))
        val vm = GameViewModel(FakeGameStore(SavedGame(GameCodec.decode(json)!!, 0)), fast, FakeClock())
        vm.continueSaved()
        rule.setContent { MPacienciaTheme { GameScreen(vm, onOpenMenu = {}, onNewGame = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("finish").performClick()
        rule.waitUntil(10_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            vm.state.value.won
        }
        val moves = vm.state.value.game!!.moves.size
        var menuOpened = 0
        rule.onNodeWithTag("undo").performClick()
        rule.waitForIdle()
        assertEquals(moves, vm.state.value.game!!.moves.size)
        assertEquals(0, menuOpened)
    }

    @Test
    @Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
    fun `game entry with no saved game sends the player back to the menu`() {
        val vm = GameViewModel(FakeGameStore(), FakeSettingsStore(), FakeClock())
        var missing = 0
        rule.setContent { MPacienciaTheme { GameEntry(vm, onMissing = { missing++ }, onOpenMenu = {}, onNewGame = {}, onExitToMenu = {}) } }
        rule.waitUntil(5_000) { missing == 1 }
    }

    @Test
    @Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
    fun `game entry restores the saved game when the screen comes back first`() {
        val store = FakeGameStore(SavedGame(io.github.decarc.mpaciencia.core.engine.Game.new(1, DrawMode.ONE), 0))
        val vm = GameViewModel(store, FakeSettingsStore(), FakeClock())
        rule.setContent { MPacienciaTheme { GameEntry(vm, onMissing = {}, onOpenMenu = {}, onNewGame = {}, onExitToMenu = {}) } }
        rule.waitUntil(5_000) { vm.state.value.game != null }
        rule.onNodeWithTag("stock").assertExists()
    }
}
