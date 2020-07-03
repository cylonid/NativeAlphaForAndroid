package com.cylonid.nativealpha;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;

import okhttp3.internal.Util;


public class DataManager {

    private static final String shared_pref_data = "WEBSITEDATA";
    private static final String shared_pref_max_id  = "MAX_ID";
    private static final String shared_pref_glob_cache = "globalCache";
    private static final String shared_pref_glob_cookie = "globalCookies";
    private static final String shared_pref_glob_2fmultitouch = "global2FingerMultiTouch";
    private static final String shared_pref_glob_3fmultitouch = "global3FingerMultiTouch";
    private static final String shared_pref_glob_ui_theme = "globalUITheme";


    private static final DataManager instance = new DataManager();
    private ArrayList<WebApp> websites;
    private int max_assigned_ID;
    private SharedPreferences appdata;
    private Context context = null;
    private GlobalSettings settings;

    private DataManager()
    {
        websites = new ArrayList<>();
        max_assigned_ID = -1;
        settings = new GlobalSettings();
    }

    public static DataManager getInstance(){
//        assert(context != null);
        return instance;
    }

    public GlobalSettings getSettings() {
        return settings;
    }

    public void setSettings(GlobalSettings settings) {
        this.settings = settings;
        saveGlobalSettings();
    }

    public void initContext(Context context) {
        this.context = context;
    }

    public void saveWebAppData() {
        Utility.Assert(context != null, "Context null before saving sharedpref");

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_data, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        Gson gson = new Gson();
        String json = gson.toJson(websites);
        editor.putString(shared_pref_data, json);
        editor.putInt(shared_pref_max_id, max_assigned_ID);
        editor.commit();
    }

    public void loadAppData() {
        Utility.Assert(context != null, "Context null before loading sharedpref");

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_data, Context.MODE_PRIVATE);
        //Webapp data
        if (appdata.contains(shared_pref_data)) {
            Gson gson = new Gson();
            String json = appdata.getString(shared_pref_data, "");
            websites = gson.fromJson(json, new TypeToken<ArrayList<WebApp>>() {}.getType());
        }

        max_assigned_ID = appdata.getInt(shared_pref_max_id, max_assigned_ID);

        //Global app data
        settings.setClearCache(appdata.getBoolean(shared_pref_glob_cache, false));
        settings.setClearCookies(appdata.getBoolean(shared_pref_glob_cookie, false));
        settings.setTwoFingerMultitouch(appdata.getBoolean(shared_pref_glob_2fmultitouch, true));
        settings.setThreeFingerMultitouch(appdata.getBoolean(shared_pref_glob_3fmultitouch, false));
        settings.setThemeId(appdata.getInt(shared_pref_glob_ui_theme, 0));

        Utility.applyUITheme();
    }

    private void saveGlobalSettings() {
        Utility.Assert(context != null, "Context null before saving appdata to sharedpref");

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_data, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        editor.putBoolean(shared_pref_glob_cache, settings.isClearCache());
        editor.putBoolean(shared_pref_glob_cookie, settings.isClearCookies());
        editor.putBoolean(shared_pref_glob_2fmultitouch, settings.isTwoFingerMultitouch());
        editor.putBoolean(shared_pref_glob_3fmultitouch, settings.isThreeFingerMultitouch());
        editor.putInt(shared_pref_glob_ui_theme, settings.getThemeId());
        editor.commit();
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

    public WebApp getSuccessor(int i) {
        if (i != (websites.size() - 1)) {
            return websites.get(i + 1);
        }
        else
            return websites.get(0);

    }
    public WebApp getPredecessor(int i) {
        if (i != 0) {
            return websites.get(i - 1);
        }
        else
            return websites.get(websites.size() - 1);

    }
}

