package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cylonid.nativealpha.databinding.WebappSettingsBinding;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;

public class WebAppSettingsActivity extends AppCompatActivity {

    int webappID = -1;
    private ShortcutHelper shortcutHelper = null;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebappSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.webapp_settings);

        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");

        final View inflated_view = binding.getRoot();
        final WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        if (webapp == null) {
            finish();
        }
        else {
            final WebApp modified_webapp = new WebApp(webapp);
            binding.setWebapp(modified_webapp);

            final Button btnCreateShortcut = inflated_view.findViewById(R.id.btnRecreateShortcut);

            btnCreateShortcut.setOnClickListener(view -> {
                shortcutHelper = new ShortcutHelper(webapp, WebAppSettingsActivity.this, 1);
            });
            Button btnSave = findViewById(R.id.btnSave);
            Button btnCancel = findViewById(R.id.btnCancel);

            btnSave.setOnClickListener(v -> {
                ActivityManager activityManager =
                        (ActivityManager) App.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);

                for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
                    int id = task.getTaskInfo().baseIntent.getIntExtra(Const.INTENT_WEBAPPID, -1);
                    if (id == webappID)
                        task.finishAndRemoveTask();

                }
                DataManager.getInstance().replaceWebApp(modified_webapp);
                Intent i = new Intent(WebAppSettingsActivity.this, MainActivity.class);
                i.putExtra(Const.INTENT_WEBAPP_CHANGED, true);
                finish();
                startActivity(i);
            });

            btnCancel.setOnClickListener(v -> finish());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shortcutHelper != null) {
            if (shortcutHelper.getAsyncTask() != null)
                shortcutHelper.getAsyncTask().cancel(true);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (shortcutHelper != null) {
            shortcutHelper.onActivityResult(requestCode, resultCode, data);
        }

    }
}


