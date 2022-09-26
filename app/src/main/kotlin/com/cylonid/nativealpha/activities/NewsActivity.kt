package com.cylonid.nativealpha.activities;

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.cylonid.nativealpha.BuildConfig
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.util.LocaleUtils
import kotlinx.android.synthetic.main.news_activity.*


class NewsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_activity)
        initializeUI()
    }

    override fun onBackPressed() {}

    private fun initializeUI() {
        setText()
        btnNewsConfirm.setOnClickListener {
            confirm()
        }
    }

    private fun setText() {
        val fileId = intent.extras?.getString("text") ?: "latestUpdate"

        news_content.loadUrl("file:///android_asset/news/" + fileId + "_" + LocaleUtils.fileEnding +".html")
        if(DataManager.getInstance().eulaData) {
            news_content.settings.javaScriptEnabled = true
            news_content.webViewClient = NewsWebViewClient()
        }
    }

    private fun confirm() {
        DataManager.getInstance().eulaData = true
        DataManager.getInstance().lastShownUpdate = BuildConfig.VERSION_CODE
        finish()
    }
}

private class NewsWebViewClient : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String) {
        view.evaluateJavascript("hideById('eula')", null)
        view.settings.javaScriptEnabled = false
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        return true
    }

}