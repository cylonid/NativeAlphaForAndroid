package com.cylonid.nativealpha.util;

import android.app.Application;
import android.content.Context;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.cylonid.nativealpha.BuildConfig;
import com.cylonid.nativealpha.helper.AdblockLifecycleHelper;

import timber.log.Timber;

public class App extends Application {

	private static App instance;

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;
		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}
		AdblockLifecycleHelper.sanitizeFilterDownloadStates(this);
		if (!WorkManager.isInitialized()) {
			WorkManager.initialize(this, new Configuration.Builder().build());
		}
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		if (level >= TRIM_MEMORY_BACKGROUND || level == TRIM_MEMORY_UI_HIDDEN) {
			ShortcutIconUtils.clearMemoryCache();
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		ShortcutIconUtils.clearMemoryCache();
	}

	public static Context getAppContext() {
		return instance != null ? instance.getApplicationContext() : null;
	}
}
