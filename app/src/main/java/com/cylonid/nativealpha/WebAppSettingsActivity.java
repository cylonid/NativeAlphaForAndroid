package com.cylonid.nativealpha;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cylonid.nativealpha.databinding.WebappSettingsBinding;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WebAppSettingsActivity extends AppCompatActivity {

    int webappID = -1;
    WebApp webapp;
    boolean isGlobalWebApp;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebappSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.webapp_settings);
        TextView txt = findViewById(R.id.txthintUserAgent);
        txt.setText(Html.fromHtml(getString(R.string.hint_user_agent), Html.FROM_HTML_MODE_LEGACY));
        txt.setMovementMethod(LinkMovementMethod.getInstance());

        webappID = getIntent().getIntExtra(Const.INTENT_WEBAPPID, -1);
        Utility.Assert(webappID != -1, "WebApp ID could not be retrieved.");
        isGlobalWebApp = webappID == DataManager.getInstance().getSettings().getGlobalWebApp().getID();

        final View inflated_view = binding.getRoot();

        if (isGlobalWebApp) {
            webapp = DataManager.getInstance().getSettings().getGlobalWebApp();
            prepareGlobalWebAppScreen();
        }
        else
            webapp = DataManager.getInstance().getWebAppIgnoringGlobalOverride(webappID, true);

        if (webapp == null) {
            finish();
        }
        else {
            final WebApp modified_webapp = new WebApp(webapp);
            binding.setWebapp(modified_webapp);

            final Button btnCreateShortcut = inflated_view.findViewById(R.id.btnRecreateShortcut);

            btnCreateShortcut.setOnClickListener(view -> {
                ShortcutDialogFragment frag = ShortcutDialogFragment.newInstance(webapp);
                frag.show(getSupportFragmentManager(), "SCFetcher-" + webapp.getID());

            });
            Button btnSave = findViewById(R.id.btnSave);
            Button btnCancel = findViewById(R.id.btnCancel);

            btnSave.setOnClickListener(v -> {
                ActivityManager activityManager =
                        (ActivityManager) App.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);

                //Global web app => close all webview activities, save to global settings
                if (isGlobalWebApp) {
                    for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
                        int id = task.getTaskInfo().baseIntent.getIntExtra(Const.INTENT_WEBAPPID, -1);
                        if (id != -1)
                            task.finishAndRemoveTask();

                    }
                    DataManager.getInstance().getSettings().setGlobalWebApp(modified_webapp);
                    DataManager.getInstance().saveGlobalSettings();
                }
                //Normal web app => only close that specific web app, save to webapp arraylist
                else {
                    for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
                        int id = task.getTaskInfo().baseIntent.getIntExtra(Const.INTENT_WEBAPPID, -1);
                        if (id == webappID)
                            task.finishAndRemoveTask();
                    }
                    DataManager.getInstance().replaceWebApp(modified_webapp);
                }

                Intent i = new Intent(WebAppSettingsActivity.this, MainActivity.class);
                i.putExtra(Const.INTENT_WEBAPP_CHANGED, true);
                finish();
                startActivity(i);
            });

            btnCancel.setOnClickListener(v -> finish());
            EditText txtBeginDarkMode = inflated_view.findViewById(R.id.textDarkModeBegin);
            EditText txtEndDarkMode = inflated_view.findViewById(R.id.textDarkModeEnd);
            handleSandboxes(inflated_view, modified_webapp);


            txtBeginDarkMode.setOnClickListener(view -> showTimePicker(txtBeginDarkMode));
            txtEndDarkMode.setOnClickListener(view -> showTimePicker(txtEndDarkMode));

            webapp.onSwitchExpertSettingsChanged(inflated_view.findViewById(R.id.switchExpertSettings), webapp.isShowExpertSettings());
            webapp.onSwitchOverrideGlobalSettingsChanged(findViewById(R.id.switchOverrideGlobal), webapp.isOverrideGlobalSettings());
            setPlusSettings(inflated_view);
        }
    }

    private void setPlusSettings(View v) {
        LinearLayout secDarkMode = v.findViewById(R.id.sectionDarkmode);
        LinearLayout secSandbox= v.findViewById(R.id.sectionSandbox);
        if (!BuildConfig.FLAVOR.equals("extended")) { 
            secDarkMode.setVisibility(View.GONE);
            secSandbox.setVisibility(View.GONE);

        }
    }

    private void handleSandboxes(View inflated_view, WebApp modified_webapp) {
        Spinner dropdownSandbox = inflated_view.findViewById(R.id.dropdownSandbox);

        List<String> sandboxes = DataManager.getInstance().getAvailableSandboxes(modified_webapp.getContainerId());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sandboxes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (sandboxes.size() == 1) {
            dropdownSandbox.setEnabled(false);
        }

        dropdownSandbox.setAdapter(adapter);
        if (modified_webapp.getContainerId() != Const.NO_CONTAINER) {
            for (int pos_idx = 1; pos_idx < sandboxes.size(); pos_idx++) {
                if (modified_webapp.getContainerId() == Integer.parseInt(sandboxes.get(pos_idx))) {
                    dropdownSandbox.setSelection(pos_idx);
                    break;
                }
            }

        }
        dropdownSandbox.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position == 0) {
                    modified_webapp.setContainerId(Const.NO_CONTAINER);
                }
                else {
                    modified_webapp.setContainerId(Integer.parseInt(sandboxes.get(position)));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                modified_webapp.setContainerId(Const.NO_CONTAINER);
            }

        });
    }

    private void showTimePicker(EditText txtField) {
        Calendar c = Utility.convertStringToCalendar(txtField.getText().toString());
        TimePickerDialog timePickerDialog = new TimePickerDialog(WebAppSettingsActivity.this, (timePicker, selectedHour, selectedMinute) -> {
            Calendar datetime = Calendar.getInstance();
            datetime.set(Calendar.HOUR_OF_DAY, selectedHour);
            datetime.set(Calendar.MINUTE, selectedMinute);
            txtField.setText(Utility.getHourMinFormat().format(datetime.getTime()));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        timePickerDialog.show();

    }

    private void prepareGlobalWebAppScreen() {
        findViewById(R.id.btnRecreateShortcut).setVisibility(View.GONE);
        findViewById(R.id.labelWebAppName).setVisibility(View.GONE);
        findViewById(R.id.txtWebAppName).setVisibility(View.GONE);
        findViewById(R.id.switchOverrideGlobal).setVisibility(View.GONE);
        findViewById(R.id.sectionSSL).setVisibility(View.GONE);
        findViewById(R.id.labelTitle).setVisibility(View.GONE);
        TextView page_title = findViewById(R.id.labelPageTitle);
        page_title.setText(getString(R.string.global_web_app_settings));

    }
}


