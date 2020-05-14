package com.cylonid.nativefier;

import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import static android.widget.LinearLayout.HORIZONTAL;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        LinearLayout mainScreen = (LinearLayout) findViewById(R.id.mainScreen);

        for (int i = 0; i < 15; i++)
            addRow(mainScreen);

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

    private void addRow(LinearLayout mainScreen)
    {
        int row_height = (int)getResources().getDimension(R.dimen.line_height);

        LinearLayout ll_row = new LinearLayout(this);
        ll_row.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams layout_row = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        layout_row.width = LinearLayout.LayoutParams.MATCH_PARENT;
        layout_row.height = row_height;
        ll_row.setLayoutParams(layout_row);

        Button btn_title = new Button(this);
//        btn_title.setId("btn_title");
        btn_title.setBackgroundColor(Color.parseColor("#07000000"));
        btn_title.setText("ORF.at");
//        btn_title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_black_24dp);
//        btn_title.setGravity(Gravity.START | Gravity.CENTER);
        LinearLayout.LayoutParams layout_title = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row_height);
        layout_title.width = 0;
        layout_title.height = (int)getResources().getDimension(R.dimen.line_height);
        layout_title.weight = 4;
        btn_title.setLayoutParams(layout_title);
        ll_row.addView(btn_title);

        ImageButton btn_settings = new ImageButton(this);
        btn_settings.setBackgroundColor(Color.parseColor("#07000000"));
        btn_settings.setImageResource(R.drawable.ic_settings_black_24dp);
        LinearLayout.LayoutParams layout_action_buttons = new LinearLayout.LayoutParams(0, row_height);
        layout_action_buttons.width = 0;
        layout_action_buttons.height = row_height;
        layout_action_buttons.weight = 1;
        btn_settings.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_settings);

        ImageButton btn_delete = new ImageButton(this);
        btn_delete.setBackgroundColor(Color.parseColor("#07000000"));
        btn_delete.setImageResource(R.drawable.ic_delete_black_24dp);
        btn_delete.setLayoutParams(layout_action_buttons);
        ll_row.addView(btn_delete);

        mainScreen.addView(ll_row);
    }

}


