package com.cylonid.nativealpha.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.cylonid.nativealpha.WebSandboxActivity0
import com.cylonid.nativealpha.WebSandboxActivity7
import com.cylonid.nativealpha.WebViewActivity
import com.cylonid.nativealpha.model.WebApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewLauncherTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCreateWebViewIntentRoutesStandardAndSandboxedContainers() {
        val standardApp = WebApp("https://example.com", 4, 0)
        val standardIntent = WebViewLauncher.createWebViewIntent(standardApp, context)
        assertNotNull(standardIntent)
        assertEquals(WebViewActivity::class.java.name, standardIntent!!.component?.className)
        assertEquals(4, standardIntent.getIntExtra(Const.INTENT_WEBAPPID, -1))
        assertEquals(Intent.ACTION_VIEW, standardIntent.action)
        assertEquals("https://example.com4", standardIntent.dataString)

        val sandbox0 =
            WebApp("https://s0.example", 5, 1).apply {
                isUseContainer = true
                containerId = 0
            }
        val intent0 = WebViewLauncher.createWebViewIntent(sandbox0, context)
        assertEquals(WebSandboxActivity0::class.java.name, intent0?.component?.className)

        val sandbox7 =
            WebApp("https://s7.example", 6, 2).apply {
                isUseContainer = true
                containerId = 7
                isBiometricProtection = true
            }
        val intent7 = WebViewLauncher.createWebViewIntent(sandbox7, context)
        assertNotNull(intent7)
        assertEquals(WebSandboxActivity7::class.java.name, intent7!!.component?.className)
        assertTrue((intent7.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0)
    }
}
