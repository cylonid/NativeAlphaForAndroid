package com.cylonid.nativefier;

public class WebsiteData {
    private String name;
    private String url;
    private int ID;
    private boolean open_url_external;

    public WebsiteData(String name, String url, boolean open_url_internal) {
        this.name = name;
        this.url = url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = open_url_internal;
    }

    public WebsiteData(String name, String url) {
        this.name = name;
        this.url = url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = false;
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
