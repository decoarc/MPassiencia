package io.github.decarc.mpaciencia.core.random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Pcg32Test {
    // Vetor de referência do pcg32-demo do pcg-c: pcg32_srandom(42, 54)
    @Test
    fun `matches pcg32 reference vector`() {
        val rng = Pcg32(42u, 54u)
        val expected = listOf(0xa15c02b7u, 0x7b47f409u, 0xba1d3330u, 0x83d2f293u, 0xbfa4784bu, 0xcbed606eu)
        assertEquals(expected, List(6) { rng.nextUInt() })
    }

    @Test
    fun `bounded ints stay in range and cover it`() {
        val rng = Pcg32(7u, 1u)
        val seen = mutableSetOf<Int>()
        repeat(10_000) {
            val v = rng.nextInt(13)
            assertTrue(v in 0..12)
            seen += v
        }
        assertEquals((0..12).toSet(), seen)
    }

    @Test
    fun `shuffle is a permutation and deterministic`() {
        val list = (1..52).toList()
        val a = list.shuffledWith(Pcg32(3u, 9u))
        val b = list.shuffledWith(Pcg32(3u, 9u))
        assertEquals(a, b)
        assertEquals(list.toSet(), a.toSet())
        assertTrue(a != list)
    }
}
