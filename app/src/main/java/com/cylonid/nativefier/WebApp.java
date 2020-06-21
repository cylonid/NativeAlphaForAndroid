package com.cylonid.nativefier;

import android.widget.Toast;

public class WebApp {
    private String name;

    public void setUrl(String url) {
        this.url = url;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public void setActiveEntry(boolean active_entry) {
        this.active_entry = active_entry;
        WebsiteDataManager.getInstance().saveAppData();
    }

    private String url;
    private int ID;
    private boolean open_url_external;
    private boolean active_entry;

    public WebApp(String name, String url, boolean open_url_internal) {
        this.name = name;
        this.url = url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = open_url_internal;
        active_entry = true;
    }

    public WebApp(String name, String url) {
        this.name = name;
        this.url = url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = false;
        active_entry = true;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getID() {
        return ID;
    }

    public boolean openUrlExternal() {
        return open_url_external;
    }
}
