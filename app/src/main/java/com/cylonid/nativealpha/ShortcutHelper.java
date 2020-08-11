package com.cylonid.nativealpha;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ShortcutHelper {

    private final Activity activity;
    private final WebApp webapp;
    private Bitmap bitmap;
    private ImageView uiFavicon;
    private CircularProgressBar uiProgressBar;
    private EditText uiTitle;
    private LinearLayout uiIconLayout;
    private Button uiBtnPositive;

    public ShortcutHelper(WebApp webapp, Activity c) {
        this.webapp = webapp;
        this.activity = c;
        this.bitmap = null;
    }

    private void addShortcutToHomeScreen(Bitmap bitmap) {
        Intent intent = Utility.createWebViewIntent(webapp, activity);

        IconCompat icon;
        if (bitmap != null)
            icon = IconCompat.createWithBitmap(bitmap);
        else
            icon = IconCompat.createWithResource(activity, R.mipmap.native_alpha_shortcut);

        String final_title = uiTitle.getText().toString();
        if (final_title.equals(""))
            final_title = webapp.getTitle();

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {

            ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(activity, final_title)
                    .setIcon(icon)
                    .setShortLabel(final_title)
                    .setLongLabel(final_title)
                    .setIntent(intent)
                    .build();
            ShortcutManagerCompat.requestPinShortcut(activity, pinShortcutInfo, null);

        }

    }

    private void prepareFailedUI() {
        showFailedMessage();
        uiIconLayout.setVisibility(View.GONE);
        uiBtnPositive.setEnabled(true);
        uiTitle.setText(webapp.getTitle());
        uiTitle.requestFocus();
    }

    private void showFailedMessage() {
        Toast toast = Toast.makeText(activity, activity.getString(R.string.icon_fetch_failed_line1) + activity.getString(R.string.icon_fetch_failed_line2) + activity.getString(R.string.icon_fetch_failed_line3), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }

    public void buildShortcutDialog() {
        LayoutInflater li = LayoutInflater.from(activity);
        final View inflated_view = li.inflate(R.layout.shortcut_dialog, null);
        uiIconLayout = inflated_view.findViewById(R.id.layoutIcon);
        uiTitle = (EditText) inflated_view.findViewById(R.id.websiteTitle);
        uiFavicon = (ImageView) inflated_view.findViewById(R.id.favicon);
        uiProgressBar = (CircularProgressBar) inflated_view.findViewById(R.id.circularProgressBar);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(inflated_view)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                uiBtnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                uiBtnPositive.setEnabled(false);
                uiBtnPositive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        addShortcutToHomeScreen(bitmap);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private void setShortcutTitle(String shortcut_title) {
        if (shortcut_title != null) {
            if (!shortcut_title.equals(""))
                uiTitle.setText(shortcut_title);
            else
                uiTitle.setText(webapp.getTitle());
        }
    }

    private void applyNewBaseUrl(String url) {
        if (url != null)
            webapp.setBaseUrl(url);
    }


    public static class FaviconFetcher extends AsyncTask<Void, Void, String[]> {

        //TreeMap<Icon width in px, URL>
        final TreeMap<Integer, String> found_icons;
        private String base_url;
        private final ShortcutHelper shortcutHelper;
        private FaviconFetcher asyncObject;

        public FaviconFetcher(ShortcutHelper s) {
            found_icons = new TreeMap<>();
            this.base_url = s.webapp.getBaseUrl();
            shortcutHelper = s;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            shortcutHelper.buildShortcutDialog();
                asyncObject = this;
                new CountDownTimer(7000, 7000) {
                    public void onTick(long millisUntilFinished) {
                        // You can monitor the progress here as well by changing the onTick() time
                    }
                    public void onFinish() {
                        // stop async task if not in progress
                        if (asyncObject.getStatus() == AsyncTask.Status.RUNNING) {
                            asyncObject.cancel(true);

                            Log.d("SHORTCUT", "Timeout");
                        }
                    }
                }.start();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            shortcutHelper.prepareFailedUI();
        }

        @Override
        protected String[] doInBackground(Void... voids) {

            String[] result = new String[] {null, null, null};

            try {
                //Connect to the website
                Document doc = Jsoup.connect(base_url).userAgent(Const.MOBILE_USER_AGENT).followRedirects(true).get();

                //Step 1: Check for META Redirect
                Elements metaTags = doc.select("meta[http-equiv=refresh]");
                if (!metaTags.isEmpty()) {
                    Element metaTag = metaTags.first();
                    String content = metaTag.attr("content");
                    Pattern pattern = Pattern.compile(".*URL='?(.*)$", Pattern.CASE_INSENSITIVE);
                    Matcher m = pattern.matcher(content);
                    String redirectUrl = m.matches() ? m.group(1) : null;
                    if (redirectUrl != null) {
                        base_url = redirectUrl;
                        doc = Jsoup.connect(base_url).followRedirects(true).get();
                    }
                }
                //Step 2: Check PWA manifest
                Elements manifest = doc.select("link[rel=manifest]");
                if (!manifest.isEmpty()) {
                    Element mf = manifest.first();
                    String data = Jsoup.connect(mf.absUrl("href")).ignoreContentType(true).execute().body();
                    JSONObject json = new JSONObject(data);

                    try {
                        result[Const.RESULT_IDX_TITLE] = json.getString("name");
                        String start_url = json.getString("start_url");
                        if (!start_url.isEmpty()) {
                            URL base_url = new URL(mf.absUrl("href"));
                            URL fl_url = new URL(base_url, start_url);
                            result[Const.RESULT_IDX_NEW_BASEURL] = fl_url.toString();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        JSONArray manifest_icons = json.getJSONArray("icons");

                        for (int i = 0; i < manifest_icons.length(); i++) {
                            String icon_href = manifest_icons.getJSONObject(i).getString("src");
                            String sizes = manifest_icons.getJSONObject(i).getString("sizes");
                            Integer width = Utility.getWidthFromIcon(sizes);
                            URL base_url = new URL(mf.absUrl("href"));
                            URL full_url = new URL(base_url, icon_href);
                            found_icons.put(width, full_url.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //Step 3: Fallback to PNG icons
                if (found_icons.isEmpty()) {

                    Elements html_title = doc.select("title");
                    if (!html_title.isEmpty())
                        result[Const.RESULT_IDX_TITLE] = html_title.first().text();

                    Elements icons = doc.select("link[rel=icon]");
                    icons.addAll(doc.select("link[rel=shortcut icon]"));
                    //If necessary, use apple icons
                    if (icons.size() < 2) {
                        Elements apple_icons = doc.select("link[rel=apple-touch-icon]");
                        Elements apple_icons_prec = doc.select("link[rel=apple-touch-icon-precomposed]");
                        icons.addAll(apple_icons);
                        icons.addAll(apple_icons_prec);
                    }

                    for (Element icon : icons) {
                        String icon_href = icon.absUrl("href");
                        String sizes = icon.attr("sizes");
                        if (!sizes.equals("")) {
                            Integer width = Utility.getWidthFromIcon(sizes);
                            found_icons.put(width, icon_href);
                        } else
                            found_icons.put(1, icon_href);

                    }
                }
                addHardcodedIcons();

                if (!found_icons.isEmpty()) {
                    Map.Entry<Integer, String> best_fit = found_icons.lastEntry();
                    result[Const.RESULT_IDX_FAVICON] = best_fit.getValue();
                    loadBitmap(best_fit.getValue());

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            if (shortcutHelper.bitmap != null) {
                shortcutHelper.uiFavicon.setImageBitmap(shortcutHelper.bitmap);
                shortcutHelper.uiProgressBar.setVisibility(View.GONE);
                shortcutHelper.uiFavicon.setVisibility(View.VISIBLE);
                shortcutHelper.uiBtnPositive.setEnabled(true);
                shortcutHelper.uiTitle.requestFocus();
            }
            else
                shortcutHelper.prepareFailedUI();

            shortcutHelper.setShortcutTitle(result[Const.RESULT_IDX_TITLE]);
            shortcutHelper.applyNewBaseUrl(result[Const.RESULT_IDX_NEW_BASEURL]);


        }

        private void loadBitmap(String url)  {
            InputStream inputStream;
            try {
                inputStream = new java.net.URL(url).openStream();
                shortcutHelper.bitmap = BitmapFactory.decodeStream(inputStream);
                if (shortcutHelper.bitmap.getWidth() < 96)
                    shortcutHelper.bitmap = null;

            } catch (IOException e) {
                shortcutHelper.bitmap = null;
                e.printStackTrace();
            }
        }
        private void addHardcodedIcons() {

            //Amazon has no icon other than low-res favicon.ico
            if (base_url.contains("amazon"))
                found_icons.put(300, "https://s3.amazonaws.com/prod-widgetSource/in-shop/pub/images/amzn_favicon_blk.png");

            //Google has no icon other than low-res favicon.ico
            if (base_url.contains("https://google."))
                found_icons.put(240, "https://www.gstatic.com/images/branding/googleg/2x/googleg_standard_color_120dp.png");

            //OEBB has a typo in its web manifest
            if (base_url.contains("oebb.at"))
                found_icons.put(Integer.MAX_VALUE, "https://www.oebb.at/.resources/pv-2017/themes/images/favicons/android-chrome-192x192.png");

            //XDA has a wrong relative path in its web manifest
            if (base_url.contains("xda-developers.com"))
                found_icons.put(Integer.MAX_VALUE, "https://www.xda-developers.com/wp-content/themes/trendyblog-theme/images/android-chrome-512x512.png");
        }
    }
}
