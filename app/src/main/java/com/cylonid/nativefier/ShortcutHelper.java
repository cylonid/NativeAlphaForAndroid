package com.cylonid.nativefier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortcutHelper {
    private String full_url;
    private Context c;
    private WebsiteData d;
    private Bitmap bitmap;
    private ImageView view_favicon;
    private CircularProgressBar circularProgressBar;
    private static String USER_AGENT = "Mozilla/5.0 (Android 10; Mobile; rv:68.0) Gecko/68.0 Firefox/68.0";


    public ShortcutHelper(WebsiteData d, Context c) {
        this.d = d;
        this.full_url = d.getUrl();
        this.c = c;
        this.bitmap = null;
    }

    public void fetchFaviconURL() {
        buildShortcutDialog();
        new FaviconURLFetcher().execute();
    }
    private void loadFavicon(String url) {
        Target target = new Target() {

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                showFailedMessage();
                addShortcutToHomeScreen(d, null);
            }

            @Override
            public void onBitmapLoaded(Bitmap loaded, Picasso.LoadedFrom from) {
//                addShortcutToHomeScreen(d, bitmap);
                bitmap = loaded;
                view_favicon.setImageBitmap(bitmap);
                circularProgressBar.setVisibility(View.GONE);
                view_favicon.setVisibility(View.VISIBLE);

            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };

        if (url != null)
            Picasso.get().load(url).into(target);
        else {
            showFailedMessage();
            addShortcutToHomeScreen(d, null);
        }
    }

    private void addShortcutToHomeScreen(WebsiteData d, Bitmap bitmap)
    {
        Intent intent = Utility.createWebViewIntent(d, c);

        IconCompat icon;
        if (bitmap != null)
            icon = IconCompat.createWithBitmap(bitmap);
        else
            icon = IconCompat.createWithResource(c, R.drawable.ic_launcher_background);

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(c)) {

            ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(c, d.getName())
                    .setIcon(icon)
                    .setShortLabel(d.getName())
                    .setLongLabel(d.getName())
                    .setIntent(intent)
                    .build();
            ShortcutManagerCompat.requestPinShortcut(c, pinShortcutInfo, null);

        }

    }
    private void showFailedMessage() {
        Toast toast = Toast.makeText(c,"We could not retrieve an icon for the selected website.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 100);
        toast.show();

    }

    private Integer getWidthFromIcon(String size_string) {
        int x_index = size_string.indexOf("x");
        String width = size_string.substring(0, x_index);

        return Integer.parseInt(width);
    }

    private void buildShortcutDialog() {
        LayoutInflater li = LayoutInflater.from(c);
        final View inflated_view = li.inflate(R.layout.shortcut_dialog, null);
        final EditText title = (EditText) inflated_view.findViewById(R.id.websiteTitle);
        view_favicon = (ImageView) inflated_view.findViewById(R.id.favicon);
        circularProgressBar =  (CircularProgressBar)inflated_view.findViewById(R.id.circularProgressBar);

        final AlertDialog dialog = new AlertDialog.Builder(c)
                .setView(inflated_view)
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
                        addShortcutToHomeScreen(d, bitmap);
                    }
                });
            }
        });
        dialog.show();
    }


    private class FaviconURLFetcher extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... voids) {

            //TreeMap<Icon width in px, URL>
            TreeMap<Integer, String> found_icons = new TreeMap<>();

            try {
                //Connect to the website
                Document doc = Jsoup.connect(full_url).userAgent(USER_AGENT).followRedirects(true).get();
                Elements metaTags = doc.select("meta[http-equiv=refresh]");

                //Step 1: Check for META Redirect
                for (Element metaTag : metaTags) {
                    String content = metaTag.attr("content");
                    Pattern pattern = Pattern.compile(".*URL='?(.*)$", Pattern.CASE_INSENSITIVE);
                    Matcher m = pattern.matcher(content);
                    String redirectUrl = m.matches() ? m.group(1) : null;
                    if (redirectUrl != null) {
                        full_url = redirectUrl;
                        doc = Jsoup.connect(full_url).followRedirects(true).get();
                    }
                }
                //Step 2: Check PWA manifest
                Elements manifest = doc.select("link[rel=manifest]");
                if (!manifest.isEmpty()) {
                    for (Element mf : manifest) {
                        String data = Jsoup.connect(mf.absUrl("href")).ignoreContentType(true).execute().body();
                        JSONObject json = new JSONObject(data);
                        JSONArray manifest_icons = json.getJSONArray("icons");

                        for (int i = 0; i < manifest_icons.length(); i++) {
                            String icon_href = manifest_icons.getJSONObject(i).getString("src");
                            String sizes = manifest_icons.getJSONObject(i).getString("sizes");
                            Integer width = getWidthFromIcon(sizes);
                            URL base_url = new URL(mf.absUrl("href"));
                            URL full_url = new URL(base_url, icon_href);
                            found_icons.put(width, full_url.toString());
                        }
                    }

                }
                //Step 3: Use PNG icons
                else {
                    Elements icons = doc.select("link[rel=icon]");

                    for (Element icon : icons) {

                        String icon_href = icon.absUrl("href");
                        String sizes = icon.attr("sizes");
                        Integer width = getWidthFromIcon(sizes);
                        found_icons.put(width, icon_href);

                    }
                }

                if (!found_icons.isEmpty()) {
                    Map.Entry<Integer, String> best_fit = found_icons.lastEntry();
                    if (best_fit.getKey() < 96)
                        return null;
                    else
                        return best_fit.getValue();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

            @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            loadFavicon(result);
//            imageView.setImageBitmap(bitmap);
//            textView.setText(title);
//            progressDialog.dismiss();
        }
    }
}
