package com.suspended.music.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the edit-distance core. These are pure and don't touch
 * the Android framework, so they run as plain JVM tests.
 */
class FuzzySearchTest {

    @Test
    fun levenshtein_identicalStrings_isZero() {
        assertEquals(0, FuzzySearch.levenshtein("beatles", "beatles"))
    }

    @Test
    fun levenshtein_measuresTypicalTypos() {
        // The bug being guarded against measured distance from a single character.
        assertEquals(1, FuzzySearch.levenshtein("beattles", "beatles"))
        assertEquals(1, FuzzySearch.levenshtein("cat", "bat"))
        assertEquals(3, FuzzySearch.levenshtein("kitten", "sitting"))
        assertEquals(6, FuzzySearch.levenshtein("kitten", ""))
    }

    @Test
    fun similarity_acceptsRealTypos() {
        // "beattles" vs "beatles" = 0.875, comfortably above the 0.66 floor.
        assertEquals(0.875f, FuzzySearch.similarity("beattles", "beatles"), 0.001f)
        assertTrue(FuzzySearch.similarity("cat", "bat") >= 0.66f)
    }

    @Test
    fun similarity_rejectsUnrelatedShortWords() {
        // Previously scored ~0.83 due to the broken DP; must stay below the floor.
        assertTrue(FuzzySearch.similarity("aazzzz", "oz") < 0.66f)
    }
}
