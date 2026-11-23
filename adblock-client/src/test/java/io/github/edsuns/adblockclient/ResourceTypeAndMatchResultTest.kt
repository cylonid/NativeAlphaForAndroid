package io.github.edsuns.adblockclient

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTypeAndMatchResultTest {
    @Test
    fun testMatchResultExceptionHelpers() {
        val block = MatchResult(shouldBlock = true, matchedRule = "||ads.example^", matchedExceptionRule = null)
        assertFalse(block.isException)
        assertFalse(block.hasException)

        val allowed = MatchResult(shouldBlock = false, matchedRule = null, matchedExceptionRule = "@@||ads.example^")
        assertTrue(allowed.isException)
        assertTrue(allowed.hasException)
    }

    @Test
    fun testResourceTypeFilterOptionsAreDistinctNonZeroForKnownTypes() {
        val known = ResourceType.entries.filter { it != ResourceType.UNKNOWN }
        assertTrue(known.all { it.filterOption != 0 })
        assertTrue(known.map { it.filterOption }.toSet().size == known.size)
    }
}
