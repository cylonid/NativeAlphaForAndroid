package com.cylonid.nativefier;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class WebsiteDataManager {


    private ArrayList<WebsiteData> websites;
    private int nextID = -1;


    private static final WebsiteDataManager instance = new WebsiteDataManager();

    private WebsiteDataManager()
    {
        websites = new ArrayList<>();
    }
    public static WebsiteDataManager getInstance(){
        return instance;
    }


    public void addWebsite(WebsiteData new_site) {
            websites.add(new_site);
    }

    public int getIncrementedID() {
        nextID++;
        return nextID;
    }
    public ArrayList<WebsiteData> getWebsites() {
        return websites;
    }
}

