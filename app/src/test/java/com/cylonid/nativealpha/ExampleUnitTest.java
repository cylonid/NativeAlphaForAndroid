package com.cylonid.nativealpha;

import android.app.Instrumentation;
import android.os.AsyncTask;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void getWidthFromHTMLElementString() {
        assertEquals((Integer)192, Utility.getWidthFromIcon("192x192"));
    }


}

