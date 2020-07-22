package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.HashMap;

public class WebAppSettingsActivity extends AppCompatActivity {

    int webappID = -1;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.webapp_settings);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        String url = webapp.getLoadableUrl();
    }
}


