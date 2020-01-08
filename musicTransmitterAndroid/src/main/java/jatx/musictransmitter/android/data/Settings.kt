package jatx.musictransmitter.android.data

import android.content.Context
import android.os.Environment

const val PREFS_NAME = "MusicTransmitterPreferences"

const val KEY_MUSIC_DIR = "musicDirPath"

class Settings(
    context: Context
) {
    private val sp = context.getSharedPreferences(PREFS_NAME, 0)

    var currentMusicDirPath: String
        get() = sp.getString(KEY_MUSIC_DIR, null) ?: Environment.getExternalStorageDirectory().absolutePath
        set(value) {
            val editor = sp.edit()
            editor.putString(KEY_MUSIC_DIR, value)
            editor.commit()
        }
}