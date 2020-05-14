package com.cylonid.nativefier;

public class WebsiteData {
    private
      String name;
      String url;
      int ID;
      boolean open_url_internal;


    public WebsiteData(String name, String url, int ID, boolean open_url_internal) {
        this.name = name;
        this.url = url;
        this.ID = ID;
        this.open_url_internal = open_url_internal;
    }

    public WebsiteData(String name, String url, int ID) {
        this.name = name;
        this.url = url;
        this.ID = ID;
        this.open_url_internal = true;
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

    public boolean openUrlInternal() {
        return open_url_internal;
    }
}
