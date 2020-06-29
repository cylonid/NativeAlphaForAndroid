package com.cylonid.nativefier;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class WebViewActivity extends AppCompatActivity {
    private WebView wv;
    private WebApp webapp;
    int webappID = -1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
          ((WebView) findViewById(R.id.webview)).restoreState(savedInstanceState.getBundle("webViewState"));
        else {
            setContentView(R.layout.full_webview);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            webappID = getIntent().getIntExtra(Utility.INT_ID_WEBAPPID, -1);

            WebsiteDataManager.getInstance().initContext(this);
            webapp = WebsiteDataManager.getInstance().getWebApp(webappID);
            String url = webapp.getLoadableUrl();
            boolean open_external = webapp.openUrlExternal();

            wv = findViewById(R.id.webview);
            wv.getSettings().setBlockNetworkLoads(false);
            wv.getSettings().setJavaScriptEnabled(webapp.isAllowJSSet());
            wv.getSettings().setDomStorageEnabled(true);
            CookieManager.getInstance().setAcceptCookie(webapp.isAllowCookiesSet());
            wv.getSettings().setAppCacheEnabled(true);



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

    @Override
    protected void onPause() {
        super.onPause();
        WebsiteDataManager.getInstance().getWebApp(webappID).saveCurrentUrl(wv.getUrl());
    }


    private class InternalBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            WebApp webapp = WebsiteDataManager.getInstance().getWebApp(webappID);
            String base_url = webapp.getBaseUrl();

            Uri uri = Uri.parse(base_url);
            String host = uri.getHost();
            HashMap<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("DNT", "1");

            if (webapp.openUrlExternal()) {
                if (!url.startsWith(base_url)) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
            }

            view.loadUrl(url, extraHeaders);
            return true;
        }
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                String base_url = WebsiteDataManager.getInstance().getWebApp(webappID).getBaseUrl();
//                // all links  with in ur site will be open inside the webview
//                //links that start ur domain example(http://www.example.com/)
//                if (url != null && url.startsWith(base_url)) {
//                    return true;
//                }
//                // all links that points outside the site will be open in a normal android browser
//                else {
//                    view.getContext().startActivity(
//                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//                    return true;
//                }
//            }
    }
}


