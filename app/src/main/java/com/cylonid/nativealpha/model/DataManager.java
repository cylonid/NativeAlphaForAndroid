package com.cylonid.nativealpha.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import timber.log.Timber;
import android.net.Uri;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.Gravity;
import android.widget.Toast;

import com.cylonid.nativealpha.R;
import com.cylonid.nativealpha.model.deserializer.GlobalSettingsDeserializer;
import com.cylonid.nativealpha.model.deserializer.WebAppDeserializer;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.InvalidChecksumException;
import com.cylonid.nativealpha.util.ShortcutIconUtils;
import com.cylonid.nativealpha.util.Utility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;

import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;

public class DataManager {

	private static final Set<String> ALLOWED_BACKUP_CLASSES = new HashSet<>(
			Arrays.asList("java.util.TreeMap", "java.util.HashMap", "java.lang.String", "java.lang.Boolean",
					"java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Number"));

	// Corresponds to app release version
	private static final int LEGACY_DATA_FORMAT = 1000;

	private static final String SHARED_PREF_KEY = "WEBSITEDATA";
	private static final String GENERAL_INFO = "com.cylonid.nativealpha.GENERAL_INFO";

	public static final String ADBLOCK_CRASH = "adblockCrash";
	public static final String LAST_SHOWN_UPDATE = "lastShownUpdate";

	private static final String shared_pref_max_id = "MAX_ID";
	private static final String shared_pref_next_container = "NEXT_CONTAINER";

	private static final String shared_pref_webappdata = "WEBSITEDATA";
	private static final String shared_pref_globalsettings = "GLOBALSETTINGS";

	// TODO: Major cleanup...
	// Convert to Kotlin, streamline interface, save separated by uuid
	// <Legacy strings to be deleted in future>
	private static final String shared_pref_glob_cache = "Cache";
	private static final String shared_pref_glob_cookie = "Cookies";
	private static final String shared_pref_glob_2fmultitouch = "TwoFingerMultiTouch";
	private static final String shared_pref_glob_multitouch_reload = "ReloadMultiTouch";
	private static final String shared_pref_glob_3fmultitouch = "ThreeFingerMultiTouch";
	private static final String shared_pref_glob_progressbar = "LoadProgressBarAlwaysShown";
	private static final String shared_pref_glob_ui_theme = "UITheme";
	private static final String shared_pref_global_settings_json = "globalSettingsStoredAsJson";
	// </>

	private static final DataManager instance = new DataManager();
	private static final Gson GSON = new Gson();
	private static final Gson GSON_WITH_DESERIALIZERS = new GsonBuilder()
			.registerTypeAdapter(WebApp.class, new WebAppDeserializer())
			.registerTypeAdapter(GlobalSettings.class, new GlobalSettingsDeserializer()).create();
	private ArrayList<WebApp> websites;
	private int max_assigned_ID;
	private SharedPreferences appdata;

	private GlobalSettings settings;

	private DataManager() {
		websites = new ArrayList<>();
		max_assigned_ID = -1;
		settings = new GlobalSettings();
	}

	public static DataManager getInstance() {
		return instance;
	}

	public GlobalSettings getSettings() {
		return settings;
	}

	public void setSettings(GlobalSettings settings) {
		this.settings = settings;
		saveGlobalSettings();
	}

	public void saveWebAppData() {
		Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before saving sharedpref");

		appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
		SharedPreferences.Editor editor = appdata.edit();
		String json = GSON.toJson(websites);
		editor.putString(shared_pref_webappdata, json);
		editor.putInt(shared_pref_max_id, max_assigned_ID);
		editor.putInt(shared_pref_next_container, SandboxManager.getInstance().getNextContainer());
		editor.apply();
	}

	public boolean getHasAdblockCrashed() {
		return getGeneralInfo().getBoolean(ADBLOCK_CRASH, false);
	}

	public int getLastShownUpdate() {
		return getGeneralInfo().getInt(LAST_SHOWN_UPDATE, 0);
	}

	public void setHasAdblockCrashed(boolean newValue) {
		getGeneralInfo().edit().putBoolean(ADBLOCK_CRASH, newValue).apply();
	}

	public void setLastShownUpdate(int newValue) {
		getGeneralInfo().edit().putInt(LAST_SHOWN_UPDATE, newValue).apply();
	}

