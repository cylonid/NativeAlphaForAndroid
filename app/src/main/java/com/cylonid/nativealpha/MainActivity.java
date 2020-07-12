package com.cylonid.nativealpha;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
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
import androidx.databinding.DataBindingUtil;

import com.cylonid.nativealpha.databinding.WebappSettingsBinding;
import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Utility;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static android.widget.LinearLayout.HORIZONTAL;


public class MainActivity extends AppCompatActivity {
    private LinearLayout mainScreen;
    private ShortcutHelper.FaviconFetcher faviconFetcher = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainScreen = findViewById(R.id.mainScreen);

        DataManager.getInstance().loadAppData();
        addActiveWebAppsToUI();

        if (DataManager.getInstance().getWebsites().size() == 0) {
            buildAddWebsiteDialog(getString(R.string.welcome_msg));
        }

        Utility.personalizeToolbar(this);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildAddWebsiteDialog(getString(R.string.add_webapp));
            }
        });

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

        //noinspection SimplifiableIfStatement
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faviconFetcher != null)
            faviconFetcher.cancel(true);
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
        btn_title.setText(webapp.getTitle());
        LinearLayout.LayoutParams layout_title = new LinearLayout.LayoutParams(0, row_height, 4);
        btn_title.setLayoutParams(layout_title);
        ll_row.addView(btn_title);

        ImageButton btn_open_webview = generateImageButton("btnOpenWebview", R.drawable.ic_baseline_open_in_browser_24, webapp.getID(), ll_row);
        btn_open_webview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebView(webapp);
            }

        });

        ImageButton btn_settings = generateImageButton("btnSettings", R.drawable.ic_settings_black_24dp, webapp.getID(), ll_row);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { buildSettingsDialog(webapp.getID());
            }
        });

        ImageButton btn_delete = generateImageButton("btnDelete", R.drawable.ic_delete_black_24dp, webapp.getID(), ll_row);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { buildDeleteItemDialog(webapp.getID());
            }
        });

        mainScreen.addView(ll_row);

    }


    private void buildSettingsDialog(final int webappID) {

        WebappSettingsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.webapp_settings, null, false);
        final View inflated_view = binding.getRoot();
        final WebApp webapp = DataManager.getInstance().getWebApp(webappID);
        final WebApp modified_webapp = new WebApp(webapp);
        binding.setWebapp(modified_webapp);

        final Button btnCreateShortcut = inflated_view.findViewById(R.id.btnRecreateShortcut);

        btnCreateShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faviconFetcher = new ShortcutHelper.FaviconFetcher(new ShortcutHelper(webapp, MainActivity.this));
                faviconFetcher.execute();
                faviconFetcher = null;
            }
        });


        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(inflated_view)
                .setTitle(getString(R.string.edit_webapp_settings))
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DataManager.getInstance().replaceWebApp(modified_webapp);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();


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

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                url.requestFocus();
                positive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String str_url = url.getText().toString();

                        if (!(str_url.startsWith("https://")) && !(str_url.startsWith("http://")))
                            str_url = "https://" + str_url;

                        if (Patterns.WEB_URL.matcher(str_url.toLowerCase()).matches()) {
                            WebApp new_site = new WebApp(str_url);
                            DataManager.getInstance().addWebsite(new_site);

                            addRow(new_site);
                            dialog.dismiss();
                            if (create_shortcut.isChecked()) {
                                ShortcutHelper.FaviconFetcher f = new ShortcutHelper.FaviconFetcher(new ShortcutHelper(new_site, MainActivity.this));
                                f.execute();
                            }
                        } else
                            url.setError(getString(R.string.enter_valid_url));
                    }
                });
            }
        });
        dialog.show();
    }

    private void buildDeleteItemDialog(final int ID) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.delete_question));
        builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                DataManager.getInstance().getWebApp(ID).markInactive();
                mainScreen.removeAllViews();
                addActiveWebAppsToUI();


            }
        });
        builder.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
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


