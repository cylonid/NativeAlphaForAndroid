package com.cylonid.nativefier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static android.widget.LinearLayout.HORIZONTAL;

public class MainActivity extends AppCompatActivity {

     LinearLayout mainScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainScreen = (LinearLayout) findViewById(R.id.mainScreen);
        WebsiteDataManager.getInstance().initContext(this);
//        WebsiteDataManager.getInstance().initDummyData();

        for (WebsiteData d : WebsiteDataManager.getInstance().getWebsites())
            addRow(d);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildAddWebsiteDialog();
            }
        });

        addShortcutToHomeScreen();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
            moveTaskToBack(true);
    }

    private void addRow(final WebsiteData data)
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
        btn_title.setText(data.getName());
//        btn_title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_black_24dp);
//        btn_title.setGravity(Gravity.START | Gravity.CENTER);
        LinearLayout.LayoutParams layout_title = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        layout_title.width = 0;
        layout_title.height = (int)getResources().getDimension(R.dimen.line_height);
        layout_title.weight = 4;
        btn_title.setLayoutParams(layout_title);
        ll_row.addView(btn_title);

        btn_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebView(data);
            }
        });

        ImageButton btn_settings = new ImageButton(this);
        btn_settings.setBackgroundColor(transparent_color);
        btn_settings.setImageResource(R.drawable.ic_settings_black_24dp);
        LinearLayout.LayoutParams layout_action_buttons = new LinearLayout.LayoutParams(0, row_height);
        layout_action_buttons.width = 0;
        layout_action_buttons.height = row_height;
        layout_action_buttons.weight = 1;
        btn_settings.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_settings);

        ImageButton btn_delete = new ImageButton(this);
        btn_delete.setBackgroundColor(transparent_color);
        btn_delete.setImageResource(R.drawable.ic_delete_black_24dp);
        btn_delete.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_delete);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    buildDeleteItemDialog(data.getID());

            }
        });

        mainScreen.addView(ll_row);


    }

    private void buildAddWebsiteDialog() {
        final View inflated_view = getLayoutInflater().inflate(R.layout.add_website_dialogue, null);
        final EditText title = (EditText) inflated_view.findViewById(R.id.websiteTitle);
        final EditText url = (EditText) inflated_view.findViewById(R.id.websiteUrl);
        final Switch open_url_external = (Switch) inflated_view.findViewById(R.id.switchOpenUrlExternal);

        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(inflated_view)
                .setTitle("Add website")
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
                        String str_title = title.getText().toString();
                        String str_url = url.getText().toString();

                        if (str_title.trim().equals(""))
                            str_title = str_url.replace("http://", "").replace("https://", "").replace("http://www.", "").replace("https://www.", "");

                        if (!(str_url.startsWith("https://")) && !(str_url.startsWith("http://")))
                            str_url = "https://" + str_url;

                        if (Patterns.WEB_URL.matcher(str_url.toLowerCase()).matches()) {
                            WebsiteData new_site = new WebsiteData(str_title, str_url, open_url_external.isChecked());
                            WebsiteDataManager.getInstance().addWebsite(new_site);
                            addRow(new_site);
                            dialog.dismiss();
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
                WebsiteDataManager.getInstance().removeWebsite(ID);
                mainScreen.removeAllViews();
                for (WebsiteData d : WebsiteDataManager.getInstance().getWebsites())
                    addRow(d);
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

    private void openWebView(WebsiteData d) {
        startActivity(getWebViewIntent(d));
        finish();
    }

    private Intent getWebViewIntent(WebsiteData d) {
        Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
        intent.putExtra("URL", d.getUrl());
        intent.putExtra("open_external", d.openUrlExternal());
        return intent;
    }
    public void addShortcutToHomeScreen()
    {
        WebsiteData d = new WebsiteData("ORF.at", "https://orf.at");
        Intent intent = getWebViewIntent(d);
        intent.addCategory("android.shortcut.conversation");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            final ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, d.getName())
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setShortLabel(d.getName())
                    .setIntent(intent)
                    .build();
            shortcutManager.requestPinShortcut(pinShortcutInfo, null);
        }


    }





}


