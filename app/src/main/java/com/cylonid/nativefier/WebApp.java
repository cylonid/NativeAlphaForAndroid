package com.cylonid.nativefier;

public class WebApp {
    private String title;
    private String base_url;
    private String last_used_url;
    private Long timestamp_last_used_url;
    private int timeout_last_used_url;
    private int ID;
    private boolean open_url_external;
    private boolean allow_cookies;
    private boolean restore_page;
    private boolean allow_js;
    private boolean active_entry;

    public WebApp(String title, String base_url, boolean open_url_internal) {
        this.title = title;
        this.base_url = base_url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = open_url_internal;
        active_entry = true;
        timeout_last_used_url = 30;
        last_used_url = null;
        restore_page = true;
        allow_cookies = true;
        allow_js = true;
    }

    public WebApp(String title, String base_url) {
        this.title = title;
        this.base_url = base_url;
        this.ID = WebsiteDataManager.getInstance().getIncrementedID();
        this.open_url_external = false;
        active_entry = true;
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
        WebsiteDataManager.getInstance().loadAppData();
        if (last_used_url == null)
            return base_url;
        else if (restore_page) {

            Long current_time_sec = System.currentTimeMillis() / 1000;
            Long diff = Math.abs(timestamp_last_used_url - current_time_sec);
            if (diff <= timeout_last_used_url * 60)
                return last_used_url;
        }

        return base_url;
    }

    public void saveNewSettings(boolean allow_cookies, boolean allow_js, boolean restore_page) {
        this.allow_cookies = allow_cookies;
        this.allow_js = allow_js;
        this.restore_page = restore_page;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public String getTitle() {
        return title;
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
    public void setBase_url(String base_url) {
        this.base_url = base_url;
        WebsiteDataManager.getInstance().saveAppData();
    }
    public boolean isAllowCookiesSet() {
        return allow_cookies;
    }

    public boolean isRestorePageSet() {
        return restore_page;
    }

    public boolean isAllowJSSet() {
        return allow_js;
    }

}
