package com.cylonid.nativealpha;

import android.view.View;
import android.webkit.WebView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UITests {
//    @Rule
//    public ActivityTestRule<Activity> activityTestRule = new ActivityTestRule<>(Activity.class, false, false);
    @Rule
    public CleanActivityTestRule<MainActivity> activityTestRule = new CleanActivityTestRule<>(MainActivity.class);

    @Test
    public void addWebsite() {
        onView(withId(R.id.websiteUrl)).perform(clearText(), typeText("github.com"));
        onView(withId(R.id.switchCreateShortcut)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        assertEquals(DataManager.getInstance().getWebApp(0).getBaseUrl(), "https://github.com");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        TestUtils.waitFor(2000);
    }

    @Test
    public void startWebView() {
        initSingleWebsite("https://twitter.com");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        onView(withId(R.id.adblockwebview)).check(matches(isDisplayed()));
    }

    @Test(expected = NoMatchingViewException.class)
    public void deleteWebsite() {
        initSingleWebsite("https://twitter.com");
        onView(allOf(withTagValue(is((Object) "btnDelete0")))).perform(click());
        TestUtils.alertDialogAccept();

        onView(allOf(withTagValue(is((Object) "btnDelete0")))).check(matches(not(isDisplayed()))); //Throws exception
    }

    @Test
    public void changeWebAppSettings() {
        initSingleWebsite("https://whatismybrowser.com/detect/are-cookies-enabled");
        onView(allOf(withTagValue(is((Object) "btnSettings0")))).perform(click());
        onView(withId(R.id.switchCookies)).perform(click());
        TestUtils.alertDialogAccept();
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        onWebView().withNoTimeout().withElement(findElement(Locator.ID, "detected_value")).check(webMatches(getText(), containsString("No")));
        TestUtils.waitFor(3000);
    }


    private void initSingleWebsite(final String base_url) {
        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DataManager.getInstance().addWebsite(new WebApp(base_url));
                activityTestRule.getActivity().addActiveWebAppsToUI();
            }
        });
        //Get rid of welcome message
        TestUtils.alertDialogDismiss();

    }


}
