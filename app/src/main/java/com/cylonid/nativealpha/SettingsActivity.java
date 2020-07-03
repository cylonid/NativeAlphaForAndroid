package com.cylonid.nativealpha;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    Switch uiCache;
    Switch uiCookies;
    Switch uiMultiTouchTwoFingers;
    Switch uiMultiTouchThreeFingers;
    View inflated_view;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.global_settings);
        Utility.personalizeToolbar(this);
        initSettingsFromDB();
        Button btnSave = (Button) findViewById(R.id.btnSave);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().setSettings(new GlobalSettings(uiCache.isChecked(), uiCookies.isChecked(), uiMultiTouchTwoFingers.isChecked(), uiMultiTouchThreeFingers.isChecked()));
                onBackPressed();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void initSettingsFromDB() {
//        inflated_view = getLayoutInflater().inflate(R.layout.global_settings, null);
        uiCache = findViewById(R.id.switchGlobalCache);
        uiCookies = findViewById(R.id.switchGlobalCookies);
        uiMultiTouchTwoFingers = findViewById(R.id.switchGlobalMultiTouch);
        uiMultiTouchThreeFingers = findViewById(R.id.switchGlobalMultiTouch3Fingers);

        uiCache.setChecked(DataManager.getInstance().getSettings().isClearCache());
        uiCookies.setChecked(DataManager.getInstance().getSettings().isClearCookies());
        uiMultiTouchTwoFingers.setChecked(DataManager.getInstance().getSettings().isTwoFingerMultitouch());
        uiMultiTouchThreeFingers.setChecked(DataManager.getInstance().getSettings().isThreeFingerMultitouch());

    }
}
