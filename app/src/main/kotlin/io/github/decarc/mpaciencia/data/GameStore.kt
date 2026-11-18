package io.github.decarc.mpaciencia.data

import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.engine.GameCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

data class SavedGame(val game: Game, val elapsedMs: Long)

interface GameStore {
    suspend fun load(): SavedGame?
    suspend fun save(saved: SavedGame)
    suspend fun clear()
}

/** Salva a partida em `save.json` (escrita atômica: grava em `.tmp` e renomeia). */
class FileGameStore(private val dir: File) : GameStore {
    @Serializable
    private data class SaveFile(val version: Int = 1, val elapsedMs: Long, val game: String)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val file get() = File(dir, "save.json")
    private val tmp get() = File(dir, "save.json.tmp")

    override suspend fun load(): SavedGame? = withContext(Dispatchers.IO) {
        runCatching {
            val save = json.decodeFromString(SaveFile.serializer(), file.readText())
            GameCodec.decode(save.game)?.let { SavedGame(it, save.elapsedMs) }
        }.getOrNull()
    }

    /** Falha de disco (cheio, sem permissão) não pode fechar o app: a jogada continua na memória. */
    override suspend fun save(saved: SavedGame) = withContext(Dispatchers.IO) {
        runCatching { write(saved) }
        Unit
    }

    private fun write(saved: SavedGame) {
        dir.mkdirs()
        val text = json.encodeToString(SaveFile.serializer(), SaveFile(elapsedMs = saved.elapsedMs, game = GameCodec.encode(saved.game)))
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        tmp.delete()
        Unit
    }
}
