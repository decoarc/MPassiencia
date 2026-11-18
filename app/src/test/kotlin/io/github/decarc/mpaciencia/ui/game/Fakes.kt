package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.data.GameStore
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.data.Settings
import io.github.decarc.mpaciencia.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGameStore(var saved: SavedGame? = null) : GameStore {
    var saves = 0
    override suspend fun load() = saved
    override suspend fun save(saved: SavedGame) {
        this.saved = saved
        saves++
    }
    override suspend fun clear() {
        saved = null
    }
}

class FakeSettingsStore(initial: Settings = Settings()) : SettingsStore {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(transform: (Settings) -> Settings) {
        settings.value = transform(settings.value)
    }
}

class FakeClock(var now: Long = 1_000_000L) : () -> Long {
    override fun invoke() = now
    fun advance(ms: Long) {
        now += ms
    }
}
