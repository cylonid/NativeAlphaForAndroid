package com.cylonid.nativealpha;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.DialogFragment;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.App;
import com.cylonid.nativealpha.util.Const;
import com.cylonid.nativealpha.util.Utility;
import com.cylonid.nativealpha.util.WebViewLauncher;
import com.google.android.material.snackbar.Snackbar;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;
import static com.cylonid.nativealpha.util.Const.CODE_OPEN_FILE;


public class ShortcutDialogFragment extends DialogFragment  {

    private WebApp webapp;
    private String base_url;
    private Bitmap bitmap;
    private ImageView uiFavicon;
    private CircularProgressBar uiProgressBar;
    private EditText uiTitle;
    private Thread faviconFetcherThread;

    public ShortcutDialogFragment() {}

    public static ShortcutDialogFragment newInstance(WebApp webapp) {
        ShortcutDialogFragment frag = new ShortcutDialogFragment();
        frag.webapp = webapp;
        frag.base_url = webapp.getBaseUrl();
        frag.bitmap = null;

        return frag;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faviconFetcherThread.isAlive()) {
            faviconFetcherThread.interrupt();
            Log.d("CLEANUP", "Stopped running faviconfetcher");
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_OPEN_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                if (bitmap != null)
                    applyNewBitmapToDialog();

            }
            catch(IOException e) {
                Utility.showToast(requireActivity(), getString(R.string.icon_not_found), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final View view = getLayoutInflater().inflate(R.layout.shortcut_dialog, null);

        final AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    addShortcutToHomeScreen(bitmap);
                    dismiss();

                })
                .setNegativeButton(android.R.string.cancel,  (dialog1, which) -> {
                    dismiss();
                })
                .create();

        uiTitle = view.findViewById(R.id.websiteTitle);
        uiFavicon = view.findViewById(R.id.favicon);
        uiProgressBar = view.findViewById(R.id.circularProgressBar);

