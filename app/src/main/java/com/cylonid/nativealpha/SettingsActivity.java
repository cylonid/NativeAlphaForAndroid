package com.cylonid.nativealpha;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.cylonid.nativealpha.util.Const.CODE_OPEN_FILE;
import static com.cylonid.nativealpha.util.Const.CODE_WRITE_FILE;


public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CODE_WRITE_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            DataManager.getInstance().saveGlobalSettings(); //Needed to write legacy settings to new XML

            if (!DataManager.getInstance().saveSharedPreferencesToFile(uri)) {
                Utility.showInfoSnackbar(this, getString(R.string.export_failed), Snackbar.LENGTH_LONG);
            } else {
                Utility.showInfoSnackbar(this, getString(R.string.export_success), Snackbar.LENGTH_SHORT);
            }
        }
        if (requestCode == CODE_OPEN_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            if (!DataManager.getInstance().loadSharedPreferencesFromFile(uri)) {
                Utility.showInfoSnackbar(this, getString(R.string.import_failed), Snackbar.LENGTH_LONG);
            } else {
                Intent i = new Intent(SettingsActivity.this, MainActivity.class);
                DataManager.getInstance().loadAppData();
                i.putExtra(Const.INTENT_BACKUP_RESTORED, true);
                finish();
                startActivity(i);
            }
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.global_settings);
        GlobalSettings settings = DataManager.getInstance().getSettings();
        final GlobalSettings modified_settings = new GlobalSettings(settings);
        binding.setSettings(modified_settings);

        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnExport = findViewById(R.id.btnExportSettings);
        Button btnImport = findViewById(R.id.btnImportSettings);
        Button btnGlobalWebApp = findViewById(R.id.btnGlobalWebApp);

        btnGlobalWebApp.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, WebAppSettingsActivity.class);
            intent.putExtra(Const.INTENT_WEBAPPID, settings.getGlobalWebApp().getID());
            intent.setAction(Intent.ACTION_VIEW);
            startActivity(intent);
        });


        btnExport.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());
            intent.putExtra(Intent.EXTRA_TITLE, "NativeAlpha_" + currentDateTime);
            try {
                startActivityForResult(intent, CODE_WRITE_FILE);
            } catch (android.content.ActivityNotFoundException e) {
                Utility.showInfoSnackbar(SettingsActivity.this, getString(R.string.no_filemanager), Snackbar.LENGTH_LONG);
                e.printStackTrace();
            }

        });

        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select a file"), CODE_OPEN_FILE);
            } catch (android.content.ActivityNotFoundException e) {
                Utility.showInfoSnackbar(SettingsActivity.this, getString(R.string.no_filemanager), Snackbar.LENGTH_LONG);
                e.printStackTrace();
            }

        });

        btnSave.setOnClickListener(v -> {
            DataManager.getInstance().setSettings(modified_settings);
            onBackPressed();
        });

        btnCancel.setOnClickListener(v -> {
            onBackPressed();
        });
    }
}
