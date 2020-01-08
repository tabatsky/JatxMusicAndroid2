package jatx.musictransmitter.android.data

import android.util.Log
import jatx.musictransmitter.android.db.dao.TrackDao
import jatx.musictransmitter.android.db.entity.MIC_PATH
import jatx.musictransmitter.android.db.entity.Track
import java.io.File

class TrackInfoStorage(
    private val trackDao: TrackDao
) {
    private var onUpdateTrackListListener: OnUpdateTrackListListener? = null

    @Volatile
    var files = listOf<File>()
        set(value) {
            pauseFlag = true
            synchronized(lock) {
                lock.wait()
            }
            counter = 0
            field = value
            tracks.clear()
            resetFlag = true
        }
    private val tracks = arrayListOf<Track>()

    @Volatile
    private var counter = 0
    @Volatile
    private var resetFlag = false
    @Volatile
    private var pauseFlag = false
    private var lock = Object()

    val tagWorker = object : Thread() {
        override fun run() {
            try {
                var current = -1

                while (true) {
                    synchronized(lock) {
                        lock.notifyAll()
                    }
                    sleep(5)
                    if (resetFlag) {
                        current = -1
                        pauseFlag = false
                        resetFlag = false
                    }
                    if (pauseFlag) continue
                    current++
                    if (files.size <= current) {
                        current = -1
                        pauseFlag = true
                        resetFlag = false
                        onUpdateTrackListListener?.onUpdateTrackList(tracks)
                        continue
                    }
                    if (counter % 20 == 0) {
                        onUpdateTrackListListener?.onUpdateTrackList(tracks)
                        counter++
                    }
                    tracks.add(getTrackFromFile(files[current]))
                }
            } catch (e: InterruptedException) {
                Log.e("tag worker", "interrupted")
            }
        }
    }.apply { this.start() }

    fun getMicTrack(): Track {
        val track =
            Track(MIC_PATH)
        track.artist = "Microphone"
        track.title = "Microphone"
        track.album = "Microphone"
        track.year = "1970"
        track.number = "0"
        track.length = "00:00"
        return track
    }

    fun getTrackFromFile(file: File): Track {
        if (file.absolutePath == MIC_PATH)
            return getMicTrack()

        if (!file.exists())
            return Track(file.absolutePath, artist = "Error", title = "File does not exist")

        val path = file.absolutePath
        val lastModified = file.lastModified()

        val trackFromDB = trackDao.getTrack(path)?.apply {
            counter++
        }

        return if (lastModified > trackFromDB?.lastModified ?: 0) {
            Track(path).apply {
                this.lastModified = lastModified
                this.tryToFill(file)
                trackDao.putTrack(this)
                counter++
            }
        } else {
            trackFromDB!!
        }
    }

    fun setOnUpdateTrackListListener(onUpdate: (tracks: List<Track>) -> Unit) {
        onUpdateTrackListListener = object : OnUpdateTrackListListener {
            override fun onUpdateTrackList(tracks: List<Track>) {
                onUpdate(tracks)
            }
        }
    }
}

interface OnUpdateTrackListListener {
    fun onUpdateTrackList(tracks: List<Track>)
}
