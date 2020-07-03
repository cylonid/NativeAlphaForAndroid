package com.cylonid.nativealpha;

public class WebApp {
    private String title;
    private String base_url;
    private String last_used_url;
    private Long timestamp_last_used_url;
    private int timeout_last_used_url;
    private int ID;
    private boolean open_url_external;
    private boolean allow_cookies;
    private boolean allow_third_p_cookies;
    private boolean restore_page;
    private boolean allow_js;
    private boolean active_entry;
    private boolean request_desktop;
    private boolean clear_cache;



    public WebApp(String base_url) {
        title = base_url.replace("http://", "").replace("https://", "").replace("www.", "");
        this.base_url = base_url;
        ID = WebsiteDataManager.getInstance().getIncrementedID();
        open_url_external = true;
        active_entry = true;
        timeout_last_used_url = 10;
        last_used_url = null;
        restore_page = true;
        allow_cookies = true;
        allow_third_p_cookies = false;
        allow_js = true;
        request_desktop = false;
        clear_cache = false;

    }
    public void markInactive() {
        active_entry = false;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public void saveCurrentUrl(String url) {
        if (restore_page) {
            last_used_url = url;
            timestamp_last_used_url = System.currentTimeMillis() / 1000;
            WebsiteDataManager.getInstance().saveAppData();
        }
    }

    public String getLoadableUrl() {

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

    public void saveNewSettings(boolean open_url_external, boolean request_desktop, boolean allow_cookies, boolean allow_third_p_cookies, boolean allow_js, boolean clear_cache, boolean restore_page, Integer timeout) {
        this.open_url_external = open_url_external;
        this.allow_cookies = allow_cookies;
        this.allow_third_p_cookies = allow_third_p_cookies;
        this.allow_js = allow_js;
        this.restore_page = restore_page;
        this.timeout_last_used_url = timeout;
        this.request_desktop = request_desktop;
        this.clear_cache = clear_cache;
        WebsiteDataManager.getInstance().saveAppData();
    }

    public int getTimeout_last_used_url() {
        return timeout_last_used_url;
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
    public boolean isAllowThirdPartyCookiesSet() {
        return allow_third_p_cookies;
    }

    public boolean isRestorePageSet() {
        return restore_page;
    }

    public boolean isAllowJSSet() {
        return allow_js;
    }

    public boolean isRequestDesktopSet() {
        return request_desktop;
    }
    public boolean isClearCacheSet() {
        return clear_cache;
    }


}
