package jatx.musictransmitter.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.core.internal.deps.guava.base.Predicate
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnitRunner
import jatx.musictransmitter.android.data.commonTrackLength
import jatx.musictransmitter.android.data.makeTrackEntry
import jatx.musictransmitter.android.di.DaggerTestAppComponent
import jatx.musictransmitter.android.di.TestDeps
import jatx.musictransmitter.android.media.AlbumEntry
import jatx.musictransmitter.android.media.ArtistEntry
import jatx.musictransmitter.android.media.TrackEntry
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, TestApp::class.java.name, context)
    }
}

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UITest {
    private val appComponent = DaggerTestAppComponent
        .builder()
        .context(InstrumentationRegistry.getInstrumentation().targetContext)
        .build()

    private val testDeps = TestDeps().also {
        appComponent.injectTestDeps(it)
    }

    private val stringConst =
        StringConst(InstrumentationRegistry.getInstrumentation().targetContext)

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
        val artistEntry = testDeps.contentStorage.getArtistEntries()[1] as ArtistEntry
        val tracks = testDeps.contentStorage.getFilesByEntry(artistEntry).map {
            testDeps.trackInfoStorage.getTrackFromFile(it)
        }

        onView(withText(stringConst.itemRemove)).perform(click())
        onView(withText(stringConst.itemRemoveAll)).perform(click())
        onView(withText(stringConst.itemAdd)).perform(click())
        onView(withText(stringConst.itemAddArtist)).perform(click())
        onView(withText(artistEntry.artist)).perform(click())
        onView(withText(tracks[0].title)).check(matches(isDisplayed()))
        onView(withText(tracks[1].title)).check(matches(isDisplayed()))
        onView(withText(tracks[2].title)).check(matches(isDisplayed()))
        val artistWithCommonTrackLength = "${artistEntry.artist} | $commonTrackLength"
        onView(isRoot()).check(
            matches(withViewCountAtLeast(withText(artistWithCommonTrackLength), 4)))
        onView(isRoot()).perform(waitFor(3000))
    }

    @Test
    fun test002_albumIsAddedFromMenuCorrectly() {
        val albumEntry = testDeps.contentStorage.getAlbumEntries()[1] as AlbumEntry
        val tracks = testDeps.contentStorage.getFilesByEntry(albumEntry).map {
            testDeps.trackInfoStorage.getTrackFromFile(it)
        }

        onView(withText(stringConst.itemRemove)).perform(click())
        onView(withText(stringConst.itemRemoveAll)).perform(click())
        onView(withText(stringConst.itemAdd)).perform(click())
        onView(withText(stringConst.itemAddAlbum)).perform(click())
        val albumWithArtist = "${albumEntry.album} (${albumEntry.artist})"
        onView(withText(albumWithArtist)).perform(click())
        onView(withText(tracks[0].title)).check(matches(isDisplayed()))
        onView(withText(tracks[1].title)).check(matches(isDisplayed()))
        onView(withText(tracks[2].title)).check(matches(isDisplayed()))
        val artistWithCommonTrackLength = "${albumEntry.artist} | $commonTrackLength"
        onView(isRoot()).check(
            matches(withViewCountAtLeast(withText(artistWithCommonTrackLength), 4)))
        onView(isRoot()).perform(waitFor(3000))
    }

    @Test
    fun test003_threeTracksAreAddedFromMenuCorrectly() {
        val trackEntries = testDeps.contentStorage.getTrackEntries().map { it as TrackEntry }
        val tracks = trackEntries.map { trackEntry ->
            testDeps.contentStorage.getFilesByEntry(trackEntry)[0].let {
                testDeps.trackInfoStorage.getTrackFromFile(it)
            }
        }

        onView(withText(stringConst.itemRemove)).perform(click())
        onView(withText(stringConst.itemRemoveAll)).perform(click())

        listOf(1, 2, 3).forEach { id ->
            onView(withText(stringConst.itemAdd)).perform(click())
            onView(withText(stringConst.itemAddTrack)).perform(click())
            val trackEntry = makeTrackEntry(id, id, id)
            val index = trackEntries.indexOf(trackEntry)
            onView(withId(R.id.musicSelectorRV)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(
                index
            ))
            val track = tracks[index]
            onView(withText(trackEntry.asString)).perform(click())
            onView(withText(track.title)).check(matches(isDisplayed()))
            val artistWithCommonTrackLength = "${track.artist} | $commonTrackLength"
            onView(isRoot()).perform(waitFor(3000))
            onView(withText(artistWithCommonTrackLength)).check(matches(isDisplayed()))
        }

        onView(isRoot()).perform(waitFor(3000))
    }
}

class StringConst(context: Context) {
    val itemAdd = context.getString(R.string.item_add)
    val itemAddArtist = context.getString(R.string.item_add_artist)
    val itemAddAlbum = context.getString(R.string.item_add_album)
    val itemAddTrack = context.getString(R.string.item_add_track)
    val itemRemove = context.getString(R.string.item_remove)
    val itemRemoveAll = context.getString(R.string.item_remove_all)
}

fun waitFor(delay: Long): ViewAction {
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