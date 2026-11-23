package com.cylonid.nativealpha;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.cylonid.nativealpha.databinding.DialogHttpAuthBinding;
import com.cylonid.nativealpha.helper.AdblockLifecycleHelper;
import com.cylonid.nativealpha.helper.AdblockProviderApiHelper;
import com.cylonid.nativealpha.helper.BiometricPromptHelper;
import com.cylonid.nativealpha.model.AdblockConfig;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.SandboxManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.DateUtils;
import com.cylonid.nativealpha.util.EntryPointUtils;
import com.cylonid.nativealpha.util.LocaleUtils;
import com.cylonid.nativealpha.util.NotificationUtils;
import com.cylonid.nativealpha.util.ShortcutIconUtils;
import com.cylonid.nativealpha.util.Utility;
import com.cylonid.nativealpha.util.WebViewLauncher;
import com.google.android.material.snackbar.Snackbar;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.cylonid.nativealpha.ui.webapp.WebAppMenuStyle;
import com.cylonid.nativealpha.ui.webapp.WebViewMenuLauncher;
import com.cylonid.nativealpha.ui.webapp.WebViewMenuListener;
import io.github.edsuns.adfilter.AdFilter;
import io.github.edsuns.adfilter.Filter;
import io.github.edsuns.adfilter.FilterResult;
import timber.log.Timber;
import pub.devrel.easypermissions.EasyPermissions;

public class WebViewActivity extends AppCompatActivity
		implements
			EasyPermissions.PermissionCallbacks,
			WebViewMenuListener {

	// Constants for touchlistener
	private static final int NONE = 0;
	private static final int SWIPE = 1;
	private static final int SWIPE_THRESHOLD = 100;
	int webappID = -1;
	private WebView wv;
	private ProgressBar progressBar;
	private boolean currently_reloading = true;
	private GeolocationPermissions.Callback mGeoPermissionRequestCallback = null;
	private String mGeoPermissionRequestOrigin = null;
	private DownloadManager.Request dl_request = null;
	private Map<String, String> CUSTOM_HEADERS;
	protected ValueCallback<Uri[]> filePathCallback;
	private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
				if (filePathCallback != null) {
					if (result.getResultCode() == RESULT_OK && result.getData() != null) {
						filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams
								.parseResult(result.getResultCode(), result.getData()));
					} else {
						filePathCallback.onReceiveValue(null);
					}
					filePathCallback = null;
				}
			});

	private boolean quitOnNextBackpress = false;
	private final Handler backPressHandler = new Handler(Looper.getMainLooper());
	private final Runnable resetBackPressRunnable = () -> quitOnNextBackpress = false;
	private Handler reload_handler = null;
	private WebApp webapp = null;
	private boolean fallbackToDefaultLongClickBehaviour = false;

	private AdFilter adFilter;

	private AdblockProviderApiHelper adblockProviderApiHelper;
	private AdblockLifecycleHelper adblockLifecycleHelper;
	private Bitmap mCurrentTaskIcon = null;

	private void updateTaskDescription(String title, Bitmap icon) {
		final String effectiveTitle = (title != null && !title.isEmpty()) ? title : getString(R.string.app_name);
		setTitle(effectiveTitle);
		try {
			if (Build.VERSION.SDK_INT >= 37) {
				// On API 37+, TaskDescription.Builder supports setIcon(Icon).
				ActivityManager.TaskDescription.Builder builder = new ActivityManager.TaskDescription.Builder()
						.setLabel(effectiveTitle);
				if (icon != null) {
					Bitmap scaled = ShortcutIconUtils.downscaleIfNecessary(icon, ShortcutIconUtils.MAX_ICON_DIMENSION);
					builder.setIcon(Icon.createWithBitmap(scaled));
				}
				setTaskDescription(builder.build());
			} else {
				// On API < 37, TaskDescription.Builder only accepts a drawable resource ID
				// (int).
				// To provide a dynamic Bitmap icon, we use the TaskDescription(String, Bitmap)
				// constructor.
				Bitmap scaledIcon = icon != null
						? ShortcutIconUtils.downscaleIfNecessary(icon, ShortcutIconUtils.MAX_ICON_DIMENSION)
						: null;
				@SuppressWarnings("deprecation")
				ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(effectiveTitle, scaledIcon);
				setTaskDescription(td);
			}
		} catch (Exception e) {
			// Ignore if OS does not support task description updates in current state
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adblockLifecycleHelper = new AdblockLifecycleHelper(this);
		adblockLifecycleHelper.trySyncOperation(() -> {
			adFilter = AdblockLifecycleHelper.initAdFilter(getApplicationContext());
		});

		adblockProviderApiHelper = new AdblockProviderApiHelper(adFilter);
		webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
		EntryPointUtils.entryPointReached();
		webapp = DataManager.getInstance().getWebApp(webappID);
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				handleBackPressed();
			}
		});

		if (webapp == null) {
			// Toast is shown in getWebApp method
			finish();
		} else {
			// Set the task description as early as possible so Recent Apps shows
			// the web app label instead of the generic app name.
			mCurrentTaskIcon = ShortcutIconUtils.getIcon(this, webapp.getID());
			if (mCurrentTaskIcon == null) {
				mCurrentTaskIcon = ShortcutIconUtils.createMonogramIcon(webapp.getTitle(), 192);
			}
			updateTaskDescription(webapp.getTitle(), mCurrentTaskIcon);
			if (webapp.isBiometricProtection()) {
				new BiometricPromptHelper(WebViewActivity.this).showPrompt(() -> setupWebView(), () -> finish(),
						getString(R.string.bioprompt_restricted_webapp));
			}
			setupWebView();
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setupWebView() {

		String processName = Application.getProcessName();
		String packageName = this.getPackageName();

		// Sandboxed Web App is openend in main process using an old shortcut
		if (packageName.equals(processName) && webapp.isUseContainer()) {
			WebViewLauncher.startWebViewInNewProcess(webapp, this);
		}

		if (!packageName.equals(processName)) {
			if (SandboxManager.getInstance().isSandboxUsedByAnotherApp(webapp)) {
				SandboxManager.getInstance().unregisterWebAppFromSandbox(webapp.getContainerId());
				WebViewLauncher.startWebViewInNewProcess(webapp, this);
			}
			try {
				SandboxManager.getInstance().registerWebAppToSandbox(webapp);
				WebView.setDataDirectorySuffix(
						webapp.getContainerId() + webapp.getAlphanumericBaseUrl() + "_" + webapp.getID());
			} catch (IllegalStateException e) {
				Timber.e(e, "Failed to set data directory suffix");
			}
		}

		setContentView(R.layout.full_webview);

		if (webapp.isKeepAwake()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		String url = webapp.getBaseUrl();

		wv = findViewById(R.id.webview);
		progressBar = findViewById(R.id.progressBar);

		List<AdblockConfig> adblockConfigs = DataManager.getInstance().getSettings().getGlobalWebApp()
				.getAdBlockSettings();
		if (webapp.isUseAdblock() && !adblockConfigs.isEmpty() && adFilter != null) {
			wv.setVisibility(View.GONE);
			wv = findViewById(R.id.adblockwebview);
			wv.setVisibility(View.VISIBLE);

			try {
				adFilter.setupWebView(wv);
				if (adblockProviderApiHelper != null) {
					adblockLifecycleHelper.trySyncOperation(
							() -> adblockProviderApiHelper.synchronizeAdblockProviderWithSettings(adblockConfigs));
				}

				if (adFilter.getViewModel() != null) {
					if (adFilter.getViewModel().getOnDirty() != null) {
						adFilter.getViewModel().getOnDirty().observe(this, none -> wv.clearCache(false));
					}
					if (adFilter.getViewModel().getEnabledFilterCount() != null) {
						adFilter.getViewModel().getEnabledFilterCount().observe(this, count -> {
							if (count != null && count == adblockConfigs.size()) {
								adblockLifecycleHelper.afterAdblockOperation();
							}
						});
					}
				}
			} catch (Throwable t) {
				Timber.e(t, "Error configuring adblock for WebView");
			}
		}

		String fieldName = Stream.of(WebViewActivity.class.getDeclaredFields())
				.filter(f -> f.getType() == WebView.class).findFirst().orElseThrow(null).getName();
		String uaString = wv.getSettings().getUserAgentString().replace("; " + fieldName, "");
		wv.getSettings().setUserAgentString(uaString);
		if (webapp.isUseCustomUserAgent()) {
			if (webapp.getUserAgent() != null && !webapp.getUserAgent().isEmpty()) {
				wv.getSettings().setUserAgentString(
						webapp.getUserAgent().replace("\0", "").replace("\n", "").replace("\r", ""));
			}
		}

		if (webapp.isShowFullscreen()) {
			this.hideSystemBars();
		} else if (DataManager.getInstance().getSettings().getAlwaysShowSoftwareButtons()) {
			this.showSystemBars();
		}
		wv.setWebViewClient(new CustomBrowser());
		wv.getSettings().setSafeBrowsingEnabled(false);
		wv.getSettings().setDomStorageEnabled(true);
		wv.getSettings().setAllowFileAccess(true);
		wv.getSettings().setBlockNetworkLoads(false);
		// wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
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
		if (webapp.isEnableZooming()) {
			wv.getSettings().setSupportZoom(true);
			wv.getSettings().setBuiltInZoomControls(true);
		}

		CUSTOM_HEADERS = initCustomHeaders(webapp.isSendSavedataRequest());
		loadURL(wv, url);
		wv.setWebChromeClient(new CustomWebChromeClient());
		wv.setOnLongClickListener(view -> {
			if (webapp.getAlwaysUseFallbackContextMenu())
				return false;
			if (fallbackToDefaultLongClickBehaviour) {
				fallbackToDefaultLongClickBehaviour = false;
				return false;
			}
			showWebViewPopupMenu();
			return true;
		});

		wv.setDownloadListener((dl_url, userAgent, contentDisposition, mimeType, contentLength) -> {
			String file_name = Utility.getFileNameFromDownload(dl_url, contentDisposition, mimeType);
			boolean isPdf = (mimeType != null && mimeType.equalsIgnoreCase("application/pdf"))
					|| (file_name != null && file_name.toLowerCase(Locale.ROOT).endsWith(".pdf"))
					|| (dl_url != null && dl_url.toLowerCase(Locale.ROOT).split("\\?")[0].endsWith(".pdf"));

			String targetUrl = dl_url;
			if (targetUrl != null && targetUrl.startsWith("blob:")) {
				String stripped = targetUrl.replace("blob:", "");
				try {
					targetUrl = URLDecoder.decode(stripped, StandardCharsets.UTF_8.name());
				} catch (Exception e) {
					Timber.e(e, "Failed to decode blob download URL");
				}
			}

			if (isPdf && webapp != null && webapp.isUseBuiltinPdfViewer() && isSupportedPdfScheme(targetUrl)) {
				if (isHttpUrl(targetUrl) && !webapp.isAllowHttp()) {
					loadURL(wv, targetUrl);
					return;
				}
				String cookie = CookieManager.getInstance().getCookie(targetUrl);
				Intent intent = new Intent(WebViewActivity.this,
						com.cylonid.nativealpha.activities.PdfViewerActivity.class);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_PDF_URL, targetUrl);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_FILE_NAME, file_name);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_USER_AGENT, userAgent);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_COOKIE, cookie);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_WEBAPP_ID, webappID);
				intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_ALLOW_HTTP,
						webapp.isAllowHttp());
				startActivity(intent);
			} else if (isPdf) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(Uri.parse(dl_url), "application/pdf");
				try {
					startActivity(i);
				} catch (ActivityNotFoundException e) {
					i.setData(Uri.parse(dl_url));
					try {
						startActivity(i);
					} catch (Exception ex) {
						Timber.e(ex, "Failed to launch external PDF viewer");
					}
				}
			} else {
				if (dl_url != null && !dl_url.isEmpty()) {
					if (dl_url.startsWith("blob:")) {
						dl_url = dl_url.replace("blob:", "");
						try {
							dl_url = URLDecoder.decode(dl_url, StandardCharsets.UTF_8.name());
						} catch (Exception e) {
							Timber.e(e, "Failed to decode blob download URL");
						}
					}
					DownloadManager.Request request;
					try {
						request = new DownloadManager.Request(Uri.parse(dl_url));
					} catch (Exception e) {
						NotificationUtils.showInfoSnackbar(this, getString(R.string.file_download),
								Snackbar.LENGTH_SHORT);
						return;
					}
					file_name = Utility.getFileNameFromDownload(dl_url, contentDisposition, mimeType);

					request.setMimeType(mimeType);
					request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(dl_url));
					request.addRequestHeader("User-Agent", userAgent);
					request.setTitle(file_name);
					request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file_name);

					DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
						String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
								Manifest.permission.READ_EXTERNAL_STORAGE};
						if (!EasyPermissions.hasPermissions(WebViewActivity.this, perms)) {
							dl_request = request;
							EasyPermissions.requestPermissions(WebViewActivity.this,
									getString(R.string.permission_storage_rationale), Const.PERMISSION_RC_STORAGE,
									perms);
						} else {
							if (dm != null) {
								dm.enqueue(request);
								NotificationUtils.showInfoSnackbar(this, getString(R.string.file_download),
										Snackbar.LENGTH_SHORT);
							}
						}
					}
					// No storage permission needed for Android 10+
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						if (dm != null) {
							dm.enqueue(request);
							NotificationUtils.showInfoSnackbar(this, getString(R.string.file_download),
									Snackbar.LENGTH_SHORT);
						}
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
					case MotionEvent.ACTION_POINTER_DOWN :
						// This happens when you touch the screen with two fingers
						mode = SWIPE;
						// You can also use event.getY(1) or the average of the two
						startX = event.getX(0);
						startY = event.getY(0);
						return true;

					case MotionEvent.ACTION_POINTER_UP :
						// This happens when you release the second finger
						mode = NONE;
						if (Math.abs(startX - stopX) > SWIPE_THRESHOLD) {
							if (startX > stopX) {
								if (event.getPointerCount() == 3
										&& DataManager.getInstance().getSettings().isThreeFingerMultitouch()) {
									WebViewLauncher.startWebView(DataManager.getInstance().getPredecessor(webappID),
											WebViewActivity.this);
									finish();
								} else if (DataManager.getInstance().getSettings().isTwoFingerMultitouch()) {
									if (wv.canGoForward())
										wv.goForward();
								}
							} else {
								if (event.getPointerCount() == 3
										&& DataManager.getInstance().getSettings().isThreeFingerMultitouch()) {
									WebViewLauncher.startWebView(DataManager.getInstance().getSuccessor(webappID),
											WebViewActivity.this);
									finish();
								} else if (DataManager.getInstance().getSettings().isTwoFingerMultitouch())
									handleBackPressed();

							}
							return true;
						}
						if (DataManager.getInstance().getSettings().isMultitouchReload()
								&& Math.abs(startY - stopY) > SWIPE_THRESHOLD) {
							if (stopY > startY) {
								currently_reloading = true;
								wv.reload();
							}
							return true;
						}
						break;
					case MotionEvent.ACTION_MOVE :
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
		Calendar beginCal = webapp.isUseTimespanDarkMode()
				? DateUtils.convertStringToCalendar(webapp.getTimespanDarkModeBegin())
				: null;
		Calendar endCal = webapp.isUseTimespanDarkMode()
				? DateUtils.convertStringToCalendar(webapp.getTimespanDarkModeEnd())
				: null;
		boolean needsForcedDarkMode = (beginCal != null && endCal != null
				&& DateUtils.isInInterval(beginCal, Calendar.getInstance(), endCal))
				|| (!webapp.isUseTimespanDarkMode() && webapp.isForceDarkMode());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			try {
				if (needsForcedDarkMode) {
					wv.setBackgroundColor(Color.BLACK);
					wv.setForceDarkAllowed(true);
					getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
					if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), true);
					}
				} else {
					getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
					wv.setBackgroundColor(Color.WHITE);
					wv.setForceDarkAllowed(false);
					if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
						WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.getSettings(), false);
					}
				}
			} catch (Exception e) {
				Timber.w(e, "Dark mode web settings not supported on this device/environment");
			}
		}
	}

	private void showWebViewPopupMenu() {
		String currentUrl = wv.getUrl() != null ? wv.getUrl() : "";
		String pageTitle = wv.getTitle() != null && !wv.getTitle().isEmpty()
				? wv.getTitle()
				: (webapp != null ? webapp.getTitle() : "");
		WebAppMenuStyle style = WebAppMenuStyle.Companion
				.fromId(DataManager.getInstance().getSettings().getWebAppMenuStyle());
		WebViewMenuLauncher.show(this, style, pageTitle, currentUrl, wv.canGoBack(), wv.canGoForward(), webapp,
				findViewById(R.id.anchorCenterScreen), this);
	}

	@Override
	public void onBack() {
		handleBackPressed();
	}

	@Override
	public void onForward() {
		if (wv.canGoForward()) {
			wv.goForward();
		}
	}

	@Override
	public void onReload() {
		wv.reload();
	}

	@Override
	public void onShare() {
		if (wv.getUrl() != null) {
			new ShareCompat.IntentBuilder(WebViewActivity.this).setType("text/plain").setChooserTitle("Share URL")
					.setText(wv.getUrl()).startChooser();
		}
	}

	@Override
	public void onCopyUrl() {
		if (wv.getUrl() != null) {
			ClipboardManager clipboard = getSystemService(ClipboardManager.class);
			if (clipboard != null) {
				ClipData clip = ClipData.newPlainText("URL", wv.getUrl());
				clipboard.setPrimaryClip(clip);
			}
		}
	}

	@Override
	public void onMainMenu() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	@Override
	public void onFallbackContextMenu() {
		fallbackToDefaultLongClickBehaviour = true;
	}

	@Override
	public void onCloseWebApp() {
		finishAndRemoveTask();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.setDarkModeIfNeeded();
	}

	private void handleBackPressed() {
		WebApp webapp = DataManager.getInstance().getWebApp(webappID);
		if (webapp == null) {
			finish();
			return;
		}

		if (wv != null && wv.canGoBack()) {
			WebBackForwardList history = wv.copyBackForwardList();
			int currentIndex = history.getCurrentIndex();
			if (currentIndex > 0) {
				String currentUrl = wv.getUrl();
				if (currentUrl == null) {
					WebHistoryItem currentItem = history.getItemAtIndex(currentIndex);
					currentUrl = currentItem != null ? currentItem.getUrl() : "";
				}

				int targetIndex = currentIndex - 1;
				while (targetIndex >= 0) {
					WebHistoryItem item = history.getItemAtIndex(targetIndex);
					if (item != null && !isSameOrRedirectUrl(currentUrl, item.getUrl())) {
						break;
					}
					targetIndex--;
				}

				if (targetIndex >= 0) {
					int steps = targetIndex - currentIndex;
					wv.goBackOrForward(steps);
					quitOnNextBackpress = false;
					backPressHandler.removeCallbacks(resetBackPressRunnable);
					return;
				}
			}
		}

		if (quitOnNextBackpress) {
			quitOnNextBackpress = false;
			backPressHandler.removeCallbacks(resetBackPressRunnable);
			moveTaskToBack(true);
			return;
		}

		quitOnNextBackpress = true;
		NotificationUtils.showToast(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT);
		backPressHandler.removeCallbacks(resetBackPressRunnable);
		backPressHandler.postDelayed(resetBackPressRunnable, 2000);
	}

	private boolean isSameOrRedirectUrl(String url1, String url2) {
		if (url1 == null || url2 == null) {
			return false;
		}
		if (url1.equals(url2)) {
			return true;
		}
		String clean1 = normalizeUrlForComparison(url1);
		String clean2 = normalizeUrlForComparison(url2);
		return clean1.equals(clean2);
	}

	private String normalizeUrlForComparison(String url) {
		if (url == null) {
			return "";
		}
		String clean = url.trim();
		if (clean.endsWith("#")) {
			clean = clean.substring(0, clean.length() - 1);
		}
		if (clean.startsWith("http://")) {
			clean = "https://" + clean.substring(7);
		}
		if (clean.endsWith("/") && clean.length() > 8) {
			clean = clean.substring(0, clean.length() - 1);
		}
		return clean;
	}

	@Override
	protected void onResume() {
		super.onResume();
		int new_id = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);

		if (new_id != webappID) {
			WebApp new_webapp = DataManager.getInstance().getWebApp(new_id);
			WebViewLauncher.startWebViewInNewProcess(new_webapp, this);
		}

		// Refresh task description on every resume — covers re-entry from back stack
		// or system-restored tasks where the system may have reverted to the manifest
		// label.
		if (webapp != null) {
			updateTaskDescription(webapp.getTitle(), mCurrentTaskIcon);
		}

		wv.onResume();
		wv.resumeTimers();
		this.setDarkModeIfNeeded();

		if (webapp.isBiometricProtection()) {
			View fullActivityView = findViewById(R.id.webviewActivity);
			fullActivityView.setVisibility(View.GONE);
			new BiometricPromptHelper(WebViewActivity.this).showPrompt(
					() -> fullActivityView.setVisibility(View.VISIBLE), () -> finish(),
					getString(R.string.bioprompt_restricted_webapp));
		}
		if (webapp.isAutoreload()) {
			reload_handler = new Handler(Looper.getMainLooper());
			reload();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		if (wv != null) {
			wv.evaluateJavascript(
					"document.querySelectorAll('audio').forEach(x => x.pause());document.querySelectorAll('video').forEach(x => x.pause());",
					null);
			wv.onPause();
			wv.pauseTimers();

			if (webapp.isClearCache() || DataManager.getInstance().getSettings().isClearCache())
				wv.clearCache(true);
		}
		WebViewMenuLauncher.dismiss(this);

		if (reload_handler != null) {
			reload_handler.removeCallbacksAndMessages(null);
			Timber.d("Stopped reload handler");
		}
		if (backPressHandler != null && resetBackPressRunnable != null) {
			backPressHandler.removeCallbacks(resetBackPressRunnable);
		}
		quitOnNextBackpress = false;
	}

	@Override
	protected void onDestroy() {
		if (backPressHandler != null && resetBackPressRunnable != null) {
			backPressHandler.removeCallbacks(resetBackPressRunnable);
		}
		super.onDestroy();
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
		extraHeaders.put("X-REQUESTED-WITH", "");
		if (save_data)
			extraHeaders.put("Save-Data", "on");
		return Collections.unmodifiableMap(extraHeaders);
	}

	public static boolean isHttpUrl(final String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		try {
			final String scheme = Uri.parse(url).getScheme();
			return "http".equalsIgnoreCase(scheme);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isHttpOrHttpsUrl(final String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		try {
			final String scheme = Uri.parse(url).getScheme();
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isSupportedPdfScheme(final String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		try {
			final String scheme = Uri.parse(url).getScheme();
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
					|| "file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme);
		} catch (Exception e) {
			return false;
		}
	}

	private void loadURL(final WebView view, final String url) {
		final WebApp webApp = DataManager.getInstance().getWebApp(webappID);
		if (isHttpUrl(url) && !webApp.isAllowHttp()) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

			builder.setTitle(getString(R.string.no_https_dialog_title));
			builder.setMessage(getString(R.string.no_https_dialog_msg));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setPositiveButton(getString(R.string.no_https_dialog_accept), (dialog, id) -> {
				webApp.setAllowHttp(true);
				webApp.setOverrideGlobalSettings(true);
				WebApp stored = DataManager.getInstance().getWebAppIgnoringGlobalOverride(webappID, true);
				if (stored != null) {
					stored.setAllowHttp(true);
					stored.setOverrideGlobalSettings(true);
				}
				if (webapp != null) {
					webapp.setAllowHttp(true);
					webapp.setOverrideGlobalSettings(true);
				}
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
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
				getWindow().getDecorView());
		if (controller != null) {
			controller.hide(WindowInsetsCompat.Type.systemBars());
			controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
		}
	}

	private void showSystemBars() {
		if (webapp.isShowFullscreen())
			return;
		WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
		WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
				getWindow().getDecorView());
		if (controller != null) {
			controller.show(WindowInsetsCompat.Type.systemBars());
			controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
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
					NotificationUtils.showInfoSnackbar(this, getString(R.string.file_download), Snackbar.LENGTH_SHORT);
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

	private class CustomWebChromeClient extends android.webkit.WebChromeClient {
		private View mCustomView;
		private WebChromeClient.CustomViewCallback mCustomViewCallback;
		private int mOriginalOrientation;

		private void handlePermissionRequest(int titleRes, int messageRes, boolean currentState,
				String[] androidPermissions, int requestCode, List<String> permissionsToGrant,
				String[] webkitPermission, PermissionGrantedCallback successCallback) {
			boolean androidPermissionsMissing = !EasyPermissions.hasPermissions(WebViewActivity.this,
					androidPermissions);
			if (currentState && androidPermissionsMissing) {
				ActivityCompat.requestPermissions(WebViewActivity.this, androidPermissions, requestCode);
				return;
			}
			if (currentState && !androidPermissionsMissing) {
				permissionsToGrant.addAll(Arrays.asList(webkitPermission));
				handleGeoPermissionCallback(true);
				return;
			}

			new AlertDialog.Builder(WebViewActivity.this).setTitle(titleRes).setMessage(messageRes)
					.setPositiveButton(android.R.string.ok, (dialog, id) -> {
						enablePermissionBoolOnWebApp(successCallback);
						handleGeoPermissionCallback(true);
						permissionsToGrant.addAll(Arrays.asList(webkitPermission));
						if (androidPermissionsMissing) {
							ActivityCompat.requestPermissions(WebViewActivity.this, androidPermissions, requestCode);
						}
					}).setNegativeButton(android.R.string.cancel, (dialog, id) -> handleGeoPermissionCallback(false))
					.create().show();
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			super.onReceivedTitle(view, title);
			if (title != null && !title.isEmpty() && !title.startsWith("http://") && !title.startsWith("https://")) {
				updateTaskDescription(title, mCurrentTaskIcon);
			}
		}

		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			super.onReceivedIcon(view, icon);
			if (icon != null && webapp != null) {
				mCurrentTaskIcon = icon;
				if (!ShortcutIconUtils.hasIcon(WebViewActivity.this, webapp.getID())) {
					ShortcutIconUtils.saveIcon(WebViewActivity.this, webapp.getID(), icon);
				}
				String currentTitle = getTitle() != null && !getTitle().toString().isEmpty()
						? getTitle().toString()
						: webapp.getTitle();
				updateTaskDescription(currentTitle, icon);
			}
		}

		@Override
		public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> pFilePathCallback,
				WebChromeClient.FileChooserParams fileChooserParams) {
			filePathCallback = pFilePathCallback;
			try {
				Intent intent = fileChooserParams.createIntent();
				fileChooserLauncher.launch(intent);
			} catch (Exception e) {
				NotificationUtils.showInfoSnackbar(WebViewActivity.this, getString(R.string.no_filemanager),
						Snackbar.LENGTH_LONG);
				if (filePathCallback != null) {
					filePathCallback.onReceiveValue(null);
					filePathCallback = null;
				}
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
			this.mOriginalOrientation = getRequestedOrientation();
			this.mCustomViewCallback = pViewCallback;
			((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
			hideSystemBars();
		}

		@Override
		public void onPermissionRequest(PermissionRequest request) {
			List<String> permissionsToGrant = new ArrayList<>();

			boolean containsDrmRequest = Arrays.asList(request.getResources())
					.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);
			boolean containsCameraRequest = Arrays.asList(request.getResources())
					.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
			boolean containsMicrophoneRequest = Arrays.asList(request.getResources())
					.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE);

			if (containsDrmRequest) {
				this.handlePermissionRequest(R.string.dialog_permission_drm_title, R.string.dialog_permission_drm_txt,
						webapp.isDrmAllowed(), new String[]{}, -1, permissionsToGrant,
						new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID}, () -> webapp.setDrmAllowed(true));
			}
			if (containsCameraRequest) {
				this.handlePermissionRequest(R.string.dialog_permission_camera_title,
						R.string.dialog_permission_camera_txt, webapp.isCameraPermission(),
						new String[]{Manifest.permission.CAMERA}, Const.PERMISSION_CAMERA, permissionsToGrant,
						new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}, () -> webapp.setCameraPermission(true));
			}

			if (containsMicrophoneRequest) {
				this.handlePermissionRequest(R.string.dialog_permission_microphone_title,
						R.string.dialog_permission_microphone_txt, webapp.isMicrophonePermission(),
						new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS},
						Const.PERMISSION_AUDIO, permissionsToGrant,
						new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE},
						() -> webapp.setMicrophonePermission(true));
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
			this.handlePermissionRequest(R.string.dialog_permission_location_title,
					R.string.dialog_permission_location_txt, webapp.isAllowLocationAccess(),
					new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
					Const.PERMISSION_RC_LOCATION, Collections.emptyList(), new String[]{},
					() -> webapp.setAllowLocationAccess(true));

		}
	}

	private void showHttpAuthDialog(final HttpAuthHandler handler, String host, String realm) {
		DialogHttpAuthBinding localBinding = DialogHttpAuthBinding.inflate(LayoutInflater.from(this));
		new AlertDialog.Builder(this).setView(localBinding.getRoot()).setTitle(getString(R.string.http_auth_title))
				.setMessage(getString(R.string.enter_http_auth_credentials, realm, host))
				.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
					String username = localBinding.username.getText().toString();
					String password = localBinding.password.getText().toString();

					handler.proceed(username, password);

				}).setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> handler.cancel()).show();
	}

	private class CustomBrowser extends WebViewClient {

		@Override
		public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
			Timber.e("WebView render process gone. Crashed: %b", detail.didCrash());
			if (wv != null) {
				ViewGroup parent = (ViewGroup) wv.getParent();
				if (parent != null) {
					parent.removeView(wv);
				}
				wv.destroy();
				wv = null;
			}
			finish();
			return true;
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
			showHttpAuthDialog(handler, host, realm);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (url.equals("about:blank")) {
				String langExtension = LocaleUtils.getFileEnding();
				wv.loadUrl("file:///android_asset/errorSite/error_" + langExtension + ".html");
			}
			wv.evaluateJavascript(
					"document.addEventListener(\"visibilitychange\",function (event) {event.stopImmediatePropagation();},true);",
					null);
			super.onPageFinished(view, url);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			try {
				if (adFilter != null && webapp != null && webapp.isUseAdblock()) {
					adFilter.performScript(view, url);
				}
			} catch (Throwable t) {
				Timber.e(t, "Error performing adblock script");
			}
			super.onPageStarted(view, url, favicon);
		}

		@Nullable
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
			if (request == null) {
				return null;
			}

			if (webapp != null && webapp.isUseAdblock() && adFilter != null) {
				try {
					FilterResult filterResult = adFilter.shouldIntercept(view, request);
					if (filterResult != null && filterResult.getResourceResponse() != null) {
						return filterResult.getResourceResponse();
					}
				} catch (Throwable t) {
					Timber.e(t, "Error intercepting adblock request");
				}
			}
			if (webapp != null && webapp.isBlockThirdPartyRequests()) {
				Uri uri = request.getUrl();
				Uri webapp_uri = Uri.parse(webapp.getBaseUrl());

				if (uri != null && uri.getHost() != null && webapp_uri != null && webapp_uri.getHost() != null) {
					if (!uri.getHost().endsWith(webapp_uri.getHost())) {
						return new WebResourceResponse("text/plain", "utf-8", null);
					}
				}
			}
			return super.shouldInterceptRequest(view, request);
		}

		@Override
		public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

			// This option is hidden in "expert settings"
			if (webapp.isIgnoreSslErrors()) {
				handler.proceed();
				return;
			}

			final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

			String message = getString(R.string.ssl_error_msg_line1) + " ";
			switch (error.getPrimaryError()) {
				case SslError.SSL_UNTRUSTED :
					message += getString(R.string.ssl_error_unknown_authority) + "\n";
					break;
				case SslError.SSL_EXPIRED :
					message += getString(R.string.ssl_error_expired) + "\n";
					break;
				case SslError.SSL_IDMISMATCH :
					message += getString(R.string.ssl_error_id_mismatch) + "\n";
					break;
				case SslError.SSL_NOTYETVALID :
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
			// dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setPadding(5, 5, 5, 5);
			// dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(ContextCompat.getColor(WebViewActivity.this,
			// android.R.color.holo_orange_light));
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
					.setTextColor(ContextCompat.getColor(WebViewActivity.this, android.R.color.holo_red_dark));
			dialog.getButton(AlertDialog.BUTTON_POSITIVE)
					.setTextColor(ContextCompat.getColor(WebViewActivity.this, android.R.color.holo_green_dark));
		}

		@Override
		public void onLoadResource(WebView view, String url) {
			super.onLoadResource(view, url);

			if (DataManager.getInstance().getWebApp(webappID).isRequestDesktop())
				view.evaluateJavascript(
						"""
								 var needsForcedWidth = document.documentElement.clientWidth < 1200;
								 if(needsForcedWidth) {
								   document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px, initial-scale=' + (document.documentElement.clientWidth / 1200));
								 }
								""",
						null);
			view.evaluateJavascript(
					"document.addEventListener(    \"visibilitychange\"    , (event) => {         event.stopImmediatePropagation();    }  );",
					null);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			if (request == null || request.getUrl() == null) {
				return false;
			}
			if (!request.isForMainFrame()) {
				return false;
			}
			runOnUiThread(() -> setDarkModeIfNeeded());
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

			String method = request.getMethod();

			if ("GET".equalsIgnoreCase(method) && webapp != null && webapp.isUseBuiltinPdfViewer()
					&& isSupportedPdfScheme(url)) {
				String cleanUrl = url.toLowerCase(Locale.ROOT).split("\\?")[0].split("#")[0];
				if (cleanUrl.endsWith(".pdf")) {
					if (isHttpUrl(url) && !webapp.isAllowHttp()) {
						loadURL(view, url);
						return true;
					}
					String cookie = CookieManager.getInstance().getCookie(url);
					Intent intent = new Intent(view.getContext(),
							com.cylonid.nativealpha.activities.PdfViewerActivity.class);
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_PDF_URL, url);
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_FILE_NAME,
							Utility.getFileNameFromDownload(url, null, "application/pdf"));
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_USER_AGENT,
							wv != null && wv.getSettings() != null ? wv.getSettings().getUserAgentString() : null);
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_COOKIE, cookie);
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_WEBAPP_ID, webappID);
					intent.putExtra(com.cylonid.nativealpha.activities.PdfViewerActivity.EXTRA_ALLOW_HTTP,
							webapp.isAllowHttp());
					view.getContext().startActivity(intent);
					return true;
				}
			}

			if (webapp != null && webapp.isOpenUrlExternal()) {
				String base_url = webapp.getBaseUrl();
				if (base_url != null) {
					Uri uri = Uri.parse(base_url);
					String host = uri.getHost();
					if (host != null && !url.contains(host)) {
						view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
						return true;
					}
				}
			}

			if (webapp != null && isHttpUrl(url) && !webapp.isAllowHttp()) {
				loadURL(view, url);
				return true;
			}

			if ("GET".equalsIgnoreCase(method) && isHttpOrHttpsUrl(url)) {
				loadURL(view, url);
				return true;
			}

			return false;
		}
	}
}
