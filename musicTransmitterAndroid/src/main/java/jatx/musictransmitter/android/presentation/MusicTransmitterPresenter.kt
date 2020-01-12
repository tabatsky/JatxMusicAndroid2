package jatx.musictransmitter.android.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.text.format.Formatter
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.data.Settings
import jatx.musictransmitter.android.data.TrackInfoStorage
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.services.*
import jatx.musictransmitter.android.ui.*
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

    private val shuffledList = arrayListOf<Int>()
    private var isShuffle = false

    private val realPosition: Int
        get() = if (isShuffle)
                    shuffledList[currentPosition] % files.size
                else
                    currentPosition

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        for (i in 0 until 10000) shuffledList.add(i)
        shuffledList.shuffle()

        trackInfoStorage.setOnUpdateTrackListListener { tracks ->
            this.tracks = tracks
            viewState.showTracks(tracks, currentPosition)
        }

        files.addAll(settings.currentFileList)
        trackInfoStorage.files = files

        initBroadcastReceivers()

        startService()
    }

    override fun onDestroy() {
        MusicTransmitterNotification.hideNotification(context)

        val intent = Intent(STOP_SERVICE)
        context.sendBroadcast(intent)
    }

    fun onBackPressed() = viewState.close()

    fun onPlayClick() {
        if (files.isEmpty()) {
            return
        } else if (currentPosition == -1) {
            currentPosition = 0
            viewState.showTracks(tracks, realPosition)
            tpSetPosition(realPosition)
            viewState.scrollToPosition(realPosition)
        }

        viewState.showPlayingState(true)
        tpAndTcPlay()

        val track = tracks[realPosition]
        MusicTransmitterNotification.showNotification(context, track.artist, track.title, true)
    }

    fun onPauseClick() {
        viewState.showPlayingState(false)
        tpAndTcPause()

        val track = tracks[realPosition]
        MusicTransmitterNotification.showNotification(context, track.artist, track.title, false)
    }
    
    fun onRepeatClick() {
        isShuffle = true
        viewState.showShuffleState(true)
    }

    fun onShuffleClick() {
        isShuffle = false
        viewState.showShuffleState(false)
    }

    fun onRevClick() {
        if (files.isEmpty()) return

        currentPosition = if (currentPosition > 0)
            currentPosition - 1
        else
            files.size - 1

        onPlayClick()
        tpSetPosition(realPosition)
        viewState.showTracks(tracks, realPosition)
        viewState.scrollToPosition(realPosition)
    }

    fun onFwdClick() {
        if (files.isEmpty()) return

        currentPosition = (currentPosition + 1) % files.size

        onPlayClick()
        tpSetPosition(realPosition)
        viewState.showTracks(tracks, realPosition)
        viewState.scrollToPosition(realPosition)
    }

    fun onVolumeUpClick() {
        settings.volume = if (settings.volume < 100) settings.volume + 5 else settings.volume
        tcSetVolume(settings.volume)
        viewState.showVolume(settings.volume)
    }

    fun onVolumeDownClick() {
        settings.volume = if (settings.volume > 0) settings.volume - 5 else settings.volume
        tcSetVolume(settings.volume)
        viewState.showVolume(settings.volume)
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
        updateTpFiles()
        trackInfoStorage.files = files
    }

    fun onFolderOpened(path: String) {
        files.addAll(findFiles(path, ".*\\.mp3$"))
        settings.currentMusicDirPath = path
        updateTpFiles()
        trackInfoStorage.files = files
    }

    fun onRemoveAllTracksSelected() {
        files.clear()
        updateTpFiles()
        trackInfoStorage.files = files
    }

    fun onRemoveTrackSelected() {
        viewState.showRemoveTrackMessage()
    }

    fun onTrackClick(position: Int) {
        currentPosition = position
        viewState.showTracks(tracks, currentPosition)
        viewState.scrollToPosition(currentPosition)
        onPlayClick()
        tpSetPosition(position)
    }

    fun onTrackLongClick(position: Int) {
        viewState.showTrackLongClickDialog(position)
    }

    fun onDeleteTrack(position: Int) {
        files.removeAt(position)
        if (position < currentPosition) {
            currentPosition -= 1
        } else if (position == currentPosition) {
            currentPosition = -1
        }
        updateTpFiles()
        trackInfoStorage.files = files
    }

    fun onOpenTagEditor(position: Int) {
        TODO("implement tag editor")
    }

    fun onProgressChanged(progress: Double) {
        tpSeek(progress)
    }

    fun onAddMicSelected() {
        viewState.tryAddMic()
    }

    fun onAddMicPermissionsAccepted() {
        files.add(File(MIC_PATH))
        updateTpFiles()
        trackInfoStorage.files = files
    }

    fun onShowIPSelected() {
        val wifiMgr = context.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        val ipAddress =
            if (ip > 0) Formatter.formatIpAddress(ip)
            else
                context.getString(R.string.not_detected_message)
        viewState.showIPAddress(ipAddress)
    }

    private fun startService() {
        val intent = Intent(context, MusicTransmitterService::class.java)
        context.startService(intent)
    }

    private fun initBroadcastReceivers() {
        val setCurrentTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val currentMs = intent.getFloatExtra(KEY_CURRENT_MS, 0f)
                val trackLengthMs = intent.getFloatExtra(KEY_TRACK_LENGTH_MS, 0f)
                viewState.showCurrentTime(currentMs, trackLengthMs)
            }
        }
        context.registerReceiver(setCurrentTimeReceiver, IntentFilter(SET_CURRENT_TIME))

        val setWifiStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getBooleanExtra(EXTRA_WIFI_STATUS, false)
                viewState.showWifiStatus(status)
            }
        }
        context.registerReceiver(setWifiStatusReceiver, IntentFilter(SET_WIFI_STATUS))

        val nextTrackReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onFwdClick()
            }
        }
        context.registerReceiver(nextTrackReceiver, IntentFilter(NEXT_TRACK))
        context.registerReceiver(nextTrackReceiver, IntentFilter(CLICK_FWD))

        val prevTrackReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onRevClick()
            }
        }
        context.registerReceiver(prevTrackReceiver, IntentFilter(CLICK_REV))

        val clickPlayReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPlayClick()
            }
        }
        context.registerReceiver(clickPlayReceiver, IntentFilter(CLICK_PLAY))

        val clickPauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPauseClick()
            }
        }
        context.registerReceiver(clickPauseReceiver, IntentFilter(CLICK_PAUSE))
    }

    private fun updateTpFiles() {
        settings.currentFileList = files
        context.sendBroadcast(Intent(TP_SET_FILE_LIST))
    }

    private fun tpSetPosition(position: Int) {
        val intent = Intent(TP_SET_POSITION)
        intent.putExtra(KEY_POSITION, position)
        context.sendBroadcast(intent)
    }

    private fun tpAndTcPlay() {
        val intent = Intent(TP_AND_TC_PLAY)
        context.sendBroadcast(intent)
    }

    private fun tpAndTcPause() {
        val intent = Intent(TP_AND_TC_PAUSE)
        context.sendBroadcast(intent)
    }

    private fun tpSeek(progress: Double) {
        val intent = Intent(TP_SEEK)
        intent.putExtra(KEY_PROGRESS, progress)
        context.sendBroadcast(intent)
    }

    private fun tcSetVolume(volume: Int) {
        val intent = Intent(TC_SET_VOLUME)
        intent.putExtra(KEY_VOLUME, volume)
        context.sendBroadcast(intent)
    }
}