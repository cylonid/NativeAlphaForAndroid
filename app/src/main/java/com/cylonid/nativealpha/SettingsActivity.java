package com.cylonid.nativealpha;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.databinding.DataBindingUtil;

import com.cylonid.nativealpha.databinding.GlobalSettingsBinding;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.GlobalSettings;
import com.cylonid.nativealpha.util.Utility;

public class SettingsActivity extends AppCompatActivity {

//    Intent intent = new Intent()
//            .setType("*/*")
//            .setAction(Intent.ACTION_GET_CONTENT);
//
//    startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == 123 && resultCode == RESULT_OK) {
//            Uri selectedfile = data.getData(); //The uri with the location of the file
//        }
//    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.global_settings);
        GlobalSettings settings = DataManager.getInstance().getSettings();
        final GlobalSettings modified_settings = new GlobalSettings(settings);
        binding.setSettings(modified_settings);

        handleThemeSelection(modified_settings.getThemeId(), modified_settings);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnExport = findViewById(R.id.btnExportSettings);
        Button btnImport = findViewById(R.id.btnImportSettings);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().saveSharedPreferencesToFile(SettingsActivity.this);

            }

        });

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().loadSharedPreferencesFromFile(SettingsActivity.this);
                Intent i = new Intent(SettingsActivity.this, MainActivity.class);
                finish();
                startActivity(i);

            }

        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().setSettings(modified_settings);
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


    private void handleThemeSelection(int current_theme, final GlobalSettings modified_settings) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Spinner uiDropDownTheme = findViewById(R.id.dropDownTheme);

        switch (current_theme) {
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

        LinearLayout layoutTheme = findViewById(R.id.layoutUIModeSelection);
        layoutTheme.setVisibility(View.VISIBLE);
        //uiDropDownTheme.setSelection(0, false); //Weird hack needed in order to prevent listener firing upon creation
        uiDropDownTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        modified_settings.setThemeId(0);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                    case 1:
                        modified_settings.setThemeId(1);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case 2:
                        modified_settings.setThemeId(2);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    }
}
