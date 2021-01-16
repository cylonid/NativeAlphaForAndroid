package com.cylonid.nativealpha;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
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
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);
//    public ActivityScenarioRule<MainActivity> scenarioRule = new ActivityScenarioRule<>(MainActivity.class);
    @Test
    public void addWebsite() {
        onView(withId(R.id.websiteUrl)).perform(clearText(), typeText("github.com"));
        onView(withId(R.id.switchCreateShortcut)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        assertEquals(DataManager.getInstance().getWebApp(0).getBaseUrl(), "https://github.com");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
    }

    @Test
    public void startWebView() {
        initSingleWebsite("https://twitter.com");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        onView(withId(R.id.webview)).check(matches(isDisplayed()));
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
        onView(withId(R.id.btnSave)).perform(click());
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        onWebView(Matchers.allOf(withId(R.id.webview))).withNoTimeout().withElement(findElement(Locator.ID, "detected_value")).check(webMatches(getText(), containsString("No")));

    }
    @Test
    public void badSSLAccept() {
        initSingleWebsite("https://untrusted-root.badssl.com/");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        TestUtils.waitForElementWithText(R.string.load_anyway);
        onView(withText(R.string.load_anyway)).perform(click());
        onWebView(Matchers.allOf(withId(R.id.webview))).withNoTimeout().withElement(findElement(Locator.ID, "content")).check(webMatches(getText(), containsString("untrusted-root")));
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void badSSLDismiss() {
        initSingleWebsite("https://untrusted-root.badssl.com/");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        TestUtils.waitForElementWithText(android.R.string.cancel);
        onView(withText(android.R.string.cancel)).perform(click());
        onWebView(Matchers.allOf(withId(R.id.webview))).withTimeout(3, TimeUnit.SECONDS).withElement(findElement(Locator.ID, "content")).check(webMatches(getText(), containsString("untrusted-root")));

    }

    @Test
    public void openHTTPSite() {
        initSingleWebsite("http://annozone.de");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        TestUtils.waitForElementWithText(android.R.string.cancel);
        onView(withId(android.R.id.button2)).perform(scrollTo()).perform(click());
//        onView(isRoot()).perform(ViewActions.pressBack());

        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        TestUtils.waitForElementWithText(android.R.string.cancel);
        onView(withId(android.R.id.button1)).perform(scrollTo()).perform(click());
        onWebView(Matchers.allOf(withId(R.id.webview))).withTimeout(6, TimeUnit.SECONDS).check(webMatches(getCurrentUrl(), containsString("annozone")));
    }


    @Test
    public void changeUIModes() {
        String[] ui_modes = activityTestRule.getActivity().getResources().getStringArray(R.array.ui_modes);
        TestUtils.alertDialogDismiss();

        //Open settings, set to dark mode and cancel
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_settings)).perform(click());
        onView(withId(R.id.dropDownTheme)).perform(click());
        onView(withText(ui_modes[2])).perform(click());
        assertEquals(AppCompatDelegate.getDefaultNightMode(), AppCompatDelegate.MODE_NIGHT_YES);
        onView(withId(R.id.btnCancel)).perform(click());

        //Check that default mode is restored, change to light mode and check light mode
        assertEquals(AppCompatDelegate.getDefaultNightMode(), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_settings)).perform(click());
        onView(withId(R.id.dropDownTheme)).perform(click());
        onView(withText(ui_modes[1])).perform(click());
        onView(withId(R.id.btnSave)).perform(click());
        assertEquals(AppCompatDelegate.getDefaultNightMode(), AppCompatDelegate.MODE_NIGHT_NO);

    }


    private void initSingleWebsite(final String base_url) {
        activityTestRule.getActivity().runOnUiThread(() -> {
            DataManager.getInstance().addWebsite(new WebApp(base_url, DataManager.getInstance().getIncrementedID()));
            activityTestRule.getActivity().addActiveWebAppsToUI();
        });
        //Get rid of welcome message
        TestUtils.alertDialogDismiss();
    }

    private void initMultipleWebsites(final List<String> urls) {
        activityTestRule.getActivity().runOnUiThread(() -> {
            for (String base_url : urls) {
                DataManager.getInstance().addWebsite(new WebApp(base_url, DataManager.getInstance().getIncrementedID()));
            }
            activityTestRule.getActivity().addActiveWebAppsToUI();
        });
        //Get rid of welcome message
        TestUtils.alertDialogDismiss();
    }



}
