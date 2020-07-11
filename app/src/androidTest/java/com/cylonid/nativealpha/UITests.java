package com.cylonid.nativealpha;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cylonid.nativealpha.model.DataManager;
import com.cylonid.nativealpha.model.WebApp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
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
