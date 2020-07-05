package com.cylonid.nativealpha;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static android.widget.LinearLayout.HORIZONTAL;


public class MainActivity extends AppCompatActivity {
    LinearLayout mainScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainScreen = findViewById(R.id.mainScreen);
        DataManager.getInstance().initContext(this);
        DataManager.getInstance().loadAppData();
        addActiveWebAppsToUI();

        if (DataManager.getInstance().getWebsites().size() == 0) {
            buildAddWebsiteDialog("Welcome!\nAdd your first web shortcut:");
        }

        Utility.personalizeToolbar(this);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildAddWebsiteDialog("Add website");
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

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
            moveTaskToBack(true);
    }

    private void addRow(final WebApp webapp)
    {
        int row_height = (int)getResources().getDimension(R.dimen.line_height);
        int transparent_color = ResourcesCompat.getColor(getResources(), R.color.transparent, null);

        LinearLayout ll_row = new LinearLayout(this);
        ll_row.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams layout_row = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        layout_row.width = LinearLayout.LayoutParams.MATCH_PARENT;
        layout_row.height = row_height;
        ll_row.setLayoutParams(layout_row);

        Button btn_title = new Button(this);
//        btn_title.setId("btn_title");
        btn_title.setBackgroundColor(transparent_color);
        btn_title.setText(webapp.getTitle());
//        btn_title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_24dp);
//        btn_title.setGravity(Gravity.START | Gravity.CENTER);
        LinearLayout.LayoutParams layout_title = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        layout_title.width = 0;
        layout_title.height = (int)getResources().getDimension(R.dimen.line_height);
        layout_title.weight = 4;
        btn_title.setLayoutParams(layout_title);
        ll_row.addView(btn_title);

        ImageButton btn_shortcut = new ImageButton(this);
        btn_shortcut.setBackgroundColor(transparent_color);
        btn_shortcut.setImageResource(R.drawable.ic_baseline_open_in_browser_24);
        LinearLayout.LayoutParams layout_action_buttons = new LinearLayout.LayoutParams(0, row_height);
        layout_action_buttons.width = 0;
        layout_action_buttons.height = row_height;
        layout_action_buttons.weight = 1;
        btn_shortcut.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_shortcut);
        btn_shortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        openWebView(webapp);
            };
        });

        ImageButton btn_settings = new ImageButton(this);
        btn_settings.setBackgroundColor(transparent_color);
        btn_settings.setImageResource(R.drawable.ic_settings_black_24dp);
        layout_action_buttons.width = 0;
        layout_action_buttons.height = row_height;
        layout_action_buttons.weight = 1;
        btn_settings.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_settings);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildSettingsDialog(webapp.getID());

            }
        });

        ImageButton btn_delete = new ImageButton(this);
        btn_delete.setBackgroundColor(transparent_color);
        btn_delete.setImageResource(R.drawable.ic_delete_black_24dp);
        btn_delete.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_delete);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    buildDeleteItemDialog(webapp.getID());

            }
        });

        mainScreen.addView(ll_row);


    }

    private void buildSettingsDialog(final int webappID) {
        final View inflated_view = getLayoutInflater().inflate(R.layout.webapp_settings, null);
        final WebApp webapp = DataManager.getInstance().getWebApp(webappID);

        final Switch switchOpenUrlExternal = inflated_view.findViewById(R.id.switchOpenUrlExternal);
        final Switch switchCookies = inflated_view.findViewById(R.id.switchCookies);
        final Switch switchThirdPartyCookies = inflated_view.findViewById(R.id.switch3PCookies);
        final Switch switchDesktopVersion = inflated_view.findViewById(R.id.switchDesktopSite);
        final Switch switchJS = inflated_view.findViewById(R.id.switchJavascript);
        final Switch switchRestorePage = inflated_view.findViewById(R.id.switchRestorePage);
        final Switch switchCache = inflated_view.findViewById(R.id.switchCache);
        final Switch switchAdblock = inflated_view.findViewById(R.id.switchAdblock);
        final EditText textTimeout = inflated_view.findViewById(R.id.textTimeout);
        final Button btnCreateShortcut = inflated_view.findViewById(R.id.btnRecreateShortcut);

        switchOpenUrlExternal.setChecked(webapp.openUrlExternal());
        switchCookies.setChecked(webapp.isAllowCookiesSet());
        switchThirdPartyCookies.setChecked(webapp.isAllowThirdPartyCookiesSet());
        switchDesktopVersion.setChecked(webapp.isRequestDesktopSet());
        switchJS.setChecked(webapp.isAllowJSSet());
        switchRestorePage.setChecked(webapp.isRestorePageSet());
        switchCache.setChecked(webapp.isClearCacheSet());
        switchAdblock.setChecked(webapp.isUseAdblock());

        if (!webapp.isRestorePageSet()) {
            textTimeout.setEnabled(false);
        }
        else {
            textTimeout.setEnabled(true);
            textTimeout.setText(String.valueOf(webapp.getTimeou()));
        }

        if (!webapp.isAllowCookiesSet()) {
            switchThirdPartyCookies.setChecked(false);
            switchThirdPartyCookies.setEnabled(false);
        }

        if (!webapp.isAllowJSSet()) {
            switchDesktopVersion.setChecked(false);
            switchDesktopVersion.setEnabled(false);
            switchAdblock.setChecked(false);
            switchAdblock.setEnabled(false);
        }
        switchRestorePage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    textTimeout.setEnabled(true);
                else
                    textTimeout.setEnabled(false);
            }
        });

        switchCookies.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    switchThirdPartyCookies.setEnabled(true);
                else {
                    switchThirdPartyCookies.setChecked(false);
                    switchThirdPartyCookies.setEnabled(false);
                }
            }
        });

        switchJS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    switchDesktopVersion.setEnabled(true);
                    switchAdblock.setEnabled(true);
                }
                else {
                    switchDesktopVersion.setChecked(false);
                    switchDesktopVersion.setEnabled(false);
                    switchAdblock.setChecked(false);
                    switchAdblock.setEnabled(false);
                }
            }
        });

        btnCreateShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShortcutHelper s = new ShortcutHelper(webapp, MainActivity.this);
                s.fetchFaviconURL();
            }
        });


        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(inflated_view)
                .setTitle("Edit webapp settings")
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
                        DataManager.getInstance().getWebApp(webappID).saveNewSettings(switchAdblock.isChecked(), switchOpenUrlExternal.isChecked(), switchDesktopVersion.isChecked(), switchCookies.isChecked(), switchThirdPartyCookies.isChecked(), switchJS.isChecked(), switchCache.isChecked(), switchRestorePage.isChecked(), Integer.parseInt(textTimeout.getText().toString()));
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
                                ShortcutHelper fav = new ShortcutHelper(new_site, MainActivity.this);
                                fav.fetchFaviconURL();

                            }
                        }
                        else
                            url.setError("Please input a valid web address.");
                    }
                });
            }
        });
        dialog.show();
    }

    private void buildDeleteItemDialog(final int ID) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to remove this website?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                DataManager.getInstance().getWebApp(ID).markInactive();
                mainScreen.removeAllViews();
                addActiveWebAppsToUI();


            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addActiveWebAppsToUI () {
        for (WebApp d : DataManager.getInstance().getWebsites()) {
            if (d.isActive())
                addRow(d);
        }
    }

    private void openWebView(WebApp d) {
        startActivity(Utility.createWebViewIntent(d, MainActivity.this));
        //finish();
    }


}