	private SharedPreferences getGeneralInfo() {
		Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before saving sharedpref");
		return App.getAppContext().getSharedPreferences(GENERAL_INFO, MODE_PRIVATE);

	}

	private void checkIfWebAppIdsCollide(ArrayList<WebApp> oldWebApps, ArrayList<WebApp> newWebApps) {
		if (oldWebApps == null || newWebApps == null)
			return;
		int end = Math.min(oldWebApps.size(), newWebApps.size());
		ArrayList<Integer> shortcuts_to_be_removed = new ArrayList<>();

		for (int i = 0; i < end; i++) {
			if (oldWebApps.get(i) != null && newWebApps.get(i) != null) {
				if (!oldWebApps.get(i).getBaseUrl().equals(newWebApps.get(i).getBaseUrl())) {
					shortcuts_to_be_removed.add(newWebApps.get(i).getID());
				}
			}
		}

		Context context = App.getAppContext();
		if (context != null && !shortcuts_to_be_removed.isEmpty()) {
			ShortcutIconUtils.deleteShortcuts(shortcuts_to_be_removed, context);
		}
	}

	public void loadAppData() {
		Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before loading sharedpref");

		appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
		// Webapp data
		if (appdata.contains(shared_pref_webappdata)) {
			String json = appdata.getString(shared_pref_webappdata, "");
			int oldDataFormat = DataVersionConverter.getDataFormat(json);
			String currentDataFormattedJson = this.checkDataFormat(oldDataFormat, json);
			try {
				ArrayList<WebApp> new_websites = GSON_WITH_DESERIALIZERS.fromJson(currentDataFormattedJson,
						new TypeToken<ArrayList<WebApp>>() {
						}.getType());
				if (new_websites != null) {
					checkIfWebAppIdsCollide(websites, new_websites);
					websites = new_websites;
					if (oldDataFormat != DataVersionConverter.getDataFormat(currentDataFormattedJson)) {
						this.saveWebAppData();
					}
				} else {
					Timber.w("Deserialized null websites list, keeping fallback list");
					if (websites == null) {
						websites = new ArrayList<>();
					}
				}
			} catch (JsonSyntaxException e) {
				Timber.e(e, "Failed to deserialize websites JSON");
				if (websites == null) {
					websites = new ArrayList<>();
				}
			}
		}

		if (websites != null && App.getAppContext() != null) {
			ArrayList<Integer> validIds = new ArrayList<>();
			for (WebApp w : websites) {
				if (w != null && w.isActiveEntry()) {
					validIds.add(w.getID());
				}
			}
			ShortcutIconUtils.cleanupOrphanedIcons(App.getAppContext(), validIds);
		}

		max_assigned_ID = appdata.getInt(shared_pref_max_id, max_assigned_ID);
		SandboxManager.getInstance().setNextContainer(appdata.getInt(shared_pref_next_container, 0));

		if (appdata.getBoolean(shared_pref_global_settings_json, false)) {
			loadGlobalSettingsLegacy();
		}
		// Global settings
		if (appdata.contains(shared_pref_globalsettings)) {
			String json = appdata.getString(shared_pref_globalsettings, "");
			int oldDataFormat = DataVersionConverter.getDataFormat(json);
			String currentDataFormattedJson = this.checkDataFormat(oldDataFormat, json);
			try {
				GlobalSettings loadedSettings = GSON_WITH_DESERIALIZERS.fromJson(currentDataFormattedJson,
						GlobalSettings.class);
				if (loadedSettings != null) {
					settings = loadedSettings;
				} else {
					Timber.w("Deserialized null GlobalSettings, falling back to defaults");
					settings = new GlobalSettings();
				}
			} catch (JsonSyntaxException e) {
				Timber.e(e, "Failed to deserialize GlobalSettings JSON, falling back to defaults");
				settings = new GlobalSettings();
			}
			assertGlobalWebappData();
			if (oldDataFormat != DataVersionConverter.getDataFormat(currentDataFormattedJson))
				this.saveGlobalSettings();
		}

	}

