package com.cylonid.nativealpha.model;

public class GlobalSettings {

    private boolean clear_cache;
    private boolean clear_cookies;
    private boolean two_finger_multitouch;
    private boolean three_finger_multitouch;
    private boolean show_progressbar;
    private boolean multitouch_reload;
    private int theme_id;
    private WebApp global_web_app;

    public GlobalSettings(GlobalSettings other) {
        this.clear_cache = other.clear_cache;
        this.clear_cookies = other.clear_cookies;
        this.two_finger_multitouch = other.two_finger_multitouch;
        this.three_finger_multitouch = other.three_finger_multitouch;
        this.theme_id = other.theme_id;
        this.multitouch_reload = other.multitouch_reload;
        this.show_progressbar = other.show_progressbar;
        this.global_web_app = other.global_web_app;
    }

    public GlobalSettings() {
        clear_cache = false;
        clear_cookies = false;
        two_finger_multitouch = true;
        three_finger_multitouch = false;
        multitouch_reload = true;
        theme_id = 0;
        show_progressbar = false;
        global_web_app = new WebApp("about:blank", Integer.MAX_VALUE);
    }

    public boolean isTwoFingerMultitouch() {
        return two_finger_multitouch;
    }

    public void setTwoFingerMultitouch(boolean twoFingerMultitouch) {
        this.two_finger_multitouch = twoFingerMultitouch;
    }

    public boolean isThreeFingerMultitouch() {
        return three_finger_multitouch;
    }

    public void setThreeFingerMultitouch(boolean threeFingerMultitouch) {
        this.three_finger_multitouch = threeFingerMultitouch;
    }

    public boolean isClearCache() {
        return clear_cache;
    }

    public void setClearCache(boolean clearCache) {
        this.clear_cache = clearCache;
    }

    public boolean isClearCookies() {
        return clear_cookies;
    }

    public void setClearCookies(boolean clear_cookies) {
        this.clear_cookies = clear_cookies;
    }
    public int getThemeId() {
        return theme_id;
    }
    public void setThemeId(int theme_id) {
        this.theme_id = theme_id;
    }

    public boolean isMultitouchReload() {
        return multitouch_reload;
    }

    public void setMultitouchReload(boolean multitouch_reload) {
        this.multitouch_reload = multitouch_reload;
    }
    public boolean isShowProgressbar() {
        return show_progressbar;
    }

    public void setShowProgressbar(boolean show_progressbar) {
        this.show_progressbar = show_progressbar;
    }

    public WebApp getGlobalWebApp() {
        return global_web_app;
    }

    public void setGlobalWebApp(WebApp globalWebApp) {
        this.global_web_app = globalWebApp;
    }




}
