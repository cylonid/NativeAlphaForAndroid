package com.cylonid.nativealpha;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import static android.widget.LinearLayout.HORIZONTAL;


public class MainActivity extends AppCompatActivity {
    private LinearLayout mainScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainScreen = findViewById(R.id.mainScreen);

        DataManager.getInstance().loadAppData();
        Utility.applyUITheme();
        addActiveWebAppsToUI();

        if (DataManager.getInstance().getWebsites().size() == 0) {
            buildAddWebsiteDialog(getString(R.string.welcome_msg));
        }

        Utility.personalizeToolbar(this);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> buildAddWebsiteDialog(getString(R.string.add_webapp)));


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(Const.INTENT_BACKUP_RESTORED, false)) {

            mainScreen.removeAllViews();
            addActiveWebAppsToUI();

            boolean refresh_ui_mode = intent.getBooleanExtra(Const.INTENT_REFRESH_NEW_THEME, false);
            buildImportSuccessDialog(refresh_ui_mode);
            intent.putExtra(Const.INTENT_BACKUP_RESTORED, false);
            intent.putExtra(Const.INTENT_REFRESH_NEW_THEME, false);
        }
        if (intent.getBooleanExtra(Const.INTENT_WEBAPP_CHANGED, false)) {
            mainScreen.removeAllViews();
            addActiveWebAppsToUI();
            intent.putExtra(Const.INTENT_WEBAPP_CHANGED, false);
        }
    }


    private void buildImportSuccessDialog(boolean refresh_ui_mode) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String message =  getString(R.string.import_success_dialog_txt2) + "\n\n" + getString(R.string.import_success_dialog_txt3);

        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setTitle(getString(R.string.import_success, DataManager.getInstance().getActiveWebsitesCount()));
        builder.setPositiveButton(getString(android.R.string.yes), (dialog, id) -> {

            ArrayList<WebApp> webapps = DataManager.getInstance().getActiveWebsites();

            for (int i = webapps.size() - 1; i >= 0; i--) {
                WebApp webapp = webapps.get(i);
                boolean last_webapp = i == webapps.size() - 1;
                Spanned msg = Html.fromHtml(getString(R.string.restore_shortcut, webapp.getTitle()), Html.FROM_HTML_MODE_COMPACT);
                final AlertDialog addition_dialog = new AlertDialog.Builder(this)
                        .setMessage(msg)
                        .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                            ShortcutDialogFragment frag = ShortcutDialogFragment.newInstance(webapp);
                            if (refresh_ui_mode && last_webapp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                frag.refreshUIMode();
                            }
                            frag.show(getSupportFragmentManager(), "SCFetcher-" + webapp.getID());
                        })
                        .setNegativeButton(android.R.string.no, (dialog1, which) -> {
                            if (refresh_ui_mode && last_webapp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Utility.applyUITheme();
                            }
                        })
                        .create();

                addition_dialog.show();

            }

        });
        builder.setNegativeButton(getString(android.R.string.no),  (dialog, id) -> { Utility.applyUITheme();});
        builder.create().show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private ImageButton generateImageButton(String name, int resourceID, int webappID, LinearLayout ll_row) {
        int row_height = (int) getResources().getDimension(R.dimen.line_height);
        int transparent_color = ResourcesCompat.getColor(getResources(), R.color.transparent, null);

        ImageButton btn = new ImageButton(this);
        btn.setTag(name + webappID);
        btn.setBackgroundColor(transparent_color);
        btn.setImageResource(resourceID);
        LinearLayout.LayoutParams layout_action_buttons = new LinearLayout.LayoutParams(0, row_height, 1);
        btn.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn);

        return btn;
    }
    private void addRow(final WebApp webapp) {
        int row_height = (int) getResources().getDimension(R.dimen.line_height);
        int transparent_color = ResourcesCompat.getColor(getResources(), R.color.transparent, null);

        LinearLayout ll_row = new LinearLayout(this);
        ll_row.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams layout_row = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        ll_row.setLayoutParams(layout_row);

        Button btn_title = new Button(this);
        btn_title.setBackgroundColor(transparent_color);
        btn_title.setText(webapp.getSingleLineTitle());
        LinearLayout.LayoutParams layout_title = new LinearLayout.LayoutParams(0, row_height, 4);
        btn_title.setLayoutParams(layout_title);
        ll_row.addView(btn_title);

        ImageButton btn_open_webview = generateImageButton("btnOpenWebview", R.drawable.ic_baseline_open_in_browser_24, webapp.getID(), ll_row);
        btn_open_webview.setOnClickListener(v -> openWebView(webapp));

        ImageButton btn_settings = generateImageButton("btnSettings", R.drawable.ic_settings_black_24dp, webapp.getID(), ll_row);
        btn_settings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WebAppSettingsActivity.class);
            intent.putExtra(Const.INTENT_WEBAPPID, webapp.getID());
            intent.setAction(Intent.ACTION_VIEW);
            startActivity(intent);

        });

        ImageButton btn_delete = generateImageButton("btnDelete", R.drawable.ic_delete_black_24dp, webapp.getID(), ll_row);
        btn_delete.setOnClickListener(v -> buildDeleteItemDialog(webapp.getID()));

        mainScreen.addView(ll_row);

    }


    private void buildAddWebsiteDialog(String title) {
        final View inflated_view = getLayoutInflater().inflate(R.layout.add_website_dialogue, null);
        final EditText url = (EditText) inflated_view.findViewById(R.id.websiteUrl);
        final Switch create_shortcut = (Switch) inflated_view.findViewById(R.id.switchCreateShortcut);

        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(inflated_view)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            url.requestFocus();
            positive.setOnClickListener(view -> {
                String str_url = url.getText().toString().trim();

                if (!(str_url.startsWith("https://")) && !(str_url.startsWith("http://")))
                    str_url = "https://" + str_url;

                if (Patterns.WEB_URL.matcher(str_url.toLowerCase()).matches()) {
                    WebApp new_site = new WebApp(str_url, DataManager.getInstance().getIncrementedID());
                    new_site.applySettingsForNewWebApp();
                    DataManager.getInstance().addWebsite(new_site);

                    addRow(new_site);
                    dialog.dismiss();
                    if (create_shortcut.isChecked()) {
                        ShortcutDialogFragment frag = ShortcutDialogFragment.newInstance(new_site);
                        frag.show(getSupportFragmentManager(), "SCFetcher-" + new_site.getID());
                    }
                } else
                    url.setError(getString(R.string.enter_valid_url));
            });
        });
        dialog.show();
    }

    private void buildDeleteItemDialog(final int ID) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.delete_question));
        builder.setPositiveButton(getString(android.R.string.yes), (dialog, id) -> {
            WebApp webapp = DataManager.getInstance().getWebApp(ID);
            if (webapp != null) {
                webapp.markInactive();
            }
            mainScreen.removeAllViews();
            addActiveWebAppsToUI();


        });
        builder.setNegativeButton(getString(android.R.string.no), (dialog, id) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void addActiveWebAppsToUI() {
        for (WebApp d : DataManager.getInstance().getWebsites()) {
            if (d.isActiveEntry())
                addRow(d);
        }
    }

    private void openWebView(WebApp d) {
        startActivity(Utility.createWebViewIntent(d, MainActivity.this));
        //finish();
    }


}


