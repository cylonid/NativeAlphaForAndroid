package com.cylonid.nativealpha.model;


import android.provider.ContactsContract;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.cylonid.nativealpha.R;

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
    private boolean use_adblock;
    private boolean send_savedata_request;
    private boolean block_images;
    private boolean allow_http;
    private String url_on_first_pageload;
    private boolean allow_location_access;

    public WebApp(String url, int id) {
        title = url.replace("http://", "").replace("https://", "").replace("www.", "");
        base_url = url.toLowerCase();
        ID = id;
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
        use_adblock = false;
        send_savedata_request = false;
        block_images = false;
        allow_http = false;
        url_on_first_pageload = null;
        allow_location_access = false;

    }

    public WebApp(WebApp other) {
        this.title = other.title;
        this.base_url = other.base_url;
        this.last_used_url = other.last_used_url;
        this.timestamp_last_used_url = other.timestamp_last_used_url;
        this.timeout_last_used_url = other.timeout_last_used_url;
        this.ID = other.ID;
        this.open_url_external = other.open_url_external;
        this.allow_cookies = other.allow_cookies;
        this.allow_third_p_cookies = other.allow_third_p_cookies;
        this.restore_page = other.restore_page;
        this.allow_js = other.allow_js;
        this.active_entry = other.active_entry;
        this.request_desktop = other.request_desktop;
        this.clear_cache = other.clear_cache;
        this.use_adblock = other.use_adblock;
        this.send_savedata_request = other.send_savedata_request;
        this.block_images = other.block_images;
        this.allow_http = other.allow_http;
        this.url_on_first_pageload = other.url_on_first_pageload;
        this.allow_location_access = other.allow_location_access;

    }

    public boolean isAllowHttp() {
        return allow_http;
    }

    public void setAllowHttp(boolean allow_http) {
        this.allow_http = allow_http;
    }

    public boolean isSendSavedataRequest() {
        return send_savedata_request;
    }

    public void setSendSavedataRequest(boolean send_savedata_request) {
        this.send_savedata_request = send_savedata_request;
    }

    public boolean isBlockImages() {
        return block_images;
    }

    public void setBlockImages(boolean block_images) {
        this.block_images = block_images;
    }

    public void markInactive() {
        active_entry = false;
        DataManager.getInstance().saveWebAppData();
    }

    public void allowHTTP() {
        allow_http = true;
        DataManager.getInstance().saveWebAppData();
    }

    public void saveCurrentUrl(String url) {
        if (restore_page) {
            last_used_url = url;
            timestamp_last_used_url = System.currentTimeMillis() / 1000;
            DataManager.getInstance().saveWebAppData();
        }
    }

    public String getSingleLineTitle() {
        if (title.length() > 24) {
            String single_line = title.substring(0, 25);
            single_line += " ...";
            return single_line;
        }

        return title;
    }

    public String getLoadableUrl() {

        if (last_used_url == null)
            return base_url;
        else if (restore_page) {

            long current_time_sec = System.currentTimeMillis() / 1000;
            long diff = Math.abs(timestamp_last_used_url - current_time_sec);
            if (diff <= timeout_last_used_url * 60)
                return last_used_url;
        }

        return base_url;
    }

    public int getTimeoutLastUsedUrl() {
        return timeout_last_used_url;
    }

    public void setTimeoutLastUsedUrl(int timeout_last_used_url) {
        this.timeout_last_used_url = timeout_last_used_url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBaseUrl() {
        return base_url;
    }

    public void setBaseUrl(String base_url) {
        this.base_url = base_url;
        DataManager.getInstance().saveWebAppData();
    }

    public int getID() {
        return ID;
    }

    public boolean isOpenUrlExternal() {
        return open_url_external;
    }

    public void setOpenUrlExternal(boolean open_url_external) {
        this.open_url_external = open_url_external;
    }

    public boolean isActiveEntry() { return active_entry; }

    public void setActiveEntry(boolean active_entry) {
        this.active_entry = active_entry;
    }

    public boolean isAllowCookies() {
        return allow_cookies;
    }

    public void setAllowCookies(boolean allow_cookies) {
        this.allow_cookies = allow_cookies;
    }

    public boolean isAllowThirdPartyCookies() {
        return allow_third_p_cookies;
    }

    public void setAllowThirdPartyCookies(boolean allow_third_p_cookies) {
        this.allow_third_p_cookies = allow_third_p_cookies;
    }

    public boolean isRestorePage() {
        return restore_page;
    }

    public void setRestorePage(boolean restore_page) {
        this.restore_page = restore_page;
    }

    public boolean isAllowJs() {
        return allow_js;
    }

    public void setAllowJs(boolean allow_js) {
        this.allow_js = allow_js;
    }

    public boolean isRequestDesktop() {
        return request_desktop;
    }

    public void setRequestDesktop(boolean request_desktop) {
        this.request_desktop = request_desktop;
    }

    public boolean isClearCache() {
        return clear_cache;
    }

    public void setClearCache(boolean clear_cache) {
        this.clear_cache = clear_cache;
    }

    public boolean isUseAdblock() {
        return use_adblock;
    }

    public void setUseAdblock(boolean use_adblock) {
        this.use_adblock = use_adblock;
    }

    public boolean isAllowLocationAccess() {
        return allow_location_access;
    }

    public void setAllowLocationAccess(boolean allow_location_access) {
        this.allow_location_access = allow_location_access;
    }

    public void enableLocationAccess() {
        this.allow_location_access = true;
        DataManager.getInstance().saveWebAppData();
    }

    public void setLastUsedUrl(String last_used_url) {
        this.last_used_url = last_used_url;
    }

    public void setTimestampLastUsedUrl(Long timestamp_last_used_url) {
        this.timestamp_last_used_url = timestamp_last_used_url;
    }

    public String getNonNullUrlOnFirstPageload() {
        if (url_on_first_pageload != null)
            return url_on_first_pageload;

        return base_url;
    }

    public String getUrlOnFirstPageload() {
            return url_on_first_pageload;
    }


    public void saveUrlOnFirstPageLoad(String url) {
        url_on_first_pageload = url;
        DataManager.getInstance().saveWebAppData();
    }


    public void onSwitchCookiesChanged(CompoundButton mSwitch, boolean isChecked) {
        Switch third_party_cookies = mSwitch.getRootView().findViewById(R.id.switch3PCookies);
        if (isChecked)
            third_party_cookies.setEnabled(true);
        else {
            third_party_cookies.setEnabled(false);
            third_party_cookies.setChecked(false);
        }
    }
    public void onSwitchJsChanged(CompoundButton mSwitch, boolean isChecked) {
        Switch switchDesktopVersion = mSwitch.getRootView().findViewById(R.id.switchDesktopSite);
        Switch switchAdblock = mSwitch.getRootView().findViewById(R.id.switchAdblock);
        if (isChecked) {
            switchDesktopVersion.setEnabled(true);
            switchAdblock.setEnabled(true);
        } else {
            switchDesktopVersion.setChecked(false);
            switchDesktopVersion.setEnabled(false);
            switchAdblock.setChecked(false);
            switchAdblock.setEnabled(false);
        }
    }

    public void onSwitchRestorePageChanged(CompoundButton mSwitch, boolean isChecked) {
        EditText textTimeout = mSwitch.getRootView().findViewById(R.id.textTimeout);
        if (isChecked) {
            textTimeout.setEnabled(true);
        }
        else
            textTimeout.setEnabled(false);
     }
}
