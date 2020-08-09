package com.cylonid.nativealpha.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Utility;
import com.google.gson.Gson;
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

import static android.content.Context.MODE_PRIVATE;


public class DataManager {

    private static final String SHARED_PREF_KEY = "WEBSITEDATA";
    private static final String SHARED_PREF_LEGACY_KEY = "GLOBALSETTINGS";
    private static final String shared_pref_max_id  = "MAX_ID";
    private static final String shared_pref_glob_cache = "Cache";
    private static final String shared_pref_webappdata = "WEBSITEDATA";
    private static final String shared_pref_glob_cookie = "Cookies";
    private static final String shared_pref_glob_2fmultitouch = "TwoFingerMultiTouch";
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
            Gson gson = new Gson();
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
            settings.setThreeFingerMultitouch(appdata.getBoolean(shared_pref_glob_3fmultitouch, false));
            settings.setThemeId(appdata.getInt(shared_pref_glob_ui_theme, 0));
        }

        Utility.applyUITheme();
    }



    private void saveGlobalSettings() {
        Utility.Assert(App.getAppContext() != null, "App.getAppContext() null before saving appdata to sharedpref");

        appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        editor.putBoolean(shared_pref_glob_cache, settings.isClearCache());
        editor.putBoolean(shared_pref_glob_cookie, settings.isClearCookies());
        editor.putBoolean(shared_pref_glob_2fmultitouch, settings.isTwoFingerMultitouch());
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

    public void initDummyData()
    {
        loadAppData();
        WebApp d1 = new WebApp("orf.at");
        WebApp d2 = new WebApp("diepresse.com");
        WebApp d3 = new WebApp("oebb.at");

        addWebsite(d1);
        addWebsite(d2);
        addWebsite(d3);

    }

    public void addWebsite(WebApp new_site) {
            websites.add(new_site);
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
        return websites.get(i);
    }

    public void replaceWebApp(WebApp webapp) {
        int index = webapp.getID();
        websites.set(index, webapp);
        saveWebAppData();
    }


    public boolean saveSharedPreferencesToFile(Context context) {
        boolean res = false;
        File dst = new File(context.getExternalFilesDir(null), "settings_backup");
        ObjectOutputStream output = null;
        try {
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            output = new ObjectOutputStream(bytestream);
            appdata = App.getAppContext().getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE);
            output.writeObject(appdata.getAll());
            String checksum = Hasher.Companion.hash(new String(bytestream.toByteArray()), HashType.SHA_256);
            FileOutputStream fileOutputStream = new FileOutputStream(dst);
            fileOutputStream.write(Base64.encode(bytestream.toByteArray(), Base64.DEFAULT));
            fileOutputStream.write(0x5C);
            fileOutputStream.write(Base64.encode(checksum.getBytes(), Base64.DEFAULT));
            bytestream.close();
            res = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({ "unchecked" })
    public boolean loadSharedPreferencesFromFile(Context context) {
        boolean res = false;
        File src = new File(context.getExternalFilesDir(null), "settings_backup");
        String str_file = Utility.readFromFile(src);
        int sep_index = str_file.indexOf(0x5C);
        String webapps = new String(Base64.decode(str_file.substring(0, sep_index), Base64.DEFAULT));
        String checksum = new String(Base64.decode(str_file.substring(sep_index + 1), Base64.DEFAULT));
        String new_checksum = Hasher.Companion.hash(webapps, HashType.SHA_256);

        ObjectInputStream input = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(webapps); //write the string as object
            oos.close();
            input = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
//            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = context.getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE).edit();
            prefEdit.clear();

            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
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
            res = true;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return res;
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

