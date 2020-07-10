package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application {

    @SuppressLint("StaticFieldLeak") //We are using app context which is never deleted during runtime, so this is not a leak per se.
    //https://stackoverflow.com/questions/2002288/static-way-to-get-context-in-android
    private static Context context;

    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}