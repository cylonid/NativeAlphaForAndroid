package com.cylonid.nativefier;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;


public class WebsiteDataManager {


    private ArrayList<WebsiteData> websites;
    private int max_assigned_ID;
    private SharedPreferences appdata;
    private static final String shared_pref_data = "WEBSITEDATA";
    private static final String shared_pref_max_id  = "MAX_ID";
    private Context context = null;


    private static final WebsiteDataManager instance = new WebsiteDataManager();

    private WebsiteDataManager()
    {
        websites = new ArrayList<>();
         max_assigned_ID = -1;
    }
    public void initContext(Context context) {
        this.context = context;
    }

    private void saveAppData() {
        assert(context != null);

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_data, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        Gson gson = new Gson();
        String json = gson.toJson(websites);
        editor.putString(shared_pref_data, json);
        editor.putInt(shared_pref_max_id, max_assigned_ID);
        editor.commit();
    }

    private void loadAppData() {
        assert(context != null);

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_data, Context.MODE_PRIVATE);
        if (appdata.contains(shared_pref_data)) {
            Gson gson = new Gson();
            String json = appdata.getString(shared_pref_data, "");
            websites = gson.fromJson(json, new TypeToken<ArrayList<WebsiteData>>() {}.getType());
        }
        if (appdata.contains(shared_pref_max_id))
            max_assigned_ID = appdata.getInt(shared_pref_max_id, max_assigned_ID);
    }

    public void initDummyData()
    {
        loadAppData();
        WebsiteData d1 = new WebsiteData("ORF.at", "orf.at");
        WebsiteData d2 = new WebsiteData("Die Presse", "diepresse.com");
        WebsiteData d3 = new WebsiteData("ÖBB", "oebb.at");

        addWebsite(d1);
        addWebsite(d2);
        addWebsite(d3);

    }
    public static WebsiteDataManager getInstance(){
//        assert(context != null);
        return instance;
    }


    public void addWebsite(WebsiteData new_site) {
            websites.add(new_site);
            saveAppData();
    }

    public void removeWebsite(int ID) {
        for (WebsiteData d : websites) {
            if (d.getID() == ID) {
                websites.remove(d);
                break;
            }
        }
        saveAppData();
    }

    public int getIncrementedID() {
        max_assigned_ID++;
        return max_assigned_ID;
    }
    public ArrayList<WebsiteData> getWebsites() {
        loadAppData();
        return websites;
    }
}

