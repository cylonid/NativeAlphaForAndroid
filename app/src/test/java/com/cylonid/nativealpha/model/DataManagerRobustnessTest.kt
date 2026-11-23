package com.cylonid.nativealpha.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataManagerRobustnessTest {
    private lateinit var context: Context
    private lateinit var dataManager: DataManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager.getInstance()
        dataManager.websites.clear()
        dataManager.saveWebAppData()
    }

    @After
    fun tearDown() {
        dataManager.websites.clear()
        dataManager.saveWebAppData()
    }

    @Test
    fun testGetSuccessorAndPredecessorSkipInactiveEntriesAndWrap() {
        val app0 = WebApp("https://zero.example", 0, 0).apply { isActiveEntry = true }
        val app1 = WebApp("https://one.example", 1, 1).apply { isActiveEntry = false }
        val app2 = WebApp("https://two.example", 2, 2).apply { isActiveEntry = true }
        dataManager.websites.addAll(listOf(app0, app1, app2))

        assertEquals(2, dataManager.getSuccessor(0).ID)
        assertEquals(0, dataManager.getSuccessor(2).ID)
        assertEquals(0, dataManager.getPredecessor(2).ID)
        assertEquals(2, dataManager.getPredecessor(0).ID)
    }

    @Test
    fun testReplaceWebAppUpdatesEntryAndPersists() {
        val app0 = WebApp("https://initial.example", 0, 0).apply { title = "Initial" }
        dataManager.websites.add(app0)
        dataManager.saveWebAppData()

        val updated =
            WebApp("https://initial.example", 0, 0).apply {
                title = "Updated"
                isAllowJs = false
            }
        dataManager.replaceWebApp(updated)
        dataManager.loadAppData()

        val loaded = dataManager.getWebAppIgnoringGlobalOverride(0, true)
        assertEquals("Updated", loaded.title)
        assertEquals(false, loaded.isAllowJs)
    }

    @Test
    fun testGlobalWebAppOverridesNonOverridingWebAppSettings() {
        dataManager.settings.globalWebApp.apply {
            isAllowJs = false
            isForceDarkMode = true
        }
        val app0 =
            WebApp("https://example.com", 0, 0).apply {
                isOverrideGlobalSettings = false
                isAllowJs = true
                isForceDarkMode = false
            }
        dataManager.websites.add(app0)

        val effective = dataManager.getWebApp(0)
        assertNotSame(app0, effective)
        assertEquals(false, effective.isAllowJs)
        assertTrue(effective.isForceDarkMode)
    }
}
