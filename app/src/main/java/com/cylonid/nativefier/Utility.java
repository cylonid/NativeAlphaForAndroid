package com.cylonid.nativefier;

import android.content.Context;
import android.content.Intent;

public final class Utility {
    public static final String INT_ID_URL = "url";
    public static final String INT_ID_EXTERNAL = "open_external";
    public static Intent createWebViewIntent(WebsiteData d, Context c) {
        Intent intent = new Intent(c, WebViewActivity.class);
        intent.putExtra(Utility.INT_ID_URL, d.getUrl());
        intent.putExtra(Utility.INT_ID_EXTERNAL, d.openUrlExternal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        return intent;
    }
}
