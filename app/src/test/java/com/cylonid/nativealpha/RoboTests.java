package com.cylonid.nativealpha;

import org.apache.tools.ant.Main;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class RoboTests {

    public void fetchFavicon(String base_url, final String expected, final boolean expect_null) {
        final MainActivity activity = Robolectric.buildActivity(MainActivity.class )
                .create()
                .resume()
                .get();


        WebApp webapp = new WebApp(base_url);
        ShortcutHelper.FaviconURLFetcher f = new ShortcutHelper.FaviconURLFetcher(new ShortcutHelper(webapp, activity)) {

            @Override
            protected void onPostExecute(String[] result) {
                if (expect_null)
                    assertNull(result[0]);
                else
                assertTrue(result[0].equals(expected));
            }
        };
        f.execute();
    }

    @Test
    public void faviconFromWebManifest() {
        fetchFavicon("https://twitter.com", "https://abs.twimg.com/responsive-web/web/icon-default-maskable-large.433070b4.png", false);
    }

    @Test
    public void faviconWithoutManifest() {
        fetchFavicon("https://orf.at", "https://orf.at/mojo/1_4_1/storyserver//common/images/favicons/favicon-128x128.png", false);
    }

    @Test
    public void faviconNull() {
        fetchFavicon("https://tugraz.at", null, true);
    }
}