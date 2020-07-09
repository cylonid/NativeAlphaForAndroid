package com.cylonid.nativealpha;

import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UITests {
    @Rule
    public CleanActivityTestRule<MainActivity> activityTestRule = new CleanActivityTestRule<>(MainActivity.class);
    @Test
    public void addWebsite() {
        onView(withId(R.id.websiteUrl)).perform(clearText(), typeText("github.com"));
        onView(withId(R.id.switchCreateShortcut)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        assertEquals(DataManager.getInstance().getWebApp(0).getBaseUrl(), "https://github.com");
        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
//        onView(isRoot()).perform(waitFor(5000));
    }

//    @Rule
//    public ActivityTestRule<Activity> activityTestRule = new ActivityTestRule<>(Activity.class, false, false);
    @Test
    public void startWebView() {

        initSingleWebsite("https://twitter.com");

        onView(allOf(withTagValue(is((Object) "btnOpenWebview0")), isDisplayed())).perform(click());
        onView(withId(R.id.adblockwebview)).check(matches(isDisplayed()));
        onView(isRoot()).perform(waitFor(2000));
    }

    @Test(expected = NoMatchingViewException.class)
    public void deleteWebsite() {
        initSingleWebsite("https://twitter.com");
        onView(allOf(withTagValue(is((Object) "btnDelete0")))).perform(click());
        alertDialogAccept();

        onView(allOf(withTagValue(is((Object) "btnDelete0")))).check(matches(not(isDisplayed()))); //Throws exception
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
        alertDialogDismiss();

    }

    private void alertDialogAccept() {
        onView(withId(android.R.id.button1)).perform(click());
    }
    private void alertDialogDismiss() {
        onView(withId(android.R.id.button2)).perform(click());
    }
    
//
//    public static boolean viewIsDisplayed(int viewId)
//    {
//        final boolean[] isDisplayed = {true};
//        onView(withId(viewId)).withFailureHandler(new FailureHandler()
//        {
//            @Override
//            public void handle(Throwable error, Matcher<View> viewMatcher)
//            {
//                isDisplayed[0] = false;
//            }
//        }).check(matches(isDisplayed()));
//        return isDisplayed[0];
//    }
//

    public static ViewAction waitFor(final long delay) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() {
                return ViewMatchers.isRoot();
            }

            @Override public String getDescription() {
                return "wait for " + delay + "milliseconds";
            }

            @Override public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(delay);
            }
        };
    }


}
