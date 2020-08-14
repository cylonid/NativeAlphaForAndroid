package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity {


    private WebView wv;
    int webappID = -1;

    //Constants for touchlistener
    private static final int NONE = 0;
    private static final int SWIPE = 1;
    private static final int TRESHOLD = 100;
    private Map<String, String> CUSTOM_HEADERS;

    private boolean quit_on_next_backpress = false;
    private String last_onbackpress_url = "";


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.full_webview);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        DataManager.getInstance().loadAppData();
        Utility.applyUITheme();
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        String url = webapp.getLoadableUrl();

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
//        wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wv.getSettings().setForceDark(WebSettings.FORCE_DARK_OFF);
        }

        wv.getSettings().setJavaScriptEnabled(webapp.isAllowJs());

        CookieManager.getInstance().setAcceptCookie(webapp.isAllowCookies());
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, webapp.isAllowThirdPartyCookies());

        if (webapp.isBlockImages())
            wv.getSettings().setBlockNetworkImage(true);

        if (webapp.isRequestDesktop()) {
            wv.getSettings().setUserAgentString(Const.DESKTOP_USER_AGENT);
            wv.getSettings().setUseWideViewPort(true);
            wv.getSettings().setLoadWithOverviewMode(true);

            wv.getSettings().setSupportZoom(true);
            wv.getSettings().setBuiltInZoomControls(true);
            wv.getSettings().setDisplayZoomControls(false);

            wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            wv.setScrollbarFadingEnabled(false);

        }

        CUSTOM_HEADERS = initCustomHeaders(webapp.isSendSavedataRequest());
        loadURL(wv, url);

        wv.setOnTouchListener(new View.OnTouchListener() {
            private int mode = NONE;
            private float startX;
            private float stopX;
            private float startY;
            private float stopY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (DataManager.getInstance().getWebApp(webappID).isRequestDesktop())
                    return false;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        // This happens when you touch the screen with two fingers
                        mode = SWIPE;
                        // You can also use event.getY(1) or the average of the two
                        startX = event.getX(0);
                        startY = event.getY(0);
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
                        if (DataManager.getInstance().getSettings().isMultitouchReload() && Math.abs(startY - stopY) > TRESHOLD) {
                            if (stopY > startY) {
                                wv.reload();
                            }
                            return true;
                        }
                    case MotionEvent.ACTION_MOVE:
                        if (mode == SWIPE) {
                            stopX = event.getX(0);
                            stopY = event.getY(0);
                        }
                        return false;
                }
                return false;
            }
        });


    }


    @Override
    public void onBackPressed() {
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        String current_onbackpress_url = wv.getUrl();

        if (quit_on_next_backpress || Utility.URLEqual(current_onbackpress_url, webapp.getBaseUrl()) || Utility.URLEqual(current_onbackpress_url, last_onbackpress_url)) {
            moveTaskToBack(true);
            quit_on_next_backpress = false;
        } else if (wv.canGoBack())
            wv.goBack();
        else {
            loadURL(wv, webapp.getBaseUrl());
            quit_on_next_backpress = true;
        }
        last_onbackpress_url = wv.getUrl();

    }


    @Override
    protected void onPause() {
        super.onPause();
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        if (webapp.isRestorePage())
            webapp.saveCurrentUrl(wv.getUrl());
        if (webapp.isClearCache() || DataManager.getInstance().getSettings().isClearCache())
            wv.clearCache(true);

        if (DataManager.getInstance().getSettings().isClearCookies()) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }

    }

    public WebView getWebView() {
        return wv;
    }
    private Map<String, String> initCustomHeaders(boolean save_data) {
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("DNT", "1");
        if (save_data)
            extraHeaders.put("Save-Data", "on");
        return Collections.unmodifiableMap(extraHeaders);
    }

    private void loadURL(final WebView view, final String url) {
        final WebApp webApp = DataManager.getInstance().getWebApp(webappID);
        if (url.contains("http://") && !webApp.isAllowHttp()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

            builder.setTitle(getString(R.string.no_https_dialog_title));
            builder.setMessage(getString(R.string.no_https_dialog_msg));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(getString(R.string.no_https_dialog_accept), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    webApp.allowHTTP();
                    view.loadUrl(url, CUSTOM_HEADERS);
                }
            });
            builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    onBackPressed();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
        else
          view.loadUrl(url, CUSTOM_HEADERS);

    }


    private class CustomBrowser extends WebViewClient {

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

            String message = getString(R.string.ssl_error_msg_line1) + " ";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message += getString(R.string.ssl_error_unknown_authority ) + "\n";
                    break;
                case SslError.SSL_EXPIRED:
                    message += getString(R.string.ssl_error_expired) + "\n";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message += getString(R.string.ssl_error_id_mismatch) + "\n";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message += getString(R.string.ssl_error_notyetvalid) + "\n";
                    break;
            }
            message += getString(R.string.ssl_error_msg_line2) + "\n";

            builder.setTitle(getString(R.string.ssl_error_title));
            builder.setMessage(message);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    handler.cancel();
                }
            });
            builder.setNegativeButton(getString(R.string.load_anyway), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    handler.proceed();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
//            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setPadding(5, 5, 5, 5);
//            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(ContextCompat.getColor(WebViewActivity.this, android.R.color.holo_orange_light));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(WebViewActivity.this, android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(WebViewActivity.this, android.R.color.holo_green_dark));
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (DataManager.getInstance().getWebApp(webappID).isRequestDesktop())
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            WebApp webapp = DataManager.getInstance().getWebApp(webappID);

            if (webapp.isOpenUrlExternal()) {
                String base_url = webapp.getBaseUrl();
                Uri uri = Uri.parse(base_url);
                String host = uri.getHost();
                if (!url.contains(host)) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
            }
            loadURL(view, url);
            return true;
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }
    }
}


