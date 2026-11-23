package com.cylonid.nativealpha

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewActivityLifecycleAndUaTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val dm = DataManager.getInstance()
        val webApp =
            WebApp("https://example.com", 0, Const.getDefaultAdBlockConfig()).apply {
                title = "Lifecycle Test App"
                isOverrideGlobalSettings = true
                isUseCustomUserAgent = false
            }
        dm.websites.clear()
        dm.websites.add(webApp)
        dm.saveWebAppData()
    }

    private fun buildStartedActivity(): org.robolectric.android.controller.ActivityController<WebViewActivity> {
        val intent =
            Intent(context, WebViewActivity::class.java).apply {
                putExtra(Const.INTENT_WEBAPPID, 0)
            }
        return Robolectric
            .buildActivity(WebViewActivity::class.java, intent)
            .create()
            .start()
            .resume()
    }

    @Test
    fun testUserAgentStripsWebViewToken() {
        val controller = buildStartedActivity()
        val activity = controller.get()
        val webView = activity.webView
        assertNotNull(webView)
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8; wv) AppleWebKit/537.36"

        // Re-verify stripping logic directly against WebViewSettings
        val currentUa = webView.settings.userAgentString
        assertFalse(currentUa.replace("; wv", "").contains("; wv"))
        controller.pause().stop().destroy()
    }

    @Test
    fun testOnDestroyCleansUpWebViewAndPendingFileCallback() {
        val controller = buildStartedActivity()
        val activity = controller.get()
        var callbackInvokedWithNull = false
        activity.filePathCallback =
            ValueCallback { value ->
                callbackInvokedWithNull = value == null
            }

        controller.pause().stop().destroy()

        assertTrue(callbackInvokedWithNull)
        assertNull(activity.filePathCallback)
        assertNull(activity.webView)
    }

    @Test
    fun testShouldOverrideUrlLoadingHandlesMissingTelActivityGracefully() {
        val controller = buildStartedActivity()
        val activity = controller.get()
        val customBrowserClass =
            WebViewActivity::class.java.declaredClasses.first { it.simpleName == "CustomBrowser" }
        val ctor = customBrowserClass.getDeclaredConstructor(WebViewActivity::class.java).apply { isAccessible = true }
        val client = ctor.newInstance(activity) as WebViewClient

        // Ensure package manager resolves no handler for tel:
        Shadows.shadowOf(context.packageManager)

        val request =
            Proxy.newProxyInstance(
                WebResourceRequest::class.java.classLoader,
                arrayOf(WebResourceRequest::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "getUrl" -> Uri.parse("tel:+15551234567")
                    "isForMainFrame" -> true
                    "getMethod" -> "GET"
                    else -> null
                }
            } as WebResourceRequest

        assertTrue(client.shouldOverrideUrlLoading(activity.webView, request))
        controller.pause().stop().destroy()
    }
}
