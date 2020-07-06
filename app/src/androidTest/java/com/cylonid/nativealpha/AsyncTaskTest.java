//package com.cylonid.nativealpha;
//
//import android.content.Context;
//
//import androidx.test.platform.app.InstrumentationRegistry;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.concurrent.CountDownLatch;
//
//import static org.junit.Assert.*;
//
//@RunWith(AndroidJUnit4.class)
//public class AsyncTaskTest {
//
//    Context context;
//
//    @Test
//    public void testVerifyJoke() throws InterruptedException {
//        assertTrue(true);
//        final CountDownLatch latch = new CountDownLatch(1);
//        context = InstrumentationRegistry.getInstrumentation().getContext();
//        ShortcutHelper.FaviconURLFetcher f = new ShortcutHelper.FaviconURLFetcher() {
//            @Override
//            protected void onPostExecute(String result) {
//                assertNotNull(result);
//                if (result != null){
//                    assertTrue(result.length() > 0);
//                    latch.countDown();
//                }
//            }
//        };
//        f.execute
//        latch.await();
//    }