package io.github.decarc.mpaciencia.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.ui.game.SoundEvent

/** Onde os sons tocam de fato (separado para os testes). */
interface SoundBackend {
    fun load(resId: Int): Int
    fun play(soundId: Int)
    fun release()
}

class SoundPoolBackend(private val context: Context) : SoundBackend {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    override fun load(resId: Int): Int = pool.load(context, resId, 1)

    override fun play(soundId: Int) {
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    override fun release() = pool.release()
}

/** Toca o som de cada evento do jogo, se o som estiver ligado. */
class SoundPlayer(private val backend: SoundBackend, private val soundOn: () -> Boolean) {
    private val ids: Map<SoundEvent, Int> = mapOf(
        SoundEvent.Flip to backend.load(R.raw.flip),
        SoundEvent.Place to backend.load(R.raw.place),
        SoundEvent.Foundation to backend.load(R.raw.foundation),
        SoundEvent.Win to backend.load(R.raw.win),
    )

    fun play(event: SoundEvent) {
        if (!soundOn()) return
        ids[event]?.let(backend::play)
    }

    fun release() = backend.release()
}
