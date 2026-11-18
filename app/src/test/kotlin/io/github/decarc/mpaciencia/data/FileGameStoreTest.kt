package io.github.decarc.mpaciencia.data

import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FileGameStoreTest {
    private val dir: File = Files.createTempDirectory("store").toFile()
    private val store = FileGameStore(dir)
    private val game = Game.new(220, DrawMode.THREE).play(Move.Draw)!!.play(Move.Draw)!!

    @Test
    fun `save then load returns the same game and time`() = runTest {
        store.save(SavedGame(game, 12_345))
        val loaded = store.load()!!
        assertEquals(game.state, loaded.game.state)
        assertEquals(game.moves, loaded.game.moves)
        assertEquals(12_345, loaded.elapsedMs)
    }

    @Test
    fun `load with no file is null`() = runTest {
        assertNull(store.load())
    }

    @Test
    fun `corrupted file loads as null and does not throw`() = runTest {
        File(dir, "save.json").writeText("{not json")
        assertNull(store.load())
        File(dir, "save.json").writeText("""{"version":1,"elapsedMs":5,"game":"{oops"}""")
        assertNull(store.load())
    }

    @Test
    fun `clear removes the save`() = runTest {
        store.save(SavedGame(game, 1))
        store.clear()
        assertNull(store.load())
        assertFalse(File(dir, "save.json").exists())
    }

    @Test
    fun `a leftover tmp file does not break load`() = runTest {
        store.save(SavedGame(game, 7))
        File(dir, "save.json.tmp").writeText("garbage")
        assertEquals(7, store.load()!!.elapsedMs)
        store.save(SavedGame(game, 8))
        assertEquals(8, store.load()!!.elapsedMs)
    }
}
