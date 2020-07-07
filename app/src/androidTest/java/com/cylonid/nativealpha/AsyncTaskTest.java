package com.cylonid.nativealpha;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import androidx.test.rule.ActivityTestRule;


import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class AsyncTaskTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);
    Context context;

    @Test
    public void testVerifyJoke() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        WebApp webapp = new WebApp("mkassling.at");
        ShortcutHelper.FaviconURLFetcher f = new ShortcutHelper.FaviconURLFetcher(new ShortcutHelper(webapp, mActivityTestRule.getActivity())) {
            protected void onPreExec() {
                super.onPreExec();
            }
            @Override
            protected void onPostExecute(String result) {
//                assertNotNull(result);

                latch.countDown();

            }
        };
        f.execute();
        latch.await();
    }
}