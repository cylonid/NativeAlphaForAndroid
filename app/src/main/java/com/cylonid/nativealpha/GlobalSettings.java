package com.cylonid.nativealpha;

public class GlobalSettings {

    private boolean clear_cache;
    private boolean clear_cookies;
    private boolean two_finger_multitouch;
    private boolean three_finger_multitouch;


    public GlobalSettings(boolean clear_cache, boolean clear_cookies, boolean two_finger_multitouch, boolean three_finger_multitouch) {
        this.clear_cache = clear_cache;
        this.clear_cookies = clear_cookies;
        this.two_finger_multitouch = two_finger_multitouch;
        this.three_finger_multitouch = three_finger_multitouch;
    }

    public GlobalSettings() {
        clear_cache = false;
        clear_cookies = false;
        two_finger_multitouch = true;
        three_finger_multitouch = false;
    }

    public boolean isTwoFingerMultitouch() {
        return two_finger_multitouch;
    }

    public void setTwoFingerMultitouch(boolean twoFingerMultitouch) {
        this.two_finger_multitouch = two_finger_multitouch;
    }

    public boolean isThreeFingerMultitouch() {
        return three_finger_multitouch;
    }

    public void setThreeFingerMultitouch(boolean threeFingerMultitouch) {
        this.three_finger_multitouch = three_finger_multitouch;
    }

    public boolean isClearCache() {
        return clear_cache;
    }

    public void setClearCache(boolean clearCache) {
        this.clear_cache = clear_cache;
    }

    public boolean isClearCookies() {
        return clear_cookies;
    }

    public void setClearCookies(boolean clear_cookies) {
        this.clear_cookies = clear_cookies;
    }

}
