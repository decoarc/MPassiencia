package io.github.decarc.mpaciencia.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TableauCard(val card: Card, val faceUp: Boolean)

@Serializable
enum class DrawMode(val count: Int) { ONE(1), THREE(3) }

/**
 * Estado imutável da mesa. Em todas as listas, o último elemento é o topo da pilha.
 */
@Serializable
data class GameState(
    val gameNumber: Int,
    val drawMode: DrawMode,
    val stock: List<Card>,
    val waste: List<Card>,
    val foundations: List<List<Card>>,
    val tableau: List<List<TableauCard>>,
    val score: Int = 0,
    val moves: Int = 0,
) {
    val isWon: Boolean get() = foundations.sumOf { it.size } == Card.TOTAL

    companion object {
        const val FOUNDATIONS = 12
        const val COLUMNS = 13
    }
}
