package com.cylonid.nativefier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompatSideChannelService;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.mm2d.touchicon.Icon;
import net.mm2d.touchicon.TouchIconExtractor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShortcutHelper {
    private String full_url;
    private ImageView view;
    private Context c;
    private WebsiteData d;
    private static  String NO_ICON = "no_icon_found";

    public ShortcutHelper(WebsiteData d, Context c) {
        this.d = d;
        this.full_url = d.getUrl();
        this.c = c;
        this.view = new ImageView(c);
    }

    public void fetchFaviconURL() throws IOException {
//        new FaviconURLTask().execute();
        new Content().execute();

    }
    private void loadFavicon(String url) {
        Target target = new Target() {

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                showFailedMessage();
                addShortcutToHomeScreen(d, null);
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                addShortcutToHomeScreen(d, bitmap);
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };

        if (!url.equals(NO_ICON))
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


    private void htmlRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    String full = response.body().string();
                    Document doc = Jsoup.parse(full);
//                    Elements metaTags = doc.select("meta[http-equiv=refresh]");
//
//                    for (Element metaTag : metaTags) {
//                        String content = metaTag.attr("content");
//                        Pattern pattern = Pattern.compile(".*URL='?(.*)$", Pattern.CASE_INSENSITIVE);
//                        Matcher m = pattern.matcher(content);
//                        String redirectUrl = m.matches() ? m.group(1) : null;
//                        if (redirectUrl != null)
//                            full_url = redirectUrl;
//
//
//                        TouchIconExtractor ex = new TouchIconExtractor();
//                        List<Icon> icons = ex.fromPage(full_url, true);
//
//                        Log.d("META", redirectUrl);
                    //}
                    Elements icons = doc.select("link[rel=icon]");
                    for (Element icon : icons)
                    {
                        Log.d("META", icon.attr("href"));
                    }
                    //CASE 1: META redirect
                    //CASE 2: HTML page with icon information
                    //CASE 3: Parse WebApp Manifest
                }
            }
        });
    }

private class FaviconURLTask extends AsyncTask<Void, Void, String>
{

        Icon getBestIcon(List<Icon> icons) {
            Icon best = icons.get(0);
            Icon best_apple = icons.get(0);
            String APPLE = "apple-touch-icon";

            for (Icon i : icons) {

                if (!best_apple.getRel().getValue().equals(APPLE))
                    best_apple = i; //Replace in case best_apple is non-apple due to wrong initialization.

                boolean isAppleIcon = i.getRel().getValue().equals(APPLE);
                boolean largerThanNonAppleBest = (!isAppleIcon && i.inferArea() > best.inferArea());
                boolean largerThanAppleBest = (isAppleIcon && i.inferArea() > best_apple.inferArea());

                if (largerThanNonAppleBest)
                    best = i;
                if (largerThanAppleBest)
                    best_apple = i;
            }

            if (best.inferSize().getWidth() < 96 && best_apple.inferSize().getWidth() > 96)
                return best_apple;

            return best;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
        @Override
        protected String doInBackground(Void... params) {
            try {
                htmlRequest(full_url);
            } catch (IOException e) {
                e.printStackTrace();
            }

            TouchIconExtractor ex = new TouchIconExtractor();
            List<Icon> icons = ex.fromPage(full_url, true);
            if (icons.isEmpty())
                return NO_ICON;

            Icon best = getBestIcon(icons);
            return best.getUrl();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loadFavicon(result);

        }
    }


    private Integer getWidthFromIcon(String size_string) {
        int x_index = size_string.indexOf("x");
        String width = size_string.substring(0, x_index);

        return Integer.parseInt(width);
    }
    private class Content extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... voids) {

            TreeMap<Integer, String> found_icons = new TreeMap<>();

            try {
                //Connect to the website
                Document doc = Jsoup.connect(full_url).followRedirects(true).get();
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

                Map.Entry<Integer, String> best_fit = found_icons.lastEntry();
                if (found_icons.isEmpty() || best_fit.getKey() < 96)
                    return null;
                else
                    return best_fit.getValue();


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
            if (result == null)
                showFailedMessage();
            else
                loadFavicon(result);
//            imageView.setImageBitmap(bitmap);
//            textView.setText(title);
//            progressDialog.dismiss();
        }
    }
}
