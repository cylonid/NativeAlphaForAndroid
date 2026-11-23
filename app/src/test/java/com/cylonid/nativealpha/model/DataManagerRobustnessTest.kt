package com.cylonid.nativealpha.model

import android.content.Context
import android.util.Base64
import android.util.Base64OutputStream
import androidx.test.core.app.ApplicationProvider
import com.cylonid.nativealpha.util.Const
import com.himanshurawat.hasher.HashType
import com.himanshurawat.hasher.Hasher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.Date
import java.util.TreeMap

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
        val extraConfig = AdblockConfig("extra", "extra-list")
        effective.adBlockSettings.add(extraConfig)
        assertFalse(
            dataManager.settings.globalWebApp.adBlockSettings
                .contains(extraConfig),
        )
    }

    @Test
    fun testGetSuccessorAndPredecessorReturnNullWhenEmptyOrAllInactive() {
        assertNull(dataManager.getSuccessor(0))
        assertNull(dataManager.getPredecessor(0))

        dataManager.websites.add(WebApp("https://inactive.example", 0, 0).apply { isActiveEntry = false })
        assertNull(dataManager.getSuccessor(0))
        assertNull(dataManager.getPredecessor(0))
    }

    @Test
    fun testReplaceWebAppIgnoresOutOfBoundsId() {
        val outOfBounds = WebApp("https://oob.example", 99, 99)
        dataManager.replaceWebApp(outOfBounds)
        assertTrue(dataManager.websites.isEmpty())
    }

    @Test
    fun testLoadAppDataHandlesCorruptOrNullJsonGracefully() {
        val prefs = context.getSharedPreferences("WEBSITEDATA", Context.MODE_PRIVATE)
        prefs
            .edit()
            .putString("webappdata", "{not-valid-json")
            .putString("globalsettings", "null")
            .commit()

        dataManager.loadAppData()
        assertNotNull(dataManager.websites)
        assertNotNull(dataManager.settings)
        assertEquals(Const.NO_CONTAINER, dataManager.settings.globalWebApp.containerId)
    }

    @Test
    fun testLoadSharedPreferencesRejectsUnauthorizedSerializedClass() {
        val mapWithDisallowedClass = TreeMap<String, Any>().apply { put("evil", Date(0)) }
        val baos = ByteArrayOutputStream()
        Base64OutputStream(baos, Base64.DEFAULT).use { b64 ->
            ObjectOutputStream(b64).use { oos ->
                oos.writeObject(Hasher.hash(mapWithDisallowedClass.toString(), HashType.SHA_256))
                oos.writeObject(mapWithDisallowedClass)
            }
        }
        assertFalse(dataManager.loadSharedPreferences(ByteArrayInputStream(baos.toByteArray())))
    }
}
