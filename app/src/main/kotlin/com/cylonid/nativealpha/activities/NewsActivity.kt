package com.cylonid.nativealpha.activities;

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.util.LocaleUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.news_activity.*


class NewsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.news_activity)
        initializeUI()
    }

    private fun initializeUI() {
        val enforceCheck = intent.extras!!.getBoolean("enforceCheck")
        if(enforceCheck) {
            news_cancel.setOnClickListener { cancelEULA() }
            news_confirm.setOnClickListener {
                confirmEULA()
            }
            setText()
        } else {
            news_cancel.visibility = View.GONE
            news_confirm.setOnClickListener {
                finish()
            }
        }
    }

    private fun setText() {
        val fileId = intent.extras!!.getString("text")
        news_content.loadUrl("file:///android_asset/news/" + fileId + "_" + LocaleUtils.fileEnding +".html")
    }

    private fun cancelEULA() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "You must accept the EULA to continue",
            Snackbar.LENGTH_LONG
        ).show()
    }
    
    private fun confirmEULA() {
        DataManager.getInstance().eulaData = true
        finish()
    }
}