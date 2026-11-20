package com.devbytes.vixplayer.app.data.series

/**
 * Orders names the way people read them: runs of digits compare by value, so "Ep 2" comes
 * before "Ep 10" and "Lecture 9" before "Lecture 10"; letters compare case-insensitively.
 * Only ASCII digits count as numbers, so other scripts' numerals sort as plain text.
 */
object NaturalOrder : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isAsciiDigit() && b[j].isAsciiDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isAsciiDigit()) i++
                while (j < b.length && b[j].isAsciiDigit()) j++
                val numberA = a.substring(startA, i).trimStart('0')
                val numberB = b.substring(startB, j).trimStart('0')
                // Without leading zeros, a longer run is the bigger number; equal-length runs
                // compare correctly as text.
                if (numberA.length != numberB.length) return numberA.length.compareTo(numberB.length)
                val byValue = numberA.compareTo(numberB)
                if (byValue != 0) return byValue
            } else {
                val byChar = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
                if (byChar != 0) return byChar
                i++
                j++
            }
        }
        val byRemaining = (a.length - i).compareTo(b.length - j)
        // Names differing only in case or leading zeros still need a stable, total order.
        return if (byRemaining != 0) byRemaining else a.compareTo(b)
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'
}
