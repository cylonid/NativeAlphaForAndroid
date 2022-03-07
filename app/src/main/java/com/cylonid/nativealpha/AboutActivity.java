package com.cylonid.nativealpha;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mikepenz.aboutlibraries.LibsBuilder;

import java.time.Year;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View aboutPage = new AboutPage(this)
                .enableDarkMode(false)
                .setDescription("Native Alpha for Android\nby cylonid © " + Year.now().getValue())
                .setImage(R.drawable.native_alpha_foreground)
                .addItem(new Element().setTitle("Version " + BuildConfig.VERSION_NAME))
                .addGitHub("cylonid", "Find us on GitHub")
                .addPlayStore("com.cylonid.nativealpha")
                .addGroup(getString(R.string.eula_title))
                .addItem(showEULA())
                .addGroup(getString(R.string.license))
                .addItem(showLicense())
                .addItem(showOpenSourcelibs())
                .create();

        setContentView(aboutPage);
    }

    Element showEULA() {
        Element license = new Element();
        license.setTitle(getString(R.string.eula_content));
        return license;
    }
    Element showLicense() {
        Element license = new Element();

        license.setTitle(getString(R.string.gnu_license));
        license.setOnClickListener(v -> {
            String url = "https://www.gnu.org/licenses/gpl-3.0.txt";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
        return license;

    }
    Element showPayPal() {
        Element license = new Element();

        license.setTitle(getString(R.string.paypal));
        license.setOnClickListener(v -> {
            String url = "https://paypal.me/cylonid";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
        return license;

    }

    Element showOpenSourcelibs() {
        Element os = new Element();
        os.setTitle(getString(R.string.open_source_libs));
        os.setOnClickListener(v -> {
            startActivity(new LibsBuilder()
                    .withEdgeToEdge(true)
                    .withSearchEnabled(true)
                    .intent(this));
        });
        return os;
    }



//    Element getCopyRightsElement() {
//        Element copyRightsElement = new Element();
//        final String copyrights = String.format(getString(R.string.copy_right), Calendar.getInstance().get(Calendar.YEAR));
//        copyRightsElement.setTitle(copyrights);
//        copyRightsElement.setIconDrawable(R.drawable.about_icon_copy_right);
//        copyRightsElement.setAutoApplyIconTint(true);
//        copyRightsElement.setIconTint(mehdi.sakout.aboutpage.R.color.about_item_icon_color);
//        copyRightsElement.setIconNightTint(android.R.color.white);
//        copyRightsElement.setGravity(Gravity.CENTER);
//        copyRightsElement.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(AboutActivity.this, copyrights, Toast.LENGTH_SHORT).show();
//            }
//        });
//        return copyRightsElement;
//    }
}