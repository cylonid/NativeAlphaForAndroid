package com.cylonid.nativealpha.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.Gravity;
import android.widget.Toast;

import com.cylonid.nativealpha.R;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.InvalidChecksumException;
import com.cylonid.nativealpha.util.Utility;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;


public class DataManager {

    private static final String SHARED_PREF_KEY = "WEBSITEDATA";
    private static final String SHARED_PREF_LEGACY_KEY = "GLOBALSETTINGS";
    private static final String shared_pref_max_id  = "MAX_ID";
    private static final String shared_pref_glob_cache = "Cache";
    private static final String shared_pref_webappdata = "WEBSITEDATA";
    private static final String shared_pref_glob_cookie = "Cookies";
    private static final String shared_pref_glob_2fmultitouch = "TwoFingerMultiTouch";
    private static final String shared_pref_glob_multitouch_reload = "ReloadMultiTouch";
    private static final String shared_pref_glob_3fmultitouch = "ThreeFingerMultiTouch";
    private static final String shared_pref_glob_ui_theme = "UITheme";
    private static final String shared_pref_ignore_legacy_settings = "ignoreLegacySettings";

    private static final DataManager instance = new DataManager();
    private ArrayList<WebApp> websites;
    private int max_assigned_ID;
    private SharedPreferences appdata;
  
    private GlobalSettings settings;

    private DataManager()
    {
        websites = new ArrayList<>();
        max_assigned_ID = -1;
        settings = new GlobalSettings();
    }

    public static DataManager getInstance(){
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
        Gson gson = new Gson();
        String json = gson.toJson(websites);
        editor.putString(shared_pref_webappdata, json);
        editor.putInt(shared_pref_max_id, max_assigned_ID);
        editor.apply();
    }

    public void loadAppData() {
        Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before loading sharedpref");

        appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
        //Webapp data
        if (appdata.contains(shared_pref_webappdata)) {

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(WebApp.class, new WebAppInstanceCreator());
            Gson gson = gsonBuilder.create();
            String json = appdata.getString(shared_pref_webappdata, "");
            websites = gson.fromJson(json, new TypeToken<ArrayList<WebApp>>() {}.getType());
        }

        max_assigned_ID = appdata.getInt(shared_pref_max_id, max_assigned_ID);

        //Check legacy app data
        if (!appdata.getBoolean(shared_pref_ignore_legacy_settings, false))
            applyLegacyGlobalSettings();
        else {
            //Global app data
            settings.setClearCache(appdata.getBoolean(shared_pref_glob_cache, false));
            settings.setClearCookies(appdata.getBoolean(shared_pref_glob_cookie, false));
            settings.setTwoFingerMultitouch(appdata.getBoolean(shared_pref_glob_2fmultitouch, true));
            settings.setMultitouchReload(appdata.getBoolean(shared_pref_glob_multitouch_reload, true));
            settings.setThreeFingerMultitouch(appdata.getBoolean(shared_pref_glob_3fmultitouch, false));
            settings.setThemeId(appdata.getInt(shared_pref_glob_ui_theme, 0));
        }

    }



    public void saveGlobalSettings() {
        Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before saving appdata to sharedpref");

        appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        editor.putBoolean(shared_pref_glob_cache, settings.isClearCache());
        editor.putBoolean(shared_pref_glob_cookie, settings.isClearCookies());
        editor.putBoolean(shared_pref_glob_2fmultitouch, settings.isTwoFingerMultitouch());
        editor.putBoolean(shared_pref_glob_multitouch_reload, settings.isMultitouchReload());
        editor.putBoolean(shared_pref_glob_3fmultitouch, settings.isThreeFingerMultitouch());
        editor.putInt(shared_pref_glob_ui_theme, settings.getThemeId());
        editor.putBoolean(shared_pref_ignore_legacy_settings, true);
        editor.apply();
    }

    private void applyLegacyGlobalSettings() {
        appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_LEGACY_KEY, MODE_PRIVATE);

