package com.cylonid.nativealpha.activities;

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.cylonid.nativealpha.BuildConfig
import com.cylonid.nativealpha.databinding.NewsActivityBinding
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.util.LocaleUtils
import com.cylonid.nativealpha.util.WindowInsetsUtils


class NewsActivity : AppCompatActivity(), View.OnTouchListener {

    private lateinit var binding: NewsActivityBinding

    inner class WebAppInterface {
        @JavascriptInterface
        fun onOkButtonPressed() {
            DataManager.getInstance().eulaData = true
            DataManager.getInstance().lastShownUpdate = BuildConfig.VERSION_CODE
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewsActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        WindowInsetsUtils.applyAsPadding(this)

        binding.newsContent.settings.javaScriptEnabled = true
        binding.newsContent.isLongClickable = false
        binding.newsContent.webChromeClient = WebChromeClient()

        onBackPressedDispatcher.addCallback(this) {}
        setText()
    }

    private fun setText() {
        val fileId = intent.extras?.getString("text") ?: "latestUpdate"

        binding.newsContent.loadUrl("file:///android_asset/news/" + fileId + "_" + LocaleUtils.fileEnding + ".html")
        binding.newsContent.addJavascriptInterface(WebAppInterface(), "NAlpha")
        val hideEula = DataManager.getInstance().eulaData;

        binding.newsContent.webViewClient = NewsWebViewClient(
            hideEula = hideEula,
            showLiberaPay = BuildConfig.FLAVOR == "extendedGithub"
        )
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

}

private class NewsWebViewClient(val hideEula: Boolean, val showLiberaPay: Boolean) :
    WebViewClient() {
    override fun onPageFinished(view: WebView, url: String) {
        if (hideEula) view.evaluateJavascript("hideById('eula')", null)
        if (showLiberaPay) {
            view.evaluateJavascript("showById('nonGp')", null)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        return true
    }

}