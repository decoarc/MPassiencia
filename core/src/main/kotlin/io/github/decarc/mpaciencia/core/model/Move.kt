package io.github.decarc.mpaciencia.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Referência a uma pilha da mesa. */
@Serializable
sealed interface PileRef {
    @Serializable @SerialName("s")
    data object Stock : PileRef

    @Serializable @SerialName("w")
    data object Waste : PileRef

    @Serializable @SerialName("f")
    data class Foundation(val index: Int) : PileRef

    @Serializable @SerialName("t")
    data class Tableau(val index: Int) : PileRef
}

@Serializable
sealed interface Move {
    /** Vira 1 ou 3 cartas do monte para o descarte. */
    @Serializable @SerialName("draw")
    data object Draw : Move

    /** Devolve o descarte ao monte quando o monte está vazio. */
    @Serializable @SerialName("recycle")
    data object Recycle : Move

    /** Move [count] cartas do topo de [from] para [to]. */
    @Serializable @SerialName("move")
    data class Transfer(val from: PileRef, val to: PileRef, val count: Int = 1) : Move
}
