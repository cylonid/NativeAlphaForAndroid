package com.cylonid.nativefier;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    private WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_webview);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        String url = getIntent().getStringExtra("URL");
        boolean open_external = getIntent().getBooleanExtra("open_external", false);

        wv = (WebView)findViewById(R.id.webview);
        wv.getSettings().setBlockNetworkLoads(false);
        if (open_external)
            wv.setWebViewClient(new WebViewClient());
        else
            wv.setWebViewClient(new InternalBrowser());

        wv.loadUrl(url);

        }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack())
            wv.goBack();
        else
            moveTaskToBack(true);
    }
    private class InternalBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

    }
}


