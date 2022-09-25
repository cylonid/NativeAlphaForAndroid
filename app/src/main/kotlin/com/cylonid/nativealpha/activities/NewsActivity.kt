package com.cylonid.nativealpha.activities;

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.util.LocaleUtils
import com.cylonid.nativealpha.util.Utility
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
            btnNewsCancel.setOnClickListener { cancelEULA() }
            btnNewsConfirm.setOnClickListener {
                confirmEULA()
            }
            btnNewsConfirm.text = resources.getString(R.string.accept)
            setText()
        } else {
            btnNewsCancel.visibility = View.GONE
            btnNewsConfirm.setOnClickListener {
                finish()
            }
        }
    }

    private fun setText() {
        val fileId = intent.extras!!.getString("text")
        news_content.loadUrl("file:///android_asset/news/" + fileId + "_" + LocaleUtils.fileEnding +".html")
    }

    private fun cancelEULA() {
        Utility.showInfoSnackbar(this, getString(R.string.cancel_eula), Snackbar.LENGTH_LONG)
    }

    private fun confirmEULA() {
        DataManager.getInstance().eulaData = true
        finish()
    }
}