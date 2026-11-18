package io.github.decarc.mpaciencia.core.random

/**
 * PCG32 (pcg-c: pcg32_srandom_r / pcg32_random_r / pcg32_boundedrand_r).
 * Implementação própria para que a mesma seed gere a mesma sequência em qualquer plataforma.
 */
class Pcg32(seed: ULong, sequence: ULong) {
    private var state = 0uL
    private val inc = (sequence shl 1) or 1uL

    init {
        nextUInt()
        state += seed
        nextUInt()
    }

    fun nextUInt(): UInt {
        val old = state
        state = old * MULTIPLIER + inc
        val xorshifted = (((old shr 18) xor old) shr 27).toUInt()
        val rot = (old shr 59).toInt()
        return (xorshifted shr rot) or (xorshifted shl ((-rot) and 31))
    }

    /** Inteiro uniforme em `0 until bound`, sem viés. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound deve ser positivo: $bound" }
        val b = bound.toUInt()
        val threshold = (0u - b) % b
        while (true) {
            val r = nextUInt()
            if (r >= threshold) return (r % b).toInt()
        }
    }

    private companion object {
        const val MULTIPLIER = 6364136223846793005uL
    }
}
