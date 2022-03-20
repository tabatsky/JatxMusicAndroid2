package jatx.musictransmitter.android

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun test1_getAllMusic() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        val sortOrder = MediaStore.Audio.Media.ARTIST + " || " +
                MediaStore.Audio.Media.ALBUM + " || " +
                MediaStore.Audio.Media.TITLE + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR
        )

        val cursor = instrumentationContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val artist = it.getString(1)
                val album = it.getString(2)
                val title = it.getString(3)
                val data = it.getString(4)
                val displayName = it.getString(5)
                val duration = it.getInt(6)
                val year = it.getString(7)
                Log.e("song", "$id || $artist || $album || $title || $data || $displayName || $duration || $year")
            }
        }
    }
}