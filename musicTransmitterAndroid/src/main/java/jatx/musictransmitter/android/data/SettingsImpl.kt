package jatx.musictransmitter.android.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import jatx.musictransmitter.android.domain.Settings
import java.io.File

const val PREFS_NAME = "MusicTransmitterPreferences"

const val KEY_MUSIC_DIR = "musicDirPath"
const val KEY_FILE_LIST = "fileList"
const val KEY_VOLUME = "volume"
const val KEY_IS_SHUFFLE = "isShuffle"
const val KEY_IS_LOCAL_MODE = "isLocalMode"

@SuppressLint("ApplySharedPref")
class SettingsImpl(
    context: Context
): Settings {
    private val sp = context.getSharedPreferences(PREFS_NAME, 0)

    override var currentMusicDirPath: String
        get() = sp.getString(KEY_MUSIC_DIR, null) ?:
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
        set(value) {
            val editor = sp.edit()
            editor.putString(KEY_MUSIC_DIR, value)
            editor.commit()
        }

    override var currentFileList: List<File>
        get() {
            val listStr = sp.getString(KEY_FILE_LIST, null) ?: ""
            return if (listStr.isEmpty())
                listOf()
            else
                listStr.split("\n").map { File(it) }
        }
        set(value) {
            val listStr = value.joinToString("\n")
            val editor = sp.edit()
            editor.putString(KEY_FILE_LIST, listStr)
            editor.commit()
        }

    override var volume: Int
        get() = sp.getInt(KEY_VOLUME, 100)
        set(value) {
            val editor = sp.edit()
            editor.putInt(KEY_VOLUME, value)
            editor.commit()
        }

    override var isShuffle: Boolean
        get() = sp.getBoolean(KEY_IS_SHUFFLE, false)
        set(value) {
            val editor = sp.edit()
            editor.putBoolean(KEY_IS_SHUFFLE, value)
            editor.commit()
        }

    override var isLocalMode: Boolean
        get() = sp.getBoolean(KEY_IS_LOCAL_MODE, false)
        set(value) {
            val editor = sp.edit()
            editor.putBoolean(KEY_IS_LOCAL_MODE, value)
            editor.commit()
        }
}