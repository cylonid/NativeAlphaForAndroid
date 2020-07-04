package com.cylonid.nativealpha;

import androidx.appcompat.app.AppCompatDelegate;

public class GlobalSettings {

    private boolean clear_cache;
    private boolean clear_cookies;
    private boolean two_finger_multitouch;
    private boolean three_finger_multitouch;
    private int theme_id;

    public GlobalSettings(boolean clear_cache, boolean clear_cookies, boolean two_finger_multitouch, boolean three_finger_multitouch, int theme_id) {
        this.clear_cache = clear_cache;
        this.clear_cookies = clear_cookies;
        this.two_finger_multitouch = two_finger_multitouch;
        this.three_finger_multitouch = three_finger_multitouch;
        this.theme_id = theme_id;
    }

    public GlobalSettings() {
        clear_cache = false;
        clear_cookies = false;
        two_finger_multitouch = true;
        three_finger_multitouch = false;
        theme_id = 0;
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



}
