package com.cylonid.nativefier;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;


public class WebsiteDataManager {


    private ArrayList<WebsiteData> websites;
    private int nextID = -1;
    private SharedPreferences appdata;
    private static final String shared_pref_id = "WEBSITEDATA";
    private Context context = null;


    private static final WebsiteDataManager instance = new WebsiteDataManager();

    private WebsiteDataManager()
    {
        websites = new ArrayList<>();
    }
    public void initContext(Context context) {
        this.context = context;
    }

    private void saveAppData() {
        assert(context != null);

        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_id, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appdata.edit();
        Gson gson = new Gson();
        String json = gson.toJson(websites);
        editor.putString(shared_pref_id, json);
        editor.commit();
    }

    private void loadAppData() {
        assert(context != null);
        appdata = context.getApplicationContext().getSharedPreferences(shared_pref_id, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = appdata.getString(shared_pref_id, "");
        websites = gson.fromJson(json, new TypeToken<ArrayList<WebsiteData>>(){}.getType());

    }

    public void initDummyData()
    {
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

    public int getIncrementedID() {
        nextID++;
        return nextID;
    }
    public ArrayList<WebsiteData> getWebsites() {
        loadAppData();
        return websites;
    }
}

