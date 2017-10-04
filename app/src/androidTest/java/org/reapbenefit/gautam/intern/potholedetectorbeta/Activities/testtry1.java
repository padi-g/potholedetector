package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class testtry1 {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void testtry1() {
        ViewInteraction px = onView(
                allOf(withText("Sign In"),
                        withParent(withId(R.id.start_sign_in)),
                        isDisplayed()));
        px.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        withParent(withId(R.id.toolbar)),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Yes")));
        appCompatButton.perform(scrollTo(), click());

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.stopSwitch),
                        withParent(withId(R.id.toolbar)),
                        isDisplayed()));
        switch_.perform(click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.restart), withText("Exit"),
                        withParent(allOf(withId(R.id.easyframe),
                                withParent(withId(R.id.pager)))),
                        isDisplayed()));
        appCompatButton2.perform(click());

    }

}