        settings.setClearCache(appdata.getBoolean(shared_pref_glob_cache, false));
        settings.setClearCookies(appdata.getBoolean(shared_pref_glob_cookie, false));
        settings.setTwoFingerMultitouch(appdata.getBoolean(shared_pref_glob_2fmultitouch, true));
        settings.setThreeFingerMultitouch(appdata.getBoolean(shared_pref_glob_3fmultitouch, false));
        settings.setThemeId(appdata.getInt(shared_pref_glob_ui_theme, 0));

        appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);

    }

//    public void initDummyData()
//    {
//        loadAppData();
//        WebApp d1 = new WebApp("orf.at");
//        WebApp d2 = new WebApp("diepresse.com");
//        WebApp d3 = new WebApp("oebb.at");
//
//        addWebsite(d1);
//        addWebsite(d2);
//        addWebsite(d3);
//
//    }

    public void addWebsite(WebApp new_site) {
            websites.add(new_site);
            Utility.Assert(new_site.getBaseUrl().equals(websites.get(new_site.getID()).getBaseUrl()), "WebApp ID and array position out of sync.");
            saveWebAppData();
    }

    public int getIncrementedID() {
        max_assigned_ID++;
        return max_assigned_ID;
    }
    public ArrayList<WebApp> getWebsites() {
        Utility.Assert(websites != null, "Websites not loaded");
        return websites;
    }

    public WebApp getWebApp(int i) {

        try {
            return websites.get(i);
        }
        catch (IndexOutOfBoundsException e) {
            Toast toast = Toast.makeText(App.getAppContext(), App.getAppContext().getString(R.string.webapp_not_found), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
        }
        return null;
    }

    public void replaceWebApp(WebApp webapp) {
        int index = webapp.getID();
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


    public boolean saveSharedPreferencesToFile(Uri uri) {
        boolean result = false;
        try(FileOutputStream fos = (FileOutputStream) App.getAppContext().getContentResolver().openOutputStream(uri);
            Base64OutputStream b64os = new Base64OutputStream(fos, Base64.DEFAULT);  ObjectOutputStream oos = new ObjectOutputStream(b64os);) {
            appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
            TreeMap<String, ?> shared_pref_map = new TreeMap<>(appdata.getAll());

            oos.writeObject(Hasher.Companion.hash(shared_pref_map.toString(), HashType.SHA_256));
            oos.writeObject(shared_pref_map);

            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings({ "unchecked" })
    public boolean loadSharedPreferencesFromFile(Uri uri){
        boolean result = false;
        try (FileInputStream fis = (FileInputStream) App.getAppContext().getContentResolver().openInputStream(uri);
             Base64InputStream b64is = new Base64InputStream(fis, Base64.DEFAULT);
             ObjectInputStream ois = new ObjectInputStream(b64is);) {

            SharedPreferences.Editor prefEdit = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE).edit();
            prefEdit.clear();
            String checksum = (String) ois.readObject();
            TreeMap<String, ?> shared_pref_map = ((TreeMap<String, ?>) ois.readObject());
            String new_checksum = Hasher.Companion.hash(shared_pref_map.toString(), HashType.SHA_256);

            if (!checksum.equals(new_checksum))
                throw new InvalidChecksumException("Checksums between backup and restored settings do not match.");
            for (Map.Entry<String, ?> entry : shared_pref_map.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

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
            result = true;

        } catch (InvalidChecksumException | IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        return result;
    }

    public WebApp getSuccessor(int i) {
        int INVALID = websites.size();
        int neighbor = i;
        do {
            neighbor = neighbor + 1;
            if (neighbor == INVALID)
                neighbor = 0;
        }
        while (!websites.get(neighbor).isActiveEntry());
        return websites.get(neighbor);

    }
    public WebApp getPredecessor(int i) {
        int INVALID = -1;
        int neighbor = i;
        do {
            neighbor = neighbor - 1;
            if (neighbor == INVALID)
                neighbor = websites.size() - 1;
        }
        while (!websites.get(neighbor).isActiveEntry());
        return websites.get(neighbor);

//        if (i != (websites.size() - 1)) {
//            return websites.get(i + 1);
//        }
//        else
//            return websites.get(0);

//
//        if (i != 0) {
//            return websites.get(i - 1);
//        }
//        else
//            return websites.get(websites.size() - 1);
    }
}