	public void loadGlobalSettingsLegacy() {
		settings.setClearCache(appdata.getBoolean(shared_pref_glob_cache, false));
		settings.setClearCookies(appdata.getBoolean(shared_pref_glob_cookie, false));
		settings.setTwoFingerMultitouch(appdata.getBoolean(shared_pref_glob_2fmultitouch, true));
		settings.setMultitouchReload(appdata.getBoolean(shared_pref_glob_multitouch_reload, true));
		settings.setThreeFingerMultitouch(appdata.getBoolean(shared_pref_glob_3fmultitouch, false));
		settings.setShowProgressbar(appdata.getBoolean(shared_pref_glob_progressbar, false));
		settings.setThemeId(appdata.getInt(shared_pref_glob_ui_theme, 0));
	}

	public void saveGlobalSettings() {
		Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before saving appdata to sharedpref");

		appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
		SharedPreferences.Editor editor = appdata.edit();

		String json = GSON.toJson(settings);
		editor.putString(shared_pref_globalsettings, json);
		editor.putBoolean(shared_pref_global_settings_json, true);
		editor.apply();
	}

	public void addWebsite(WebApp new_site) {
		websites.add(new_site);
		saveWebAppData();
	}

	public int getIncrementedID() {
		return getWebsites().size();
	}

	public int getIncrementedOrder() {
		return getActiveWebsitesCount() + 1;
	}

	public ArrayList<WebApp> getWebsites() {
		Utility.Assert(websites != null, "Websites not loaded");
		return websites;
	}

	public ArrayList<WebApp> getActiveWebsites() {
		ArrayList<WebApp> active_webapps = new ArrayList<>();

		for (WebApp webapp : websites) {
			if (webapp.isActiveEntry())
				active_webapps.add(webapp);
		}
		active_webapps.sort(Comparator.comparingInt(WebApp::getOrder));
		return active_webapps;
	}

	public WebApp getWebApp(int i) {
		return getWebAppIgnoringGlobalOverride(i, false);
	}

	public WebApp getWebAppIgnoringGlobalOverride(int i, boolean ignoreOverride) {
		if (websites == null || websites.isEmpty()) {
			loadAppData();
		}
		try {
			if (i < 0 || i >= websites.size()) {
				throw new IndexOutOfBoundsException("Index " + i + " out of bounds for size " + websites.size());
			}
			WebApp stored = websites.get(i);
			if (stored == null) {
				return null;
			}
			if (!stored.isOverrideGlobalSettings() && !ignoreOverride) {
				WebApp effective = stored.deepCopy();
				effective.copySettings(settings.getGlobalWebApp());
				return effective;
			}
			return stored;
		} catch (IndexOutOfBoundsException e) {
			Context context = App.getAppContext();
			if (context != null && Looper.getMainLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
				Toast toast = Toast.makeText(context, context.getString(R.string.webapp_not_found), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP, 0, 100);
				toast.show();
			} else {
				Timber.w("WebApp %d not found (size=%d)", i, websites.size());
			}
		}
		return null;
	}

	public void replaceWebApp(WebApp webapp) {
		if (webapp == null) {
			return;
		}
		int index = webapp.getID();
		if (index < 0 || index >= websites.size()) {
			Timber.w("Ignoring replaceWebApp for out-of-bounds id %d (size=%d)", index, websites.size());
			return;
		}
		websites.set(index, webapp);
		saveWebAppData();
	}

	public int getActiveWebsitesCount() {
		int c = 0;
		for (WebApp webapp : websites) {
			if (webapp.isActiveEntry())
				c += 1;
		}
		return c;
	}

	public boolean saveSharedPreferences(OutputStream os) {
		try (Base64OutputStream b64os = new Base64OutputStream(os, Base64.DEFAULT);
				ObjectOutputStream oos = new ObjectOutputStream(b64os)) {
			Context context = App.getAppContext();
			if (context == null) {
				return false;
			}
			appdata = context.getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
			TreeMap<String, ?> shared_pref_map = new TreeMap<>(appdata.getAll());

			oos.writeObject(Hasher.Companion.hash(shared_pref_map.toString(), HashType.SHA_256));
			oos.writeObject(shared_pref_map);
			return true;
		} catch (Exception e) {
			Timber.e(e, "Failed to save shared preferences");
			return false;
		}
	}

	public boolean saveSharedPreferencesToFile(Uri uri) {
		Context context = App.getAppContext();
		if (context == null) {
			return false;
		}
		try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
			if (os == null) {
				return false;
			}
			return saveSharedPreferences(os);
		} catch (Exception e) {
			Timber.e(e, "Failed to save shared preferences to file");
			return false;
		}
	}

	public boolean loadSharedPreferences(InputStream is) {
		try (Base64InputStream b64is = new Base64InputStream(is, Base64.DEFAULT);
				ObjectInputStream ois = new ObjectInputStream(b64is) {
					@Override
					protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
						String name = desc.getName();
						if (!ALLOWED_BACKUP_CLASSES.contains(name)) {
							throw new InvalidClassException("Unauthorized deserialization class", name);
						}
						return super.resolveClass(desc);
					}

					@Override
					protected Class<?> resolveProxyClass(String[] interfaces)
							throws IOException, ClassNotFoundException {
						throw new InvalidClassException("Unauthorized proxy class in backup");
					}
				}) {

			Object checksumObj = ois.readObject();
			if (!(checksumObj instanceof String)) {
				throw new IOException("Invalid backup format: expected checksum string.");
			}
			String checksum = (String) checksumObj;
			Object readObj = ois.readObject();
			if (!(readObj instanceof Map<?, ?>)) {
				throw new IOException("Invalid backup format: expected map data.");
			}
			Map<?, ?> shared_pref_map = (Map<?, ?>) readObj;
			String new_checksum = Hasher.Companion.hash(shared_pref_map.toString(), HashType.SHA_256);

			if (!checksum.equals(new_checksum)) {
				throw new InvalidChecksumException("Checksums between backup and restored settings do not match.");
			}

			Context context = App.getAppContext();
			if (context == null) {
				return false;
			}
			SharedPreferences.Editor prefEdit = context.getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE).edit();
			prefEdit.clear();
			for (Map.Entry<?, ?> entry : shared_pref_map.entrySet()) {
				Object v = entry.getValue();
				String key = String.valueOf(entry.getKey());

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, (Boolean) v);
				else if (v instanceof Float)
					prefEdit.putFloat(key, (Float) v);
				else if (v instanceof Integer)
					prefEdit.putInt(key, (Integer) v);
				else if (v instanceof Long)
					prefEdit.putLong(key, (Long) v);
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.apply();
			return true;

		} catch (InvalidChecksumException | IOException | ClassNotFoundException | ClassCastException e) {
			Timber.e(e, "Failed to load shared preferences");
			return false;
		}
	}

	public boolean loadSharedPreferencesFromFile(Uri uri) {
		Context context = App.getAppContext();
		if (context == null) {
			return false;
		}
		try (InputStream is = context.getContentResolver().openInputStream(uri)) {
			if (is == null) {
				return false;
			}
			return loadSharedPreferences(is);
		} catch (Exception e) {
			Timber.e(e, "Failed to load shared preferences from file");
			return false;
		}
	}

	private String checkDataFormat(int dataFormat, String jsonInput) {
		switch (dataFormat) {
			case LEGACY_DATA_FORMAT :
				String convertedInput = DataVersionConverter.convertToDataFormat(jsonInput,
						DataVersionConverter.getLegacyTo1300Map());
				return convertedInput;
			default :
			case 1300 : // Current data format => corresponding to app release version
				return jsonInput;
		}
	}

	public WebApp getSuccessor(int i) {
		int size = websites.size();
		if (size == 0) {
			return null;
		}
		int neighbor = (i >= 0 && i < size) ? i : 0;
		for (int step = 0; step < size; step++) {
			neighbor = (neighbor + 1) % size;
			WebApp candidate = websites.get(neighbor);
			if (candidate != null && candidate.isActiveEntry()) {
				return candidate;
			}
		}
		return null;
	}

	public WebApp getPredecessor(int i) {
		int size = websites.size();
		if (size == 0) {
			return null;
		}
		int neighbor = (i >= 0 && i < size) ? i : 0;
		for (int step = 0; step < size; step++) {
			neighbor = (neighbor - 1 + size) % size;
			WebApp candidate = websites.get(neighbor);
			if (candidate != null && candidate.isActiveEntry()) {
				return candidate;
			}
		}
		return null;
	}

	private void assertGlobalWebappData() {
		boolean override = settings.getGlobalWebApp().isOverrideGlobalSettings();
		int container = settings.getGlobalWebApp().getContainerId();
		if (!override || container != Const.NO_CONTAINER) {
			settings.getGlobalWebApp().setOverrideGlobalSettings(true);
			settings.getGlobalWebApp().setContainerId(Const.NO_CONTAINER);
			this.saveGlobalSettings();
		}
	}

}
