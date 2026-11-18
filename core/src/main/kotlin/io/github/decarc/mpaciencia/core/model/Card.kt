package io.github.decarc.mpaciencia.core.model

import kotlinx.serialization.Serializable

enum class CardColor { RED, BLACK }

@Serializable
enum class Suit(val color: CardColor) {
    HEARTS(CardColor.RED),
    DIAMONDS(CardColor.RED),
    SPADES(CardColor.BLACK),
    CLUBS(CardColor.BLACK),
}

/** Uma carta de um dos 3 baralhos. [rank]: 1 (Ás) a 13 (Rei). [deck]: 0 a 2. */
@Serializable
data class Card(val suit: Suit, val rank: Int, val deck: Int) {
    init {
        require(rank in 1..13) { "rank fora de 1..13: $rank" }
        require(deck in 0 until DECKS) { "deck fora de 0..${DECKS - 1}: $deck" }
    }

    val color: CardColor get() = suit.color

    companion object {
        const val DECKS = 3
        const val TOTAL = 52 * DECKS
    }
}

/** As 156 cartas em ordem baralho → naipe → valor. */
fun fullDeck(): List<Card> =
    (0 until Card.DECKS).flatMap { deck ->
        Suit.entries.flatMap { suit -> (1..13).map { rank -> Card(suit, rank, deck) } }
    }
