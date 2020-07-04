package com.cylonid.nativealpha;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    Switch uiCache;
    Switch uiCookies;
    Switch uiMultiTouchTwoFingers;
    Switch uiMultiTouchThreeFingers;
    Spinner uiDropDownTheme;
    int theme_id = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.global_settings);
        Utility.personalizeToolbar(this);
        initSettingsFromDB();
        Button btnSave = (Button) findViewById(R.id.btnSave);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);

        LinearLayout layoutTheme = findViewById(R.id.layoutUIModeSelection);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            layoutTheme.setVisibility(View.VISIBLE);

            //uiDropDownTheme.setSelection(0, false); //Weird hack needed in order to prevent listener firing upon creation
            uiDropDownTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0:
                            theme_id = 0;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                        case 1:
                            theme_id = 1;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case 2:
                            theme_id = 2;
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().setSettings(new GlobalSettings(uiCache.isChecked(), uiCookies.isChecked(), uiMultiTouchTwoFingers.isChecked(), uiMultiTouchThreeFingers.isChecked(), theme_id));
                onBackPressed();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.applyUITheme();
                onBackPressed();
            }
        });
    }


    private void initSettingsFromDB() {
        uiCache = findViewById(R.id.switchGlobalCache);
        uiCookies = findViewById(R.id.switchGlobalCookies);
        uiMultiTouchTwoFingers = findViewById(R.id.switchGlobalMultiTouch);
        uiMultiTouchThreeFingers = findViewById(R.id.switchGlobalMultiTouch3Fingers);
        uiDropDownTheme = findViewById(R.id.dropDownTheme);

        GlobalSettings s = DataManager.getInstance().getSettings();
        uiCache.setChecked(s.isClearCache());
        uiCookies.setChecked(s.isClearCookies());
        uiMultiTouchTwoFingers.setChecked(s.isTwoFingerMultitouch());
        uiMultiTouchThreeFingers.setChecked(s.isThreeFingerMultitouch());
        theme_id = s.getThemeId();
        switch (s.getThemeId()) {
            case 0:
                uiDropDownTheme.setSelection(0, false);
                break;
            case 1:
                uiDropDownTheme.setSelection(1, false);
                break;
            case 2:
                uiDropDownTheme.setSelection(2, false);
                break;
        }


    }
}
