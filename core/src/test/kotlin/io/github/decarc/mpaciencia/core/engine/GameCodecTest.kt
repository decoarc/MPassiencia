package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.allLegalMoves
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.random.Pcg32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameCodecTest {
    @Test
    fun `encode then decode gives the same state`() {
        val rng = Pcg32(5u, 5u)
        var g = Game.new(220, DrawMode.THREE)
        repeat(30) {
            val options = allLegalMoves(g.state)
            g = g.play(options[rng.nextInt(options.size)])!!
        }
        val back = GameCodec.decode(GameCodec.encode(g))!!
        assertEquals(g.state, back.state)
        assertEquals(g.moves, back.moves)
    }

    @Test
    fun `corrupted json decodes to null`() {
        assertNull(GameCodec.decode("{oops"))
        assertNull(GameCodec.decode(""))
        assertNull(GameCodec.decode("""{"gameNumber":1}"""))
    }

    @Test
    fun `snapshot with an impossible move decodes to null`() {
        // reciclar com o monte cheio é impossível na primeira jogada
        val json = """{"version":1,"gameNumber":1,"drawMode":"ONE","moves":[{"type":"recycle"}]}"""
        assertNull(GameCodec.decode(json))
    }

    @Test
    fun `snapshot with game number 0 decodes to null`() {
        assertNull(GameCodec.decode("""{"version":1,"gameNumber":0,"drawMode":"ONE","moves":[]}"""))
    }

    @Test
    fun `unknown fields from a newer version are ignored`() {
        val json = """{"version":1,"gameNumber":7,"drawMode":"ONE","moves":[{"type":"draw"}],"elapsedMs":1234}"""
        assertEquals(1, GameCodec.decode(json)!!.moves.size)
    }
}
