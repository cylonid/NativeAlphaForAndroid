package com.cylonid.nativealpha;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class RoboTests {

    public void testShortcutHelper(String base_url, final String expected, final int result_index) {
//        final MainActivity activity = Robolectric.buildActivity(MainActivity.class )
//                .create()
//                .resume()
//                .get();

        WebApp webapp = new WebApp(base_url);
        ShortcutHelper.FaviconURLFetcher f = new ShortcutHelper.FaviconURLFetcher(new ShortcutHelper(webapp, null)) {

            @Override
            protected void onPreExecute() {
            }
            @Override
            protected void onPostExecute(String[] result) {
                    assertEquals(result[result_index], expected);
            }
        };
        f.execute();
    }

    @Test
    public void faviconFromWebManifest() {
        testShortcutHelper("https://twitter.com", "https://abs.twimg.com/responsive-web/web/icon-default-maskable-large.433070b4.png", Const.RESULT_IDX_FAVICON);
    }

    @Test
    public void faviconWithoutManifest() {
        testShortcutHelper("https://orf.at", "https://orf.at/mojo/1_4_1/storyserver//common/images/favicons/favicon-128x128.png", Const.RESULT_IDX_FAVICON);
    }

    @Test
    public void faviconNull() {
        testShortcutHelper("https://tugraz.at", null, Const.RESULT_IDX_FAVICON);
    }

    @Test
    public void getStartUrlFromWebManifest() {
        testShortcutHelper("https://online.tugraz.at", "https://online.tugraz.at/tug_online/ee/ui/ca2/app/desktop/#/login", Const.RESULT_IDX_NEW_BASEURL);
    }

    @Test
    public void getWebAppTitleFromManifest() {
        testShortcutHelper("https://online.tugraz.at", "TUGRAZonline Go", Const.RESULT_IDX_TITLE);
    }


}