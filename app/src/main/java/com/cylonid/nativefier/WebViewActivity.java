package com.cylonid.nativefier;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    private WebView wv;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
          ((WebView) findViewById(R.id.webview)).restoreState(savedInstanceState.getBundle("webViewState"));
        else {
            setContentView(R.layout.full_webview);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            String url = getIntent().getStringExtra(Utility.INT_ID_URL);
            boolean open_external = getIntent().getBooleanExtra(Utility.INT_ID_EXTERNAL, false);

            wv = (WebView)findViewById(R.id.webview);
            wv.getSettings().setBlockNetworkLoads(false);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.getSettings().setDomStorageEnabled(true);
            if (open_external)
                wv.setWebViewClient(new WebViewClient());
            else
                wv.setWebViewClient(new InternalBrowser());

            wv.loadUrl(url);
            }

        }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack())
            wv.goBack();
        else
            moveTaskToBack(true);
    }
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        wv.saveState(bundle);
        outState.putBundle("webViewState", bundle);
    }
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        Bundle bundle = new Bundle();
        wv.saveState(bundle);
        state.putBundle("webViewState", bundle);
    }


    private class InternalBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

    }
}


