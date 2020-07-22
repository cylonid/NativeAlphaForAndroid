package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cylonid.nativealpha.databinding.WebappSettingsBinding;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.HashMap;
import java.util.Objects;

public class WebAppSettingsActivity extends AppCompatActivity {

    int webappID = -1;
    private ShortcutHelper.FaviconFetcher faviconFetcher = null;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebappSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.webapp_settings);


        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");

        final View inflated_view = binding.getRoot();
        final WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        final WebApp modified_webapp = new WebApp(webapp);
        binding.setWebapp(modified_webapp);

        final Button btnCreateShortcut = inflated_view.findViewById(R.id.btnRecreateShortcut);

        btnCreateShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faviconFetcher = new ShortcutHelper.FaviconFetcher(new ShortcutHelper(webapp, WebAppSettingsActivity.this));
                faviconFetcher.execute();
                faviconFetcher = null;
            }
        });
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().replaceWebApp(modified_webapp);
                onBackPressed();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faviconFetcher != null)
            faviconFetcher.cancel(true);
    }
}


