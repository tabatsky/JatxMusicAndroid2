package jatx.musictransmitter.android.presentation

import android.util.Log
import jatx.debug.logError
import jatx.musictransmitter.android.data.TrackInfoStorage
import moxy.InjectViewState
import moxy.MvpPresenter
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.FlacTagWriter
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import java.io.File
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import javax.inject.Inject

@InjectViewState
class MusicEditorPresenter @Inject constructor(
    private val trackInfoStorage: TrackInfoStorage
) : MvpPresenter<MusicEditorView>() {
    lateinit var file: File
        private set
    private lateinit var artist: String
    private lateinit var album: String
    private lateinit var title: String
    private lateinit var year: String
    private lateinit var number: String

    private var isWin = false

    fun onPathParsed(path: String) {
        file = File(path)

        viewState.showFileName(file.name)
        openTags()
    }

    fun onSaveClick(needQuit: Boolean) {
        viewState.saveTags(needQuit)
    }

    fun onBackPressed(artist: String, album: String, title: String, year: String, number: String) {
        if (wasChanged(artist, album, title, year, number)) {
            viewState.showNeedToSaveDialog()
        } else {
            viewState.quit()
        }
    }

    fun onNeedQuit() {
        viewState.quit()
    }

    fun onSaveTags(artist: String, album: String, title: String, year: String, number: String) {
        try {
            this.artist = artist
            this.album = album
            this.title = title
            this.year = year
            this.number = number

            when (file.extension) {
                "mp3" -> {
                    saveMP3Tags()
                }
                "flac" -> {
                    saveFLACTags()
                }
            }
        } catch (e: Throwable) {
            logError(e)
            viewState.saveTagErrorToast()
        }
    }

    private fun saveMP3Tags() {
        val mp3f = MP3File(file)
        val tag = mp3f.createDefaultTag()

        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM_ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.YEAR, year)
        tag.setField(FieldKey.TRACK, correctNumber(number))
        tag.setField(FieldKey.COMMENT, "tag created with jatx music tag editor")

        mp3f.tag = tag
        mp3f.save(file)
    }

    private fun saveFLACTags() {
        val af = AudioFileIO.read(file)
        val tag = af.tagOrCreateDefault as FlacTag
        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM_ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.YEAR, year)
        tag.setField(FieldKey.TRACK, correctNumber(number))
        tag.setField(FieldKey.COMMENT, "tag created with jatx music tag editor")
        val raf = RandomAccessFile(file, "rw")
        FlacTagWriter().write(tag, raf, raf)
    }

    fun onWinToUtfClick(artist: String, album: String, title: String) {
        if (isWin) return

        try {
            val artistBytes = artist.toByteArray(charset("ISO-8859-1"))
            val albumBytes = album.toByteArray(charset("ISO-8859-1"))
            val titleBytes = title.toByteArray(charset("ISO-8859-1"))
            val artistWin = String(artistBytes, charset("WINDOWS-1251"))
            val albumWin = String(albumBytes, charset("WINDOWS-1251"))
            val titleWin = String(titleBytes, charset("WINDOWS-1251"))

            viewState.showTags(artistWin, albumWin, titleWin, year, number)
            isWin = true
        } catch (e: UnsupportedEncodingException) {
            Log.i("error", "unsupported encoding")
        }
    }

    private fun openTags() {
        try {
            val track = trackInfoStorage.getTrackFromFile(file)
            artist = track.artist
            album = track.album
            title = track.title
            year = track.year
            number = track.number

            viewState.showTags(artist, album, title, year, number)
        } catch (e: Throwable) {
            logError(e)
        }
    }

    private fun correctNumber(num: String): String? {
        return try {
            num.toInt()
            num
        } catch (e: NumberFormatException) {
            "0"
        }
    }

    private fun wasChanged(artist: String, album: String, title: String, year: String, number: String): Boolean {
        val noChanges =
            (this.artist == artist)
                .and(this.album == album)
                .and(this.title == title)
                .and(this.year == year)
                .and(this.number == number)
        return !noChanges
    }
}