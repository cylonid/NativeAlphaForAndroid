package com.cylonid.nativealpha.model;

import android.service.autofill.DateValueSanitizer;

import java.util.HashMap;
import java.util.Map;

public class DataVersionConverter {

    public static String convertToDataFormat(String input, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            input = input.replace(DataVersionConverter.formatAsJsonKey(entry.getKey()), DataVersionConverter.formatAsJsonKey(entry.getValue()));
        }
        return input;
    }

    public static int getDataFormat(String input) {
        if(input.contains(DataVersionConverter.formatAsJsonKey("allow_js"))) return 1000;
        if(input.contains(DataVersionConverter.formatAsJsonKey("isAllowJs"))) return 1300;

        return 0;
    }
    private static String formatAsJsonKey(String key) {
        return "\"" + key + "\":";
    }

    public static Map<String, String> getLegacyTo1300Map() {
        Map<String, String> map = new HashMap<>();
        map.put("base_url","baseUrl");
        map.put("override_global_settings","isOverrideGlobalSettings");
        map.put("open_url_external","isOpenUrlExternal");
        map.put("allow_cookies","isAllowCookies");
        map.put("allow_third_p_cookies","isAllowThirdPartyCookies");
        map.put("restore_page","isRestorePage");
        map.put("allow_js","isAllowJs");
        map.put("active_entry","isActiveEntry");
        map.put("request_desktop","isRequestDesktop");
        map.put("clear_cache","isClearCache");
        map.put("use_adblock","isUseAdblock");
        map.put("send_savedata_request","isSendSavedataRequest");
        map.put("block_images","isBlockImages");
        map.put("allow_http","isAllowHttp");
        map.put("allow_location_access","isAllowLocationAccess");
        map.put("user_agent","userAgent");
        map.put("use_custom_user_agent","isUseCustomUserAgent");
        map.put("autoreload","isAutoreload");
        map.put("time_autoreload","timeAutoreload");
        map.put("force_dark_mode","isForceDarkMode");
        map.put("use_timespan_dark_mode","isUseTimespanDarkMode");
        map.put("timespan_dark_mode_begin","timespanDarkModeBegin");
        map.put("timespan_dark_mode_end","timespanDarkModeEnd");
        map.put("ignore_ssl_errors","isIgnoreSslErrors");
        map.put("show_expert_settings","isShowExpertSettings");
        map.put("safe_browsing","isSafeBrowsing");
        map.put("block_third_party_requests","isBlockThirdPartyRequests");
        map.put("container_id","containerId");
        map.put("use_container","isUseContainer");
        map.put("drm_allowed","isDrmAllowed");
        map.put("show_fullscreen","isShowFullscreen");
        map.put("keep_awake","isKeepAwake");
        map.put("camera_permission","isCameraPermission");
        map.put("microphone_permission","isMicrophonePermission");
        return map;
    }
}
