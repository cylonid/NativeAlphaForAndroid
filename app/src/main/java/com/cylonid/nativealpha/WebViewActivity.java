package com.cylonid.nativealpha;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
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
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.cylonid.nativealpha.helper.BiometricPromptHelper;
import com.cylonid.nativealpha.helper.IconPopupMenuHelper;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.SandboxManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.EntryPointUtils;
import com.cylonid.nativealpha.util.LocaleUtils;
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
import java.util.stream.Stream;

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
    private GeolocationPermissions.Callback mGeoPermissionRequestCallback = null;
    private String mGeoPermissionRequestOrigin = null;
    private DownloadManager.Request dl_request = null;
    private Map<String, String> CUSTOM_HEADERS;
    protected ValueCallback<Uri[]> filePathCallback;

    private boolean quitOnNextBackpress = false;
    private Handler reload_handler = null;
    private WebApp webapp = null;
    private String urlOnFirstPageload = "";
    private boolean fallbackToDefaultLongClickBehaviour = false;
    private PopupMenu mPopupMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
        EntryPointUtils.entryPointReached(this);

        webapp = DataManager.getInstance().getWebApp(webappID);
        if (webapp == null) {
            // Toast is shown in getWebApp method
            finish();
        } else {
            if(webapp.isBiometricProtection()) {
                new BiometricPromptHelper(WebViewActivity.this).showPrompt(() -> setupWebView(), () -> finish(), getString(R.string.bioprompt_restricted_webapp));
            }
            setupWebView();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWebView() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = this.getPackageName();

            // Sandboxed Web App is openend in main process using an old shortcut
            if(packageName.equals(processName) && webapp.isUseContainer()) {
                ProcessPhoenix.triggerRebirth(this, Utility.createWebViewIntent(webapp, this));
            }

            if (!packageName.equals(processName) && SandboxManager.getInstance() != null) {
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

        if(webapp.isKeepAwake()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        String url = webapp.getBaseUrl();

        wv = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);

        if (webapp.isUseAdblock()) {
            wv.setVisibility(View.GONE);
            wv = findViewById(R.id.adblockwebview);
            wv.setVisibility(View.VISIBLE);
        }
        String fieldName = Stream.of(WebViewActivity.class.getDeclaredFields()).filter(f -> f.getType() == WebView.class).findFirst().orElseThrow(null).getName();
        String uaString = wv.getSettings().getUserAgentString().replace("; " + fieldName, "");
        wv.getSettings().setUserAgentString(uaString);
        if (webapp.isUseCustomUserAgent()) {
            wv.getSettings().setUserAgentString(webapp.getUserAgent().replace("\0", "").replace("\n", "").replace("\r", ""));
        }

        if (webapp.isShowFullscreen()) {
            this.hideSystemBars();
        } else {
            this.showSystemBars();
        }
        wv.setWebViewClient(new CustomBrowser());
        wv.getSettings().setSafeBrowsingEnabled(false);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setBlockNetworkLoads(false);
//        wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        this.setDarkModeIfNeeded();

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
        if(webapp.isEnableZooming()) {
            wv.getSettings().setSupportZoom(true);
            wv.getSettings().setBuiltInZoomControls(true);
        }

        CUSTOM_HEADERS = initCustomHeaders(webapp.isSendSavedataRequest());
        loadURL(wv, url);
        wv.setWebChromeClient(new CustomWebChromeClient());
        wv.setOnLongClickListener(view -> {
            if(fallbackToDefaultLongClickBehaviour) {
                fallbackToDefaultLongClickBehaviour = false;
                return false;
            }
            showWebViewPopupMenu();
            return true;
        });

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

    private void setDarkModeIfNeeded() {
        boolean needsForcedDarkMode = webapp.isUseTimespanDarkMode() &&
                Utility.isInInterval(Utility.convertStringToCalendar(webapp.getTimespanDarkModeBegin()), Calendar.getInstance(), Utility.convertStringToCalendar(webapp.getTimespanDarkModeEnd()))
                || (!webapp.isUseTimespanDarkMode() && webapp.isForceDarkMode());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Utility.isNightMode(this) || needsForcedDarkMode) {
                wv.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
            } else {
                wv.getSettings().setForceDark(WebSettings.FORCE_DARK_OFF);
            }

            if (needsForcedDarkMode) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDarkStrategy(wv.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
                }
                wv.setBackgroundColor(Color.BLACK);
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDarkStrategy(wv.getSettings(), WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);
                }
                wv.setBackgroundColor(Color.WHITE);
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void showWebViewPopupMenu() {
        View center = findViewById(R.id.anchorCenterScreen);
        mPopupMenu = IconPopupMenuHelper.getMenu(center, R.menu.wv_context_menu, WebViewActivity.this);

        String currentUrl = wv.getUrl();
        String title = currentUrl.length() < 32 ? currentUrl : currentUrl.substring(0, 32) + "…";
        SpannableString spanString = new SpannableString(title);
        spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0,     spanString.length(), 0); //fix the color to white
        spanString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spanString.length(), 0);
        mPopupMenu.getMenu().getItem(0).setTitle(spanString);
        if(wv.canGoForward()) mPopupMenu.getMenu().getItem(2).setVisible(true);

        mPopupMenu.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.cmItemForward:
                    wv.goForward();
                    return true;
                case R.id.cmItemBack:
                    onBackPressed();
                    return true;
                case R.id.cmItemReload:
                    wv.reload();
                    return true;
                case R.id.cmItemCopyUrl:
                    ClipboardManager clipboard =  getSystemService(ClipboardManager.class);
                    ClipData clip = ClipData.newPlainText("URL", wv.getUrl());
                    clipboard.setPrimaryClip(clip);
                    return true;
                case R.id.cmItemShareUrl:
                    new ShareCompat.IntentBuilder(WebViewActivity.this)
                            .setType("text/plain")
                            .setChooserTitle("Share URL")
                            .setText(wv.getUrl())
                            .startChooser();
                    return true;
                case R.id.cmItemCloseWebApp:
                    finishAndRemoveTask();
                    return true;
                case R.id.cmSelectText:
                    fallbackToDefaultLongClickBehaviour = true;
                    return true;
                case R.id.cmMainMenu:
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    return true;

            }
            return false;
        });

        mPopupMenu.show();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.setDarkModeIfNeeded();
    }

    @Override
    public void onBackPressed() {
        WebApp webapp = DataManager.getInstance().getWebApp(webappID);

        if(wv.canGoBack()) {
            wv.goBack();
            return;
        }

        if(quitOnNextBackpress) {
            quitOnNextBackpress = false;
            moveTaskToBack(true);
            return;
        }

        loadURL(wv, webapp.getBaseUrl());
        quitOnNextBackpress = true;

    }

    @Override
    protected void onResume() {
        super.onResume();
        int new_id = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

        if (new_id != webappID) {
            WebApp new_webapp = DataManager.getInstance().getWebApp(new_id);
            ProcessPhoenix.triggerRebirth(this, Utility.createWebViewIntent(new_webapp, this));
        }

        wv.onResume();
        wv.resumeTimers();
        this.setDarkModeIfNeeded();

        
        if(webapp.isBiometricProtection()) {
            View fullActivityView = findViewById(R.id.webviewActivity);
            fullActivityView.setVisibility(View.GONE);
            new BiometricPromptHelper(WebViewActivity.this).showPrompt(() -> fullActivityView.setVisibility(View.VISIBLE), () -> finish(), getString(R.string.bioprompt_restricted_webapp));
        }
        if (webapp.isAutoreload()) {
            reload_handler = new Handler();
            reload();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        wv.evaluateJavascript("document.querySelectorAll('audio').forEach(x => x.pause());", null);
        wv.onPause();
        wv.pauseTimers();
        if(mPopupMenu != null) mPopupMenu.dismiss();

        if (webapp.isClearCache() || DataManager.getInstance().getSettings().isClearCache())
            wv.clearCache(true);

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
        }, webapp.getTimeAutoreload() * 1000L);
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
                webApp.setAllowHttp(true);
                webApp.setOverrideGlobalSettings(true);
                DataManager.getInstance().saveWebAppData();
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

    private void showSystemBars() {

        if(webapp.isShowFullscreen()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @FunctionalInterface
    interface PermissionGrantedCallback {
        void execute();
    }

    private void enablePermissionBoolOnWebApp(PermissionGrantedCallback successCallback) {
        webapp.setOverrideGlobalSettings(true);
        successCallback.execute();
        DataManager.getInstance().replaceWebApp(webapp);
        wv.reload();
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> list) {
        if (requestCode == Const.PERMISSION_RC_LOCATION) {
            enablePermissionBoolOnWebApp(() -> webapp.setAllowLocationAccess(true));
            this.handleGeoPermissionCallback(true);
        }
        if (requestCode == Const.PERMISSION_CAMERA) {
            enablePermissionBoolOnWebApp(() -> webapp.setCameraPermission(true));
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
            this.handleGeoPermissionCallback(false);
        }
    }

    private void handleGeoPermissionCallback(boolean allow) {
        if (mGeoPermissionRequestCallback != null) {
            mGeoPermissionRequestCallback.invoke(mGeoPermissionRequestOrigin, allow, false);
            mGeoPermissionRequestCallback = null;
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

        private void handlePermissionRequest(String resId,
                                             boolean currentState,
                                             String[] androidPermissions,
                                             int requestCode,
                                             List<String> permissionsToGrant,
                                             String[] webkitPermission,
                                             PermissionGrantedCallback successCallback) {
            boolean androidPermissionsMissing = !EasyPermissions.hasPermissions(WebViewActivity.this, androidPermissions);
            if (currentState && androidPermissionsMissing) {
                ActivityCompat.requestPermissions(WebViewActivity.this, androidPermissions, requestCode);
                return;
            }
            if (currentState && !androidPermissionsMissing) {
                permissionsToGrant.addAll(Arrays.asList(webkitPermission));
                handleGeoPermissionCallback(true);
                return;
            }

            new AlertDialog.Builder(WebViewActivity.this).setTitle(getPermissionRequestStringResource("dialog_permission_", resId, "_title"))
                    .setMessage(getPermissionRequestStringResource("dialog_permission_", resId, "_txt"))
                    .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                        enablePermissionBoolOnWebApp(successCallback);
                        handleGeoPermissionCallback(true);
                        permissionsToGrant.addAll(Arrays.asList(webkitPermission));
                        if (androidPermissionsMissing) {
                            ActivityCompat.requestPermissions(WebViewActivity.this, androidPermissions, requestCode);
                        }
                    }).setNegativeButton(android.R.string.no, (dialog, id) -> handleGeoPermissionCallback(false)).create().show();
        }

        private String getPermissionRequestStringResource(String prefix, String variable, String suffix) {
            return getString(WebViewActivity.this.getResources().getIdentifier(prefix + variable + suffix, "string", WebViewActivity.this.getPackageName()));
        }

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

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
            showSystemBars();
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
            boolean containsCameraRequest = Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
            boolean containsMicrophoneRequest = Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE);

            if (containsDrmRequest) {
                this.handlePermissionRequest("drm", webapp.isDrmAllowed(), new String[]{}, -1, permissionsToGrant, new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID}, () -> webapp.setDrmAllowed(true));
            }
            if (containsCameraRequest) {
                this.handlePermissionRequest("camera", webapp.isCameraPermission(), new String[]{Manifest.permission.CAMERA}, Const.PERMISSION_CAMERA, permissionsToGrant, new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}, () -> webapp.setCameraPermission(true));
            }

            if (containsMicrophoneRequest) {
                this.handlePermissionRequest("microphone", webapp.isMicrophonePermission(), new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS}, Const.PERMISSION_AUDIO, permissionsToGrant, new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE}, () -> webapp.setMicrophonePermission(true));
            }

            request.grant(permissionsToGrant.toArray(new String[0]));
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
            mGeoPermissionRequestCallback = callback;
            mGeoPermissionRequestOrigin = origin;
            this.handlePermissionRequest("location", webapp.isAllowLocationAccess(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, Const.PERMISSION_RC_LOCATION, Arrays.asList(new String[]{}), new String[]{}, () -> webapp.setAllowLocationAccess(true));

        }
    }

    private class CustomBrowser extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            if(url.equals("about:blank")) {
                String langExtension = LocaleUtils.getFileEnding();
                wv.loadUrl("file:///android_asset/errorSite/error_" + langExtension + ".html");
            }
            wv.evaluateJavascript("document.addEventListener(\"visibilitychange\",function (event) {event.stopImmediatePropagation();},true);", null);
            super.onPageFinished(view, url);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if(urlOnFirstPageload.equals("")) urlOnFirstPageload = request.getUrl().toString();
            if (webapp.isBlockThirdPartyRequests()) {
                Uri uri = request.getUrl();
                Uri webapp_uri = Uri.parse(webapp.getBaseUrl());

                if (!uri.getHost().endsWith(webapp_uri.getHost())) {
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
            view.evaluateJavascript("document.addEventListener(    \"visibilitychange\"    , (event) => {         event.stopImmediatePropagation();    }  );", null);
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


