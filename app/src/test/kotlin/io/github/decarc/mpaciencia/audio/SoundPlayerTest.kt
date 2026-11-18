package io.github.decarc.mpaciencia.audio

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.ui.game.SoundEvent
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SoundPlayerTest {
    private class FakeBackend : SoundBackend {
        val loaded = mutableListOf<Int>()
        val played = mutableListOf<Int>()
        override fun load(resId: Int): Int = (loaded.size + 1).also { loaded += resId }
        override fun play(soundId: Int) {
            played += soundId
        }
        override fun release() {}
    }

    @Test
    fun `loads the four generated sounds`() {
        val backend = FakeBackend()
        SoundPlayer(backend) { true }
        assertEquals(setOf(R.raw.flip, R.raw.place, R.raw.foundation, R.raw.win), backend.loaded.toSet())
    }

    @Test
    fun `plays the matching sound when sound is on`() {
        val backend = FakeBackend()
        val player = SoundPlayer(backend) { true }
        player.play(SoundEvent.Win)
        assertEquals(listOf(backend.loaded.indexOf(R.raw.win) + 1), backend.played)
    }

    @Test
    fun `plays nothing when sound is off`() {
        val backend = FakeBackend()
        val player = SoundPlayer(backend) { false }
        listOf(SoundEvent.Flip, SoundEvent.Place, SoundEvent.Foundation, SoundEvent.Win).forEach(player::play)
        assertEquals(emptyList(), backend.played)
    }

    @Test
    fun `real SoundPool backend loads the raw files without errors`() {
        val backend = SoundPoolBackend(ApplicationProvider.getApplicationContext())
        SoundPlayer(backend) { true }.play(SoundEvent.Place)
        backend.release()
    }
}
