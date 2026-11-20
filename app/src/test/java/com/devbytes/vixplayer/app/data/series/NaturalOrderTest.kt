package com.devbytes.vixplayer.app.data.series

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {

    private fun sorted(vararg names: String) = names.sortedWith(NaturalOrder)

    @Test fun numbersCompareByValue() =
        assertEquals(listOf("Ep 1", "Ep 2", "Ep 10"), sorted("Ep 10", "Ep 2", "Ep 1"))

    @Test fun lettersIgnoreCase() =
        assertEquals(listOf("lecture 2", "Lecture 10"), sorted("Lecture 10", "lecture 2"))

    @Test fun leadingZerosDoNotChangeTheValue() =
        assertEquals(listOf("Ep 02", "Ep 3", "Ep 010"), sorted("Ep 010", "Ep 3", "Ep 02"))

    @Test fun timestampNamesSortChronologically() = assertEquals(
        listOf("20250101_000000.mp4", "20251025_121631.mp4", "20251025_121700.mp4"),
        sorted("20251025_121700.mp4", "20250101_000000.mp4", "20251025_121631.mp4"),
    )

    @Test fun aPrefixComesFirst() = assertEquals(listOf("Show", "Show 2"), sorted("Show 2", "Show"))

    @Test fun numbersLongerThanALongStillCompare() = assertTrue(
        NaturalOrder.compare("a99999999999999999999", "a100000000000000000000") < 0,
    )

    @Test fun orderIsTotalForNamesThatOnlyDifferInZerosOrCase() {
        assertTrue(NaturalOrder.compare("Ep 02", "Ep 2") != 0)
        assertTrue(NaturalOrder.compare("ep 2", "Ep 2") != 0)
        assertEquals(0, NaturalOrder.compare("Ep 2", "Ep 2"))
    }
}
