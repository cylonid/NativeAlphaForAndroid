package com.cylonid.nativefier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.NotificationCompatSideChannelService;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.mm2d.touchicon.Icon;
import net.mm2d.touchicon.TouchIconExtractor;

import java.util.Arrays;
import java.util.List;

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

    public void fetchFaviconURL() {
        new FaviconURLTask().execute();

    }
    private void loadFavicon(String url) {
        Target target = new Target() {

            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                showFailedMessage();
                addShortcutToHomeScreen(d, null);
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                addShortcutToHomeScreen(d, bitmap);
            }

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
}