        Button btnCustomIcon = view.findViewById(R.id.btnCustomIcon);
        btnCustomIcon.setOnClickListener(v -> {

            uiProgressBar.setVisibility(View.GONE);
            uiFavicon.setVisibility(View.VISIBLE);

            if (uiTitle.getText().toString().equals(""))
                setShortcutTitle(webapp.getTitle());

            Intent intent = new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select an icon"), CODE_OPEN_FILE);
            } catch (android.content.ActivityNotFoundException e) {
                Utility.showInfoSnackbar((AppCompatActivity) requireActivity(), getString(R.string.no_filemanager), Snackbar.LENGTH_LONG);
                e.printStackTrace();
            }
        });
        dialog.setOnShowListener(dialog12 -> startFaviconFetching());

        return dialog;
    }

    private Bitmap loadBitmap(String strUrl)  {
        Bitmap bitmap;
        try {
            URL url = new URL(strUrl);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            InputStream is = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null || bitmap.getWidth() < Const.FAVICON_MIN_WIDTH)
                return null;

        } catch (Exception e) {
            bitmap = null;
            e.printStackTrace();
        }
        return bitmap;
    }
    private TreeMap<Integer, String> buildIconMap() {
        TreeMap<Integer, String> found_icons = new TreeMap<>();
        if(base_url == null || base_url.equals("")) return found_icons;
        String host_part = base_url.replace("http://", "").replace("https://", "").replace("www.", "");

        //No suitable icon
        if (host_part.startsWith("facebook."))
            found_icons.put(325, "https://static.xx.fbcdn.net/rsrc.php/v3/y3/r/UrYT8B96uSq.png");

        if (host_part.startsWith("amazon."))
            found_icons.put(300, "https://upload.wikimedia.org/wikipedia/commons/d/de/Amazon_icon.png");

        if (host_part.startsWith("paypal."))
            found_icons.put(196, "https://www.paypalobjects.com/webstatic/icon/pp196.png");

        if (host_part.startsWith("google."))
            found_icons.put(240, "https://www.gstatic.com/images/branding/googleg/2x/googleg_standard_color_120dp.png");

        // Size doesn't fit
        if (host_part.startsWith("anchor.fm"))
            found_icons.put(Integer.MAX_VALUE, "https://d12xoj7p9moygp.cloudfront.net/favicon/apple-touch-icon-wave-152x152.png");

        //OEBB has a typo in its web manifest
        if (host_part.startsWith("oebb.at"))
            found_icons.put(Integer.MAX_VALUE, "https://www.oebb.at/.resources/pv-2017/themes/images/favicons/android-chrome-192x192.png");

        //Wrong path in PWA manifest
        if (host_part.startsWith("explosm.net"))
            found_icons.put(Integer.MAX_VALUE, "https://files.explosm.net/img/favicons/site/android-chrome-192x192.png");

        //Path in PWA manifest is HTTP
        if (host_part.startsWith("oe3.orf.at"))
            found_icons.put(Integer.MAX_VALUE, "https://tubestatic.orf.at/mojo/1_3/storyserver//tube/common/images/apple-icons/oe3.png");

        //Non-existing path
        if (host_part.startsWith("darfichrein.de"))
            found_icons.put(Integer.MAX_VALUE, "https://c.darfichrein.de/assets/img/logo1.png");

        return found_icons;
    }

    public String[] fetchWebappData() {
        String[] result = new String[] {null, null, null};
        TreeMap<Integer, String> found_icons = buildIconMap();

        try {
            //Connect to the website
            Document doc = Jsoup.connect(base_url).ignoreHttpErrors(true).userAgent(Const.DESKTOP_USER_AGENT).followRedirects(true).get();

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
                if (icons.size() < 3) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!found_icons.isEmpty()) {
            Map.Entry<Integer, String> best_fit = found_icons.lastEntry();
            result[Const.RESULT_IDX_FAVICON] = best_fit.getValue();

        }

        return result;
    }

    private void startFaviconFetching() {

        faviconFetcherThread = new Thread(() -> {
            String[] webappdata = fetchWebappData();
            bitmap = loadBitmap(webappdata[Const.RESULT_IDX_FAVICON]);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {

                    applyNewBitmapToDialog();
                    setShortcutTitle(webappdata[Const.RESULT_IDX_TITLE]);
                    applyNewBaseUrl(webappdata[Const.RESULT_IDX_NEW_BASEURL]);

                });
            }
        });
        faviconFetcherThread.start();

        new CountDownTimer(5000, 5000) {
            @Override
            public void onTick(long millisUntilFinished) { }
            public void onFinish() {
                faviconFetcherThread.interrupt();
            }
        }.start();

    }


    private void addShortcutToHomeScreen(Bitmap bitmap) {
        Intent intent = WebViewLauncher.createWebViewIntent(webapp, requireActivity());

        IconCompat icon;
        if (bitmap != null)
            icon = IconCompat.createWithBitmap(bitmap);
        else
            icon = IconCompat.createWithResource(requireActivity(), R.mipmap.native_alpha_shortcut);


        String final_title = uiTitle.getText().toString();
        if (final_title.equals(""))
            final_title = webapp.getTitle();
        if (webapp.getTitle().equals("")) {
            final_title = "Unknown";
        }

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireActivity())) {

            ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(requireActivity(), final_title)
                    .setIcon(icon)
                    .setShortLabel(final_title)
                    .setLongLabel(final_title)
                    .setIntent(intent)
                    .build();
            String newScId = pinShortcutInfo.getId();
            ShortcutManager scManager = App.getAppContext().getSystemService(ShortcutManager.class);
            if(!scManager.getPinnedShortcuts().stream().anyMatch(s -> s.getId().equals(newScId))) {
                ShortcutManagerCompat.requestPinShortcut(requireActivity(), pinShortcutInfo, null);
            } else {
                Utility.showToast(requireActivity(), getString(R.string.shortcut_already_exists));
            }
        }

    }

    private void prepareFailedUI() {
        showFailedMessage();
        if(webapp.getTitle() != null && !webapp.getTitle().equals("")) {
          uiTitle.setText(webapp.getTitle());
        }

        uiTitle.requestFocus();

        uiProgressBar.setVisibility(View.GONE);
        uiFavicon.setVisibility(View.VISIBLE);
    }
    private void showFailedMessage() {
        Utility.showToast(requireActivity(), getString(R.string.icon_fetch_failed_line1, webapp.getTitle()) + getString(R.string.icon_fetch_failed_line2) + getString(R.string.icon_fetch_failed_line3));
    }

    private void setShortcutTitle(String shortcut_title) {
        if (shortcut_title != null) {
            if (!shortcut_title.equals(""))
                uiTitle.setText(shortcut_title);

        }
        else {
            uiTitle.setText(webapp.getTitle());
        }
        uiTitle.requestFocus();
    }

    private void applyNewBaseUrl(String url) {
        if (url != null) {
            webapp.setBaseUrl(url);
            DataManager.getInstance().saveWebAppData();
        }

    }

    private void applyNewBitmapToDialog() {
        if (bitmap == null) {
            prepareFailedUI();
            return;
        }
        uiFavicon.setImageBitmap(bitmap);
        uiProgressBar.setVisibility(View.GONE);
        uiFavicon.setVisibility(View.VISIBLE);


    }

}
