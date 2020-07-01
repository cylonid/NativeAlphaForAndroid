package com.cylonid.nativefier;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
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

    //Constants for touchlistener
    private static final int NONE = 0;
    private static final int SWIPE = 1;
    private static final int TRESHOLD = 100;



    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
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
            wv.setWebViewClient(new CustomBrowser());
            wv.getSettings().setDomStorageEnabled(true);
            wv.getSettings().setBlockNetworkLoads(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wv.getSettings().setForceDark(WebSettings.FORCE_DARK_OFF);
            }

            wv.getSettings().setJavaScriptEnabled(webapp.isAllowJSSet());

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

            HashMap<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("DNT", "1");
            wv.loadUrl(url, extraHeaders);

            wv.setOnTouchListener(new View.OnTouchListener() {
                private int mode = NONE;
                private float startX;
                private float stopX;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (WebsiteDataManager.getInstance().getWebApp(webappID).isRequestDesktopSet())
                        return false;
                    switch (event.getAction() & MotionEvent.ACTION_MASK)
                    {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            // This happens when you touch the screen with two fingers
                            mode = SWIPE;
                            // You can also use event.getY(1) or the average of the two
                            startX = event.getX(0);
                            return true;

                        case MotionEvent.ACTION_POINTER_UP:
                            // This happens when you release the second finger
                            mode = NONE;
                            if(Math.abs(startX - stopX) > TRESHOLD)
                            {
                                if(startX > stopX)
                                {
                                    if (event.getPointerCount() == 3) {
                                        startActivity(Utility.createWebViewIntent(WebsiteDataManager.getInstance().getPredecessor(webappID), WebViewActivity.this));
                                        finish();
                                    }
                                    else {
                                        if (wv.canGoForward())
                                            wv.goForward();
                                    }
                                }
                                else
                                {
                                    if (event.getPointerCount() == 3) {
                                        startActivity(Utility.createWebViewIntent(WebsiteDataManager.getInstance().getSuccessor(webappID), WebViewActivity.this));
                                        finish();
                                    }
                                    else
                                        onBackPressed();
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            if(mode == SWIPE)
                            {
                                stopX = event.getX(0);
                            }
                            return false;
                    }
                    return false;
                }
            });
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
        WebApp webapp = WebsiteDataManager.getInstance().getWebApp(webappID);
        if (webapp.isRestorePageSet())
            webapp.saveCurrentUrl(wv.getUrl());
        if (webapp.isClearCacheSet())
            wv.clearCache(true);
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
            return false;
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

    }
}


