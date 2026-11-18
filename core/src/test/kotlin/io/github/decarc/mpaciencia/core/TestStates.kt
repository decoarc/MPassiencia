package io.github.decarc.mpaciencia.core

import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.core.model.TableauCard

/** Helpers para montar estados à mão nos testes. */
fun card(rank: Int, suit: Suit, deck: Int = 0) = Card(suit, rank, deck)
fun up(rank: Int, suit: Suit, deck: Int = 0) = TableauCard(Card(suit, rank, deck), faceUp = true)
fun down(rank: Int, suit: Suit, deck: Int = 0) = TableauCard(Card(suit, rank, deck), faceUp = false)
fun col(vararg cards: TableauCard): List<TableauCard> = cards.toList()

/** Fundações com [cards] na posição [index] e o resto vazio. */
fun foundationWith(index: Int, vararg cards: Card): List<List<Card>> =
    List(GameState.FOUNDATIONS) { if (it == index) cards.toList() else emptyList() }

/** Estado montado à mão; completa até 13 colunas e 12 fundações com pilhas vazias. */
fun state(
    tableau: List<List<TableauCard>> = emptyList(),
    foundations: List<List<Card>> = emptyList(),
    stock: List<Card> = emptyList(),
    waste: List<Card> = emptyList(),
    drawMode: DrawMode = DrawMode.ONE,
    score: Int = 0,
    moves: Int = 0,
): GameState = GameState(
    gameNumber = 1,
    drawMode = drawMode,
    stock = stock,
    waste = waste,
    foundations = foundations + List(GameState.FOUNDATIONS - foundations.size) { emptyList() },
    tableau = tableau + List(GameState.COLUMNS - tableau.size) { emptyList() },
    score = score,
    moves = moves,
)
