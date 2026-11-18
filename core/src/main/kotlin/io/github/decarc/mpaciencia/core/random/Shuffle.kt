package io.github.decarc.mpaciencia.core.random

/** Fisher–Yates determinístico com [rng]. */
fun <T> List<T>.shuffledWith(rng: Pcg32): List<T> {
    val out = toMutableList()
    for (i in out.size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = out[i]
        out[i] = out[j]
        out[j] = tmp
    }
    return out
}
