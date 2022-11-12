package com.cylonid.nativealpha.activities;

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cylonid.nativealpha.BuildConfig
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.util.LocaleUtils
import com.cylonid.nativealpha.util.Utility
import kotlinx.android.synthetic.main.news_activity.*
import kotlin.properties.Delegates


class NewsActivity : AppCompatActivity(), View.OnTouchListener, ViewTreeObserver.OnScrollChangedListener {

    var btnDefaultBackgroundColor: Drawable? = null
    var btnDefaultTextColor: Int = android.R.color.black

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_activity)

        initializeUI()
        setButtonState()
    }


    override fun onBackPressed() {}

    private fun disableAcceptButton() {
        btnNewsConfirm.isActivated = false
        btnDefaultTextColor = btnNewsConfirm.currentTextColor
        btnDefaultBackgroundColor = btnNewsConfirm.background

        btnNewsConfirm.setBackgroundColor(
            ContextCompat.getColor(
                baseContext,
                R.color.disabled_background_color
            )
        )
        btnNewsConfirm.setTextColor(
            ContextCompat.getColor(
                baseContext,
                R.color.disabled_text_color
            )
        )
        btnNewsConfirm.setOnClickListener {
            Utility.showToast(
                this,
                getString(R.string.scroll_to_bottom),
                Toast.LENGTH_SHORT
            ) 
        }
    }

    private fun enableAcceptButton() {
        btnNewsConfirm.isActivated = true
        btnNewsConfirm.setTextColor(btnDefaultTextColor)
        btnNewsConfirm.background = btnDefaultBackgroundColor

        btnNewsConfirm.setOnClickListener { confirm() }
    }

    private fun initializeUI() {
        setText()
        btnNewsConfirm.setOnClickListener {
            confirm()
        }
    }

    private fun setButtonState() {
        val vto: ViewTreeObserver = news_scrollchild.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val height: Int = news_scrollchild.measuredHeight
                if (height > 0) {
                    news_scrollchild.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (news_scrollview.canScrollVertically(1) || news_scrollview.canScrollVertically(-1)) {
                        news_scrollview.setOnTouchListener(this@NewsActivity)
                        news_scrollview.viewTreeObserver.addOnScrollChangedListener(this@NewsActivity)
                        disableAcceptButton()
                    }
                }
            }
        })
    }

    private fun setText() {
        val fileId = intent.extras?.getString("text") ?: "latestUpdate"

        news_content.loadUrl("file:///android_asset/news/" + fileId + "_" + LocaleUtils.fileEnding +".html")
        if(DataManager.getInstance().eulaData) {
            btnNewsConfirm.isEnabled = true
            news_content.settings.javaScriptEnabled = true
            news_content.webViewClient = NewsWebViewClient()
        }
    }

    private fun confirm() {
        DataManager.getInstance().eulaData = true
        DataManager.getInstance().lastShownUpdate = BuildConfig.VERSION_CODE
        finish()
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

    override fun onScrollChanged() {
        val view = news_scrollview.getChildAt(news_scrollview.childCount - 1)
        val bottomDetector: Int = view.bottom - (news_scrollview.height + news_scrollview.scrollY)

        if (bottomDetector < 30) {
           enableAcceptButton()
        }
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