package com.cylonid.nativealpha;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.SandboxManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;
import static com.cylonid.nativealpha.util.Const.CODE_OPEN_FILE;

public class WebViewActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    //Constants for touchlistener
    private static final int NONE = 0;
    private static final int SWIPE = 1;
    private static final int TRESHOLD = 100;
    int webappID = -1;
    private WebView wv;
    private ProgressBar progressBar;
    private boolean currently_reloading = true;
    private GeolocationPermissions.Callback geo_callback = null;
    private String geo_origin = null;
    private DownloadManager.Request dl_request = null;
    private Map<String, String> CUSTOM_HEADERS;
    protected ValueCallback<Uri[]> filePathCallback;

    private boolean quit_on_next_backpress = false;
    private String last_onbackpress_url = "";
    private Handler reload_handler = null;
    private WebApp webapp = null;


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        DataManager.getInstance().loadAppData();

        Utility.applyUITheme();
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
        webapp = DataManager.getInstance().getWebApp(webappID);

        if (webapp == null) {
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                String processName = Application.getProcessName();
                String packageName = this.getPackageName();

                // Sandboxed Web App is openend in main process using an old shortcut
                if(packageName.equals(processName) && webapp.isUseContainer()) {
                    ProcessPhoenix.triggerRebirth(this, Utility.createWebViewIntent(webapp, this));
                }

                if (!packageName.equals(processName)) {
                    if (SandboxManager.getInstance().isSandboxUsedByAnotherApp(webapp)) {
                        SandboxManager.getInstance().unregisterWebAppFromSandbox(webapp.getContainerId());
                        ProcessPhoenix.triggerRebirth(this, Utility.createWebViewIntent(webapp, this));
                    }
                    try {
                        SandboxManager.getInstance().registerWebAppToSandbox(webapp);
                        WebView.setDataDirectorySuffix(webapp.getContainerId() + webapp.getAlphanumericBaseUrl() + "_" + webapp.getID());
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

            }
            setContentView(R.layout.full_webview);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            String url = webapp.getLoadableUrl();

            wv = findViewById(R.id.webview);
            progressBar = findViewById(R.id.progressBar);

            if (webapp.isUseAdblock()) {
                wv.setVisibility(View.GONE);
                wv = findViewById(R.id.adblockwebview);
                wv.setVisibility(View.VISIBLE);
            }

            if (webapp.isUseCustomUserAgent()) {
                wv.getSettings().setUserAgentString(webapp.getUserAgent());
            }

            if (webapp.isShowFullscreen()) {
                this.hideSystemBars();
            }

            wv.setWebViewClient(new CustomBrowser());
            wv.getSettings().setSafeBrowsingEnabled(webapp.isSafeBrowsing());
            wv.getSettings().setDomStorageEnabled(true);
            wv.getSettings().setAllowFileAccess(true);
            wv.getSettings().setBlockNetworkLoads(false);
//        wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (webapp.isUseTimespanDarkMode() &&
                        Utility.isInInterval(Utility.convertStringToCalendar(webapp.getTimespanDarkModeBegin()), Calendar.getInstance(), Utility.convertStringToCalendar(webapp.getTimespanDarkModeEnd()))
                        || (!webapp.isUseTimespanDarkMode() && webapp.isForceDarkMode())) {
                    wv.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
                    wv.setBackgroundColor(Color.BLACK);

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                        WebSettingsCompat.setForceDarkStrategy(wv.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
                    }


                } else {
                    wv.getSettings().setForceDark(WebSettings.FORCE_DARK_OFF);
                }
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
            wv.setWebChromeClient(new CustomWebChromeClient());
            wv.setDownloadListener((dl_url, userAgent, contentDisposition, mimeType, contentLength) -> {

                if (mimeType.equals("application/pdf")) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(dl_url));
                    startActivity(i);
                } else {
                    DownloadManager.Request request = new DownloadManager.Request(
                            Uri.parse(dl_url));
                    String file_name = Utility.getFileNameFromDownload(dl_url, contentDisposition, mimeType);

                    request.setMimeType(mimeType);
                    request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(dl_url));
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setTitle(file_name);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, file_name);

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                        if (!EasyPermissions.hasPermissions(WebViewActivity.this, perms)) {
                            dl_request = request;
                            EasyPermissions.requestPermissions(WebViewActivity.this, getString(R.string.permission_storage_rationale), Const.PERMISSION_RC_STORAGE, perms);
                        } else {
                            if (dm != null) {
                                dm.enqueue(request);
                                Utility.showInfoSnackbar(this, getString(R.string.file_download), Snackbar.LENGTH_SHORT);
                            }
                        }
                    }
                    //No storage permission needed for Android 10+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (dm != null) {
                            dm.enqueue(request);
                            Utility.showInfoSnackbar(this, getString(R.string.file_download), Snackbar.LENGTH_SHORT);
                        }
                    }

                }
            });
            wv.setOnTouchListener(new View.OnTouchListener() {
                private int mode = NONE;
                private float startX;
                private float stopX;
                private float startY;
                private float stopY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    WebApp webapp = DataManager.getInstance().getWebApp(webappID);
                    if (webapp.getUrlOnFirstPageload() == null)
                        DataManager.getInstance().getWebApp(webappID).saveUrlOnFirstPageLoad(wv.getUrl());

                    if (webapp.isRequestDesktop())
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
                                    currently_reloading = true;
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
    }


    @Override
    public void onBackPressed() {
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        String current_onbackpress_url = wv.getUrl();

        if (current_onbackpress_url == null)
            moveTaskToBack(true);

        if (quit_on_next_backpress || Utility.URLEqual(current_onbackpress_url, webapp.getNonNullUrlOnFirstPageload()) || Utility.URLEqual(current_onbackpress_url, last_onbackpress_url)) {
            moveTaskToBack(true);
            quit_on_next_backpress = false;
        } else if (wv.canGoBack())
            wv.goBack();
        else {
            loadURL(wv, webapp.getBaseUrl());
            quit_on_next_backpress = true;
            DataManager.getInstance().getWebApp(webappID).saveUrlOnFirstPageLoad(wv.getUrl());
        }
        last_onbackpress_url = wv.getUrl();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webapp.isAutoreload()) {
            reload_handler = new Handler();
            reload();
        }
        int new_id = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        if (new_id != webappID) {
            WebApp new_webapp = DataManager.getInstance().getWebApp(new_id);
            ProcessPhoenix.triggerRebirth(this, Utility.createWebViewIntent(new_webapp, this));

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        if (webapp != null) {
            if (webapp.isRestorePage())
                webapp.saveCurrentUrl(wv.getUrl());
            if (webapp.isClearCache() || DataManager.getInstance().getSettings().isClearCache())
                wv.clearCache(true);

/*            LEGACY SETTING REMOVED WITH RELEASE 0.86/1.00
            if (DataManager.getInstance().getSettings().isClearCookies()) {
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
            }*/
        }
        if (reload_handler != null) {
            reload_handler.removeCallbacksAndMessages(null);
            Log.d("CLEANUP", "Stopped reload handler");
        }
    }

    private void reload() {
        reload_handler.postDelayed(() -> {
            currently_reloading = true;
            wv.reload();
            reload();
        }, webapp.getTimeAutoreload() * 1000);
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
            builder.setPositiveButton(getString(R.string.no_https_dialog_accept), (dialog, id) -> {
                webApp.allowHTTP();
                view.loadUrl(url, CUSTOM_HEADERS);
            });
            builder.setNegativeButton(getString(android.R.string.cancel), (dialog, id) -> finish());
            final AlertDialog dialog = builder.create();
            dialog.show();
        } else
            view.loadUrl(url, CUSTOM_HEADERS);

    }
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        if (requestCode == Const.PERMISSION_RC_LOCATION) {
            DataManager.getInstance().getWebApp(webappID).enableLocationAccess();

            if (geo_callback != null) {
                geo_callback.invoke(geo_origin, true, false);
                geo_callback = null;
            }
        }
        if (requestCode == Const.PERMISSION_RC_STORAGE) {
            if (dl_request != null) {
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(dl_request);
                    Utility.showInfoSnackbar(this, getString(R.string.file_download), Snackbar.LENGTH_SHORT);
                }
                dl_request = null;

            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        if (requestCode == Const.PERMISSION_RC_LOCATION) {
            if (geo_callback != null) {
                geo_callback.invoke(geo_origin, false, false);
                geo_callback = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_CANCELED && requestCode == CODE_OPEN_FILE) {
            this.filePathCallback.onReceiveValue(null);
        } else if (resultCode == RESULT_OK && requestCode == CODE_OPEN_FILE) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            filePathCallback = null;
        }
    }


    private class CustomWebChromeClient extends android.webkit.WebChromeClient {
        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        @Override
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> pFilePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {
            filePathCallback = pFilePathCallback;
            try {
                Intent intent = fileChooserParams.createIntent();
                startActivityForResult(intent, CODE_OPEN_FILE);
            } catch (Exception e) {
                Utility.showInfoSnackbar(WebViewActivity.this, getString(R.string.no_filemanager), Snackbar.LENGTH_LONG);
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawARGB(0, 0, 0, 0);
            return bitmap;
        }

        public void onHideCustomView()
        {
            ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View pView, WebChromeClient.CustomViewCallback pViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = pView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = pViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            hideSystemBars();
        }

            @Override
        public void onPermissionRequest(PermissionRequest request) {
            List<String> permissionsToGrant = new ArrayList<>();

            boolean containsDrmRequest = Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);

            if(containsDrmRequest && webapp.isDrmAllowed()) {
                permissionsToGrant.add(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);
            }
            if(containsDrmRequest && !webapp.isDrmAllowed()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                builder.setTitle(getString(R.string.dialog_permission_drm_title));
                builder.setMessage(getString(R.string.dialog_permission_drm_text))
                        .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                            webapp.setOverrideGlobalSettings(true);
                            webapp.setDrmAllowed(true);
                            DataManager.getInstance().replaceWebApp(webapp);
                            permissionsToGrant.add(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);
                        }).setNegativeButton(android.R.string.no, (dialog, id) -> {
                    webapp.setDrmAllowed(false);
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            request.grant(permissionsToGrant.stream().toArray(String[]::new));
        }

        public void onProgressChanged(WebView view, int progress) {

            if (DataManager.getInstance().getSettings().isShowProgressbar() || currently_reloading) {
                if (progressBar.getVisibility() == ProgressBar.GONE && progress < 100) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }

                progressBar.setProgress(progress);

                if (progress == 100) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    currently_reloading = false;
                }
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            WebApp webapp = DataManager.getInstance().getWebApp(webappID);

            String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            if (EasyPermissions.hasPermissions(WebViewActivity.this, perms)) {
                if (webapp.isAllowLocationAccess())
                    callback.invoke(origin, true, false);
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                    builder.setTitle(getString(R.string.dialog_permission_location_title));
                    builder.setMessage(getString(R.string.dialog_permission_location_txt))
                            .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                                webapp.setOverrideGlobalSettings(true);
                                webapp.enableLocationAccess();
                                DataManager.getInstance().replaceWebApp(webapp);
                                callback.invoke(origin, true, false);
                            }).setNegativeButton(android.R.string.no, (dialog, id) -> callback.invoke(origin, false, false));
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                geo_callback = callback;
                geo_origin = origin;
                EasyPermissions.requestPermissions(WebViewActivity.this, getString(R.string.permission_location_rationale), Const.PERMISSION_RC_LOCATION, perms);

            }
        }
    }

    private class CustomBrowser extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            if(url.equals("about:blank")) {
                String langExtension;
                switch(Locale.getDefault().getLanguage()) {
                    case "de":
                        langExtension = "de";
                        break;
                    default:
                        langExtension = "en";
                }
                wv.loadUrl("file:///android_asset/errorSite/error_" + langExtension + ".html");
            }
            super.onPageFinished(view, url);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (webapp.isBlockThirdPartyRequests()) {
                Uri uri = request.getUrl();
                Uri webapp_uri = Uri.parse(webapp.getBaseUrl());

                if (!uri.getHost().endsWith(webapp_uri.getHost())) {
//                    Timber.tag("BLOCK3rd").d("Uri: " + uri + "is not within the URL scope.");
                    return null;
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

            //This option is hidden in "expert settings"
            if (webapp.isIgnoreSslErrors()) {
                handler.proceed();
                return;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

            String message = getString(R.string.ssl_error_msg_line1) + " ";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message += getString(R.string.ssl_error_unknown_authority) + "\n";
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
            builder.setPositiveButton(getString(android.R.string.cancel), (dialog, id) -> handler.cancel());
            builder.setNegativeButton(getString(R.string.load_anyway), (dialog, id) -> handler.proceed());
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

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            WebApp webapp = DataManager.getInstance().getWebApp(webappID);

            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(intent);
                return true;
            }
            if (url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

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
    }
}


