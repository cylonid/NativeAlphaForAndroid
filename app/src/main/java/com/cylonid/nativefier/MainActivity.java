package com.cylonid.nativefier;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
                Toast.makeText(getApplicationContext(), "Titel angeklickt: "+ data.getName(), Toast.LENGTH_LONG).show();

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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View inflated_view = getLayoutInflater().inflate(R.layout.add_website_dialogue, null);
        builder.setView(inflated_view);
        final EditText title = (EditText) inflated_view.findViewById(R.id.websiteTitle);
        final EditText url = (EditText) inflated_view.findViewById(R.id.websiteUrl);
        final Switch open_url_external = (Switch) inflated_view.findViewById(R.id.switchOpenUrlExternal);
        builder.setTitle("Add new website");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                String str_title = title.getText().toString();
                String str_url = url.getText().toString();

                if (str_title.trim().equals(""))
                    title.setError("Please input the title.");
                else if (str_url.trim().equals(""))
                    url.setError("Please input the web address.");
                else {
                    WebsiteData new_site = new WebsiteData(str_title, str_url, open_url_external.isChecked());
                    WebsiteDataManager.getInstance().addWebsite(new_site);
                    addRow(new_site);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();

            }
        });
        AlertDialog dialog = builder.create();
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

}


