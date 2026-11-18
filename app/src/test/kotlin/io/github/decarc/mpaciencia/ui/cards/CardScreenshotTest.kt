package io.github.decarc.mpaciencia.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import io.github.decarc.mpaciencia.ui.theme.feltBackground
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class CardScreenshotTest {
    @Test
    fun cards() {
        captureRoboImage("src/test/snapshots/cards.png") {
            MPacienciaTheme {
                Row(Modifier.feltBackground().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val w = Modifier.width(56.dp)
                    CardFace(Card(Suit.HEARTS, 1, 0), w)
                    CardFace(Card(Suit.CLUBS, 10, 0), w)
                    CardFace(Card(Suit.DIAMONDS, 12, 0), w)
                    CardFace(Card(Suit.SPADES, 13, 0), w)
                    CardBack(w)
                    EmptySlot("A", w)
                    EmptySlot("K", w)
                }
            }
        }
    }
}
