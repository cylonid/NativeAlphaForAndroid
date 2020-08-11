package com.cylonid.nativealpha.util;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.R;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.WebViewActivity;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class Utility {

    public static Intent createWebViewIntent(WebApp d, Context c) {
        Intent intent = new Intent(c, WebViewActivity.class);
        intent.putExtra(Const.INTENT_WEBAPPID, d.getID());
        intent.setData(Uri.parse(d.getBaseUrl()));
        intent.setAction(Intent.ACTION_VIEW);

        return intent;
    }

    public static Long getTimeInSeconds()
    {
        return System.currentTimeMillis() / 1000;
    }

    public static void Assert(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void personalizeToolbar(AppCompatActivity a)  {
        Toolbar toolbar = a.findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.native_alpha_white);
        toolbar.setTitle(R.string.app_name);
        a.setSupportActionBar(toolbar);
    }

    public static Integer getWidthFromIcon(String size_string) {
        int x_index = size_string.indexOf("x");
        if (x_index == -1)
            x_index = size_string.indexOf("×");

        if (x_index == -1)
            return 1;
        String width = size_string.substring(0, x_index);

        return Integer.parseInt(width);
    }


    public static void applyUITheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int id = DataManager.getInstance().getSettings().getThemeId();
            switch (id) {
                case 0:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
                case 1:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case 2:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
            }
        }
    }

    public static void writeFileOnInternalStorage(Context mcoContext, String sFileName, String sBody){

        try {
            File gpxfile = new File(mcoContext.getExternalFilesDir(null), sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void showInfoSnackbar(Activity activity, String msg, int duration) {

        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), msg, duration);

        snackbar.setAction(App.getAppContext().getString(android.R.string.ok), (View.OnClickListener) v -> snackbar.dismiss());

        View snackBarView = snackbar.getView();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(0, 30, 0, 20);


        snackBarView.setLayoutParams(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            snackBarView.setForceDarkAllowed(false);

        TextView tv = (TextView) snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setMaxLines(10);
        snackbar.setBackgroundTint(ResourcesCompat.getColor(App.getAppContext().getResources(), R.color.snackbar_background, null));
        snackbar.setTextColor(Color.BLACK);
        snackbar.show();

    }

    public static String readFromFile(File file) {

        String ret = "";

        try {
            InputStream inputStream = new FileInputStream(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException  e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    @ColorInt
    public static int getThemeColor
            (
                    @NonNull final Context context,
                    @AttrRes final int attributeColor
            )
    {
        final TypedValue value = new TypedValue();
        context.getTheme ().resolveAttribute (attributeColor, value, true);
        return value.data;
    }

}
