package com.cylonid.nativealpha;

import com.cylonid.nativealpha.util.Utility;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class UtilUnitTests {

    @Test
    public void getWidthFromHTMLElementString() {
        assertEquals((Integer)192, Utility.getWidthFromIcon("192x192"));
    }

}

