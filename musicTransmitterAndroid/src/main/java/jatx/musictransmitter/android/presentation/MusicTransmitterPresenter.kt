package jatx.musictransmitter.android.presentation

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import jatx.musictransmitter.android.data.Settings
import jatx.musictransmitter.android.data.TrackInfoStorage
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.service.MusicTransmitterService
import jatx.musictransmitter.android.util.findFiles
import moxy.InjectViewState
import moxy.MvpPresenter
import java.io.File
import javax.inject.Inject

@InjectViewState
class MusicTransmitterPresenter @Inject constructor(
    private val context: Context,
    private val settings: Settings,
    private val trackInfoStorage: TrackInfoStorage
): MvpPresenter<MusicTransmitterView>() {

    private val files = arrayListOf<File>()
    private var currentPosition = -1
    private var tracks = listOf<Track>()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        trackInfoStorage.setOnUpdateTrackListListener { tracks ->
            this.tracks = tracks
            viewState.showTracks(tracks, currentPosition)
        }

        if (!MusicTransmitterService.isInstanceRunning) {
            val intent = Intent(context, MusicTransmitterService.javaClass)
            val filePathArray = files.map { it.absolutePath }.toTypedArray()
            intent.putExtra("filePathArray", filePathArray)
            context.startService(intent)
        }
    }

    fun onPlayClick() {
        viewState.showPlayingState(true)
    }

    fun onPauseClick() {
        viewState.showPlayingState(false)
    }
    
    fun onRepeatClick() {
        viewState.showShuffleState(true)
    }

    fun onShuffleClick() {
        viewState.showShuffleState(false)
    }

    fun onRevClick() {

    }

    fun onFwdClick() {

    }

    fun onVolumeUpClick() {

    }

    fun onVolumeDownClick() {

    }

    fun onAddTrackSelected() {
        viewState.showOpenTrackDialog(settings.currentMusicDirPath)
    }

    fun onAddFolderSelected() {
        viewState.showOpenFolderDialog(settings.currentMusicDirPath)
    }

    fun onTrackOpened(path: String) {
        val file = File(path)
        files.add(file)
        settings.currentMusicDirPath = file.parentFile.absolutePath
        trackInfoStorage.files = files
    }

    fun onFolderOpened(path: String) {
        files.addAll(findFiles(path, ".*\\.mp3$"))
        settings.currentMusicDirPath = path
        trackInfoStorage.files = files
    }

    fun onRemoveAllTracks() {
        files.clear()
        trackInfoStorage.files = files
    }

    fun onTrackClick(position: Int) {
        currentPosition = position
        viewState.showTracks(tracks, currentPosition)
    }
}