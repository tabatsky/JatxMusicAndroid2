package jatx.musictransmitter.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.core.internal.deps.guava.base.Predicate
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnitRunner
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, TestApp::class.java.name, context)
    }
}

@RunWith(AndroidJUnit4::class)
@LargeTest
class UITest {
    @get:Rule
    var activityScenarioRule = activityScenarioRule<MusicTransmitterActivity>()

    @get:Rule
    var permissionNotifications: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule
    var permissionRead: GrantPermissionRule = Build.VERSION.SDK_INT
        .takeIf { it <= 32 }
        ?.let {
            GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        } ?: GrantPermissionRule.grant()

    @get:Rule
    var permissionReadMediaAudio: GrantPermissionRule = Build.VERSION.SDK_INT
        .takeIf { it >= 33 }
        ?.let {
            GrantPermissionRule.grant(android.Manifest.permission.READ_MEDIA_AUDIO)
        } ?: GrantPermissionRule.grant()

    @get:Rule
    var permissionWrite: GrantPermissionRule = Build.VERSION.SDK_INT
        .takeIf { it <= 29 }
        ?.let {
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } ?: GrantPermissionRule.grant()

    @get:Rule
    var permissionPhoneState: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    @Test
    fun test001_artistIsAddedFromMenuCorrectly() {
        onView(withText("REMOVE")).perform(click())
        onView(withText("Remove All")).perform(click())
        onView(withText("ADD")).perform(click())
        onView(withText("Add Artist")).perform(click())
        onView(withText("Artist 2")).perform(click())
        onView(withText("Title 1 1")).check(matches(isDisplayed()))
        onView(withText("Title 1 3")).check(matches(isDisplayed()))
        onView(isRoot()).check(matches(withViewCountAtLeast(withText("Artist 2 | 1:37"), 4)))
        onView(isRoot()).perform(waitFor(3000))
    }
}

fun waitFor(delay: Long): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription(): String = "wait for $delay milliseconds"
        override fun perform(uiController: UiController, v: View?) {
            uiController.loopMainThreadForAtLeast(delay)
        }
    }
}

fun withViewCountAtLeast(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?> {
    return object : TypeSafeMatcher<View?>() {
        var actualCount = -1
        override fun describeTo(description: Description) {
            if (actualCount >= 0) {
                description.appendText("With expected number of items at least: $expectedCount")
                description.appendText("\n With matcher: ")
                viewMatcher.describeTo(description)
                description.appendText("\n But got: $actualCount")
            }
        }

        override fun matchesSafely(root: View?): Boolean {
            actualCount = 0
            val iterable = TreeIterables.breadthFirstViewTraversal(root)
            actualCount =
                Iterables.filter(iterable, withMatcherPredicate(viewMatcher)).count()
            return actualCount >= expectedCount
        }
    }
}

private fun withMatcherPredicate(matcher: Matcher<View>): Predicate<View?> {
    return Predicate<View?> { view -> matcher.matches(view) }
}