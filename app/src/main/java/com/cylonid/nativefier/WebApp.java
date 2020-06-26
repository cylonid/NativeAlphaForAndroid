package com.cylonid.nativefier;

public class WebApp {
    private String name;

    public void setBase_url(String base_url) {
        this.base_url = base_url;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public void markInactive() {
        active_entry = false;
        base_url = "";
        last_used_url = "";
        timestamp_last_used_url = (long)0;

    }

    public void saveCurrentUrl(String url) {
        last_used_url = url;
        timestamp_last_used_url = System.currentTimeMillis() / 1000;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public String getLoadableUrl() {

        if (last_used_url == null)
            return base_url;

        WebsiteDataManager.getInstance().loadAppData();
        Long current_time_sec = System.currentTimeMillis() / 1000;
        Long diff = Math.abs(timestamp_last_used_url - current_time_sec);
        if (diff <= timeout_last_used_url * 60)
            return last_used_url;

        return base_url;
    }

    private String base_url;
    private String last_used_url;
    private Long timestamp_last_used_url;
    private int timeout_last_used_url;
    private int ID;
    private boolean open_url_external;
    private boolean active_entry;

    public WebApp(String name, String base_url, boolean open_url_internal) {
        this.name = name;
        this.base_url = base_url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = open_url_internal;
        active_entry = true;
        timeout_last_used_url = 30;
        last_used_url = null;
    }

    public WebApp(String name, String base_url) {
        this.name = name;
        this.base_url = base_url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = false;
        active_entry = true;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return base_url;
    }

    public int getID() {
        return ID;
    }

    public boolean openUrlExternal() {
        return open_url_external;
    }

    public boolean isActive() { return active_entry; }
}
