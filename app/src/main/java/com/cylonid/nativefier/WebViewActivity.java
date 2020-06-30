package com.cylonid.nativefier;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
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
            WebsiteDataManager.getInstance().loadAppData();
            Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
            webapp = WebsiteDataManager.getInstance().getWebApp(webappID);
            String url = webapp.getLoadableUrl();
            boolean open_external = webapp.openUrlExternal();

            wv = findViewById(R.id.webview);
            wv.getSettings().setBlockNetworkLoads(false);
            wv.getSettings().setJavaScriptEnabled(webapp.isAllowJSSet());
            wv.getSettings().setDomStorageEnabled(true);
            CookieManager.getInstance().setAcceptCookie(webapp.isAllowCookiesSet());
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, webapp.isAllowThirdPartyCookiesSet());


            if (webapp.isRequestDesktopSet()) {
                wv.getSettings().setUserAgentString(Utility.DESKTOP_USER_AGENT);
                wv.getSettings().setUseWideViewPort(true);
                wv.getSettings().setLoadWithOverviewMode(true);

                wv.getSettings().setSupportZoom(true);
                wv.getSettings().setBuiltInZoomControls(true);
                wv.getSettings().setDisplayZoomControls(false);

                wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
                wv.setScrollbarFadingEnabled(false);
            }

            wv.setWebViewClient(new CustomBrowser());

            wv.loadUrl(url);
        }

    }


    @Override
    public void onBackPressed() {
        if (wv.canGoBack())
            wv.goBack();
        else
            wv.loadUrl(WebsiteDataManager.getInstance().getWebApp(webappID).getBaseUrl());
//            moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebsiteDataManager.getInstance().getWebApp(webappID).saveCurrentUrl(wv.getUrl());
    }


    private class CustomBrowser extends WebViewClient {
        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (WebsiteDataManager.getInstance().getWebApp(webappID).isRequestDesktopSet())
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            WebApp webapp = WebsiteDataManager.getInstance().getWebApp(webappID);

            if (webapp.openUrlExternal()) {
                String base_url = webapp.getBaseUrl();
                Uri uri = Uri.parse(base_url);
                String host = uri.getHost();
                if (!url.contains(host)) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
            }
            if (!webapp.isEnableCacheSet()) {
                view.getSettings().setAppCacheEnabled(false);
                view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            }

            HashMap<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("DNT", "1");
            view.loadUrl(url, extraHeaders);
            return true;
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
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


