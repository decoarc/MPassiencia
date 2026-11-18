package io.github.decarc.mpaciencia.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.decarc.mpaciencia.core.model.DrawMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStoreTest {
    private val file = File(Files.createTempDirectory("prefs").toFile(), "settings.preferences_pb")

    private fun open(scope: CoroutineScope) =
        DataStoreSettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))

    @Test
    fun `defaults are draw one, sound on, normal speed`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        assertEquals(Settings(DrawMode.ONE, soundOn = true, animationSpeed = AnimationSpeed.NORMAL), open(scope).settings.first())
        scope.cancel()
    }

    @Test
    fun `update persists across a new store instance`() = runBlocking {
        val first = CoroutineScope(Dispatchers.IO + SupervisorJob())
        open(first).update { it.copy(defaultDrawMode = DrawMode.THREE, soundOn = false, animationSpeed = AnimationSpeed.OFF) }
        first.cancel()
        val second = CoroutineScope(Dispatchers.IO + SupervisorJob())
        assertEquals(Settings(DrawMode.THREE, soundOn = false, animationSpeed = AnimationSpeed.OFF), open(second).settings.first())
        second.cancel()
    }
}
