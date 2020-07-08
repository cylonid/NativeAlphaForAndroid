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

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.HashMap;

public class WebViewActivity extends AppCompatActivity {

    private WebView wv;
    private WebApp webapp;
    int webappID = -1;
    boolean exit_on_next_back_pressed = false;

    //Constants for touchlistener
    private static final int NONE = 0;
    private static final int SWIPE = 1;
    private static final int TRESHOLD = 100;


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.full_webview);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        DataManager.getInstance().initContext(this);
        DataManager.getInstance().loadAppData();
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
        webapp = DataManager.getInstance().getWebApp(webappID);
        String url = webapp.getLoadableUrl();
        boolean open_external = webapp.openUrlExternal();

        wv = findViewById(R.id.webview);

        if (webapp.isUseAdblock()) {
            wv.setVisibility(View.GONE);
            wv = findViewById(R.id.adblockwebview);
            wv.setVisibility(View.VISIBLE);
            ((AdblockWebView)wv).setAdblockEnabled(webapp.isUseAdblock());
        }

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
            wv.getSettings().setUserAgentString(Const.DESKTOP_USER_AGENT);
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
                if (DataManager.getInstance().getWebApp(webappID).isRequestDesktopSet())
                    return false;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        // This happens when you touch the screen with two fingers
                        mode = SWIPE;
                        // You can also use event.getY(1) or the average of the two
                        startX = event.getX(0);
                        return true;

                    case MotionEvent.ACTION_POINTER_UP:
                        // This happens when you release the second finger
                        mode = NONE;
                        if (Math.abs(startX - stopX) > TRESHOLD) {
                            if (startX > stopX) {
                                if (event.getPointerCount() == 3 && DataManager.getInstance().getSettings().isThreeFingerMultitouch()) {
                                    startActivity(Utility.createWebViewIntent(DataManager.getInstance().getPredecessor(webappID), WebViewActivity.this));
                                    finish();
                                } else if (DataManager.getInstance().getSettings().isTwoFingerMultitouch()) {
                                    if (wv.canGoForward())
                                        wv.goForward();
                                }
                            } else {
                                if (event.getPointerCount() == 3 && DataManager.getInstance().getSettings().isThreeFingerMultitouch()) {
                                    startActivity(Utility.createWebViewIntent(DataManager.getInstance().getSuccessor(webappID), WebViewActivity.this));
                                    finish();
                                } else if (DataManager.getInstance().getSettings().isTwoFingerMultitouch())
                                    onBackPressed();

                            }
                            return true;
                        }
                    case MotionEvent.ACTION_MOVE:
                        if (mode == SWIPE) {
                            stopX = event.getX(0);
                        }
                        return false;
                }
                return false;
            }
        });


    }


    @Override
    public void onBackPressed() {
        if (exit_on_next_back_pressed)
            moveTaskToBack(true);

        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            exit_on_next_back_pressed = true;
            wv.loadUrl(DataManager.getInstance().getWebApp(webappID).getBaseUrl());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        if (webapp.isRestorePageSet())
            webapp.saveCurrentUrl(wv.getUrl());
        if (webapp.isClearCacheSet() || DataManager.getInstance().getSettings().isClearCache())
            wv.clearCache(true);

        if (DataManager.getInstance().getSettings().isClearCookies())
            CookieManager.getInstance().flush();
    }


    private class CustomBrowser extends WebViewClient {
        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (DataManager.getInstance().getWebApp(webappID).isRequestDesktopSet())
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            WebApp webapp = DataManager.getInstance().getWebApp(webappID);

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


