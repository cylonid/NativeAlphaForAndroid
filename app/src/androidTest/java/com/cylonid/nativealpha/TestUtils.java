package com.cylonid.nativealpha;

import android.app.Activity;

import androidx.annotation.CheckResult;
import androidx.annotation.IdRes;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;

import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.any;

public class TestUtils {



    public static void alertDialogAccept() {
        onView(withId(android.R.id.button1)).perform(click());
    }
    public static void alertDialogDismiss() {
        onView(withId(android.R.id.button2)).perform(click());
    }

    public static boolean viewIsDisplayed(int viewId)
    {
        final boolean[] isDisplayed = {true};
        onView(withId(viewId)).withFailureHandler((error, viewMatcher) -> isDisplayed[0] = false).check(matches(isDisplayed()));
        return isDisplayed[0];
    }


    public static void waitFor(final long ms) {
        final CountDownLatch signal = new CountDownLatch(1);

        try {
            signal.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    public static void waitForElementWithText(@IdRes int stringId) {

        ViewInteraction element;
        do {
            waitFor(500);

            //simple example using withText Matcher.
            element = onView(withText(stringId));

        } while (!MatcherExtension.exists(element));

    }

    public static Activity getCurrentActivity() {
        final Activity[] activity = new Activity[1];
        onView(isRoot()).check((view, noViewFoundException) -> activity[0] = (Activity) view.getContext());
        return activity[0];
    }
    private static class MatcherExtension {
        @CheckResult
        public static boolean exists(ViewInteraction interaction) {
            try {
                interaction.perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return any(View.class);
                    }

                    @Override
                    public String getDescription() {
                        return "check for existence";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        // no op, if this is run, then the execution will continue after .perform(...)
                    }
                });
                return true;
            } catch (AmbiguousViewMatcherException ex) {
                // if there's any interaction later with the same matcher, that'll fail anyway
                return true; // we found more than one
            } catch (NoMatchingViewException | NoMatchingRootException ex) {
                return false;
            } // optional depending on what you think "exists" means

        }

    }
}
