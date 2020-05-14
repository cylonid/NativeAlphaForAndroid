package com.cylonid.nativefier;

import java.util.ArrayList;

public class WebsiteDataManager {
    private
        ArrayList<WebsiteData> websites;
        int nextID = -1;

    public
        void addWebsite(WebsiteData new_site) {
            websites.add(new_site);
    }
        int getIncrementedID() {
            nextID++;
            return nextID;
        }
}

