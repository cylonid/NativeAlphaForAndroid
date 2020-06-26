package com.cylonid.nativefier;

import android.content.Context;
import android.content.Intent;

public final class Utility {
    public static final String INT_ID_WEBAPPID = "webappID";
    public static Intent createWebViewIntent(WebApp d, Context c) {
        Intent intent = new Intent(c, WebViewActivity.class);
        intent.putExtra(Utility.INT_ID_WEBAPPID, d.getID());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        return intent;
    }

    public static Long getTimeInSeconds()
    {
        return System.currentTimeMillis() / 1000;
    }

}
