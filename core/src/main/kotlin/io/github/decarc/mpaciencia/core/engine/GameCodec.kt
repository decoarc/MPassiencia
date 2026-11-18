package io.github.decarc.mpaciencia.core.engine

import kotlinx.serialization.json.Json

/** Salva e carrega uma partida em JSON. Nunca lança exceção ao carregar. */
object GameCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(game: Game): String = json.encodeToString(GameSnapshot.serializer(), game.snapshot())

    fun decode(text: String): Game? =
        runCatching { json.decodeFromString(GameSnapshot.serializer(), text) }
            .getOrNull()
            ?.let(Game::fromSnapshot)
}
