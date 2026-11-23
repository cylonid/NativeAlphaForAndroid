package com.cylonid.nativealpha.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SandboxManagerTest {
    private lateinit var sandboxManager: SandboxManager

    @Before
    fun setUp() {
        sandboxManager = SandboxManager.getInstance()
        sandboxManager.nextContainer = 0
        for (i in 0 until 8) {
            sandboxManager.unregisterWebAppFromSandbox(i)
        }
    }

    @Test
    fun testRegisterAndUnregisterSandboxTracksOwnership() {
        val appA =
            WebApp("https://a.example", 0, 0).apply {
                isUseContainer = true
                containerId = 2
            }
        val appB =
            WebApp("https://b.example", 1, 1).apply {
                isUseContainer = true
                containerId = 2
            }

        assertFalse(sandboxManager.isSandboxUsedByAnotherApp(appA))
        sandboxManager.registerWebAppToSandbox(appA)
        assertFalse(sandboxManager.isSandboxUsedByAnotherApp(appA))
        assertTrue(sandboxManager.isSandboxUsedByAnotherApp(appB))

        // Unregistering needs DataManager.websites populated for logging
        DataManager.getInstance().websites.clear()
        DataManager.getInstance().websites.addAll(listOf(appA, appB))
        sandboxManager.unregisterWebAppFromSandbox(2)
        assertFalse(sandboxManager.isSandboxUsedByAnotherApp(appB))
        DataManager.getInstance().websites.clear()
    }

    @Test
    fun testContainerIdAllocationCyclesAcrossAllEightSlots() {
        val allocated = (0 until 10).map { sandboxManager.calculateNextFreeContainerId() }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 0, 1), allocated)
    }
}
