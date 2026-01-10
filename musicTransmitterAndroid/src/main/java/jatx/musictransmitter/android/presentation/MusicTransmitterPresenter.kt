package jatx.musictransmitter.android.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.Log
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import jatx.extensions.registerExportedReceiver
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.domain.Settings
import jatx.musictransmitter.android.domain.TrackInfoStorage
import jatx.musictransmitter.android.media.ArtKeeper
import jatx.musictransmitter.android.services.*
import jatx.musictransmitter.android.ui.*
import jatx.musictransmitter.android.util.findFiles
import moxy.InjectViewState
import moxy.MvpPresenter
import java.io.File
import java.io.PrintWriter
import java.util.*
import javax.inject.Inject

const val ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE"

@InjectViewState
class MusicTransmitterPresenter @Inject constructor(
    private val context: Context,
    private val settings: Settings,
    private val trackInfoStorage: TrackInfoStorage
): MvpPresenter<MusicTransmitterView>() {

    private lateinit var setCurrentTimeReceiver: BroadcastReceiver
    private lateinit var setWifiStatusReceiver: BroadcastReceiver
    private lateinit var nextTrackReceiver: BroadcastReceiver
    private lateinit var prevTrackReceiver: BroadcastReceiver
    private lateinit var clickPlayReceiver: BroadcastReceiver
    private lateinit var clickPauseReceiver: BroadcastReceiver
    private lateinit var incomingCallReceiver: BroadcastReceiver

    private val files = arrayListOf<File>()
    private var currentPosition = -1
    private var tracks = listOf<Track>()

    private val shuffledList = arrayListOf<Int>()
    private var isShuffle: Boolean
        get() = settings.isShuffle
        set(value) {
            settings.isShuffle = value
        }

    private val realPosition: Int
        get() = when {
            currentPosition < 0 -> {
                currentPosition
            }
            isShuffle -> {
                shuffledList[currentPosition % shuffledList.size] % files.size
            }
            else -> {
                (currentPosition % shuffledList.size) % files.size
            }
        }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        for (i in 0 until 100000) shuffledList.add(i)
        shuffledList.shuffle()

        trackInfoStorage.setOnUpdateTrackListListener { tracks ->
            this.tracks = tracks
            if (isShuffle) {
                viewState.showTracks(tracks, realPosition)
            } else {
                viewState.showTracks(tracks, currentPosition)
            }
        }

        files.addAll(settings.currentFileList)
        updateTrackInfoStorageFiles()

        viewState.showVolume(settings.volume)
        viewState.showShuffleState(isShuffle)

        viewState.showLocalMode(settings.isLocalMode)

        checkReadPhoneStatePermission()
        checkNotificationPermissions()
        initBroadcastReceivers()
        startService()
    }

    override fun onDestroy() {
        MusicTransmitterNotification.hideNotification(context)
        stopService()
        unregisterReceivers()
    }

    fun onBackPressed() = viewState.showQuitDialog()

    fun onQuit() = viewState.quit()

    fun onPlayClick() {
        if (files.isEmpty()) {
            return
        } else if (currentPosition == -1) {
            currentPosition = 0
            viewState.showTracks(tracks, realPosition)
            tpSetPosition(realPosition)
        }

        viewState.showPlayingState(true)
        tpAndTcPlay()

        val track = tracks[realPosition]
        val albumArt = ArtKeeper.retrieveArt(context, track.path)
        MusicTransmitterNotification.showNotification(context, track.artist, track.title, albumArt,true)
    }

    fun onPauseClick(needSendBroadcast: Boolean, needShowNotification: Boolean) {
        viewState.showPlayingState(false)
        if (needSendBroadcast) {
            tpAndTcPause()
        }

        if (currentPosition > -1 && needShowNotification) {
            val track = tracks[realPosition]
            val albumArt = ArtKeeper.retrieveArt(context, track.path)
            MusicTransmitterNotification.showNotification(context, track.artist, track.title, albumArt,false)
        }
    }
    
    fun onRepeatClick() {
        isShuffle = true
        currentPosition = shuffledList.indexOf(currentPosition)
        viewState.showShuffleState(true)
    }

    fun onShuffleClick() {
        isShuffle = false
        if (currentPosition >= 0) {
            currentPosition = shuffledList[currentPosition]
        }
        viewState.showShuffleState(false)
    }

    fun onRewClick() {
        if (files.isEmpty()) return

        currentPosition = when {
            currentPosition > 0 -> {
                currentPosition - 1
            }
            isShuffle -> {
                shuffledList.size - 1
            }
            else -> {
                files.size - 1
            }
        }

        tpSetPosition(realPosition)
        viewState.showTracks(tracks, realPosition)
        viewState.scrollToPosition(realPosition)
        onPlayClick()
    }

    fun onFwdClick() {
        if (files.isEmpty()) return

        currentPosition += 1

        tpSetPosition(realPosition)
        viewState.showTracks(tracks, realPosition)
        viewState.scrollToPosition(realPosition)
        onPlayClick()
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
        viewState.tryShowTrackSelectDialog()
    }

    fun onAddArtistSelected() {
        viewState.tryShowArtistSelectDialog()
    }

    fun onAddAlbumSelected() {
        viewState.tryShowAlbumSelectDialog()
    }

    fun onTrackOpened(path: String) {
        val file = File(path)
        files.add(file)
        settings.currentMusicDirPath = file.parentFile?.absolutePath ?: ""
        updateTpFiles()
        updateTrackInfoStorageFiles()
    }

    fun onFolderOpened(path: String) {
        files.addAll(findFiles(path) { it.extension in setOf("mp3", "flac") } )
        settings.currentMusicDirPath = path
        updateTpFiles()
        updateTrackInfoStorageFiles()
    }

    fun addFiles(files: List<File>) {
        this.files.addAll(files)
        updateTpFiles()
        updateTrackInfoStorageFiles()
    }

    fun onRemoveAllTracksSelected() {
        files.clear()
        updateTpFiles()
        updateTrackInfoStorageFiles()
        currentPosition = -1
    }

    fun onRemoveTrackSelected() = viewState.showRemoveTrackMessage()

    fun onExportPlaylistSelected() = viewState.trySavePlaylist()

    fun onImportPlaylistSelected() = viewState.tryLoadPlaylist()

    fun onTrackClick(position: Int) {
        currentPosition = if (isShuffle) {
            shuffledList.indexOf(position)
        } else {
            position
        }
        viewState.showTracks(tracks, realPosition)
        onPlayClick()
        tpSetPosition(realPosition)
    }

    fun onTrackLongClick(position: Int) = viewState.showTrackLongClickDialog(position)

    fun onNetworkingOrLocalModeGroupClick() = viewState
        .showNetworkingOrLocalModeDialog(settings.isLocalMode)

    fun onSetLocalMode(isLocalMode: Boolean) {
        settings.isLocalMode = isLocalMode
        viewState.showLocalMode(isLocalMode)
        switchNetworkingOrLocalMode()
    }

    fun onDeleteTrack(position: Int) {
        files.removeAt(position)
        if (position < realPosition) {
            if (isShuffle) {
                currentPosition = shuffledList.indexOf(realPosition - 1)
            } else {
                currentPosition -= 1
            }
        } else if (position == realPosition) {
            currentPosition = -1
        } else if (isShuffle) {
            currentPosition = shuffledList.indexOf(realPosition)
        }
        updateTpFiles()
        updateTrackInfoStorageFiles()
    }

    fun onOpenTagEditor(position: Int) {
        val file = files[position]
        if (file.extension in setOf("mp3", "flac")) {
            viewState.showTagEditor(Uri.fromFile(file))
        } else {
            viewState.showWrongFileFormatToast()
        }
    }

    fun onSavePlaylist(uri: Uri) {
        val playlistContent = files.joinToString("\n") { it.absolutePath }
        Log.e("playlist", playlistContent)
        try {
            context.contentResolver.openOutputStream(uri)?.let {
                PrintWriter(it).use { pw ->
                    pw.println(playlistContent)
                    pw.flush()
                }
            }
            viewState.showSavePlaylistSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            viewState.showSavePlaylistError()
        }
    }

    fun onLoadPlaylist(uri: Uri) {
        try {
            files.clear()

            Scanner(context.contentResolver.openInputStream(uri)).use { sc ->
                while (sc.hasNextLine()) {
                    val path = sc.nextLine()
                    files.add(File(path))
                }
            }

            updateTpFiles()
            updateTrackInfoStorageFiles()
            currentPosition = -1

            viewState.showLoadPlaylistSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            viewState.showLoadPlaylistError()
        }
    }

    fun onProgressChanged(progress: Double) = tpSeek(progress)

    fun onAddMicSelected() = viewState.tryAddMic()

    fun onAddMicPermissionsAccepted() {
        files.add(File(MIC_PATH))
        updateTpFiles()
        updateTrackInfoStorageFiles()
    }

    fun onShowIPSelected() {
        val wifiMgr = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val wifiInfo = wifiMgr.connectionInfo
        @Suppress("DEPRECATION")
        val wifiAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)

//        detects non wifi IP address
//        not used because ServerSocket for mobile networking not working
//        do not delete, may be solution for mobile networking will be found in the future

//        val firstCorrectAddress = Collections.list(NetworkInterface.getNetworkInterfaces()).map { intf ->
//            Log.e("interface", intf.displayName)
//            val hostAddress = Collections.list(intf.inetAddresses).firstOrNull { addr ->
//                (!addr.isLoopbackAddress).and(
//                    addr is Inet4Address
//                ).and(
//                    (addr.hostAddress ?: "0.0.0.0") != "0.0.0.0"
//                )
//            }?.hostAddress
//            Log.e("host address", hostAddress ?: "0.0.0.0")
//            hostAddress
//        }.firstOrNull {
//            (it ?: "0.0.0.0") != "0.0.0.0"
//        } ?: "0.0.0.0"

//        val correctAddress = if (wifiAddress != "0.0.0.0") wifiAddress else firstCorrectAddress

        val correctAddress = if (wifiAddress != "0.0.0.0") wifiAddress else "Wifi not available"

        viewState.showIPAddress(correctAddress)
    }

    fun onReturnFromTagEditor() = updateTrackInfoStorageFiles()

    fun onShowManualSelected() = viewState.showManual()

    fun onReviewAppSelected() = viewState.showReviewAppActivity()

    fun onReceiverAndroidSelected() = viewState.showReceiverAndroidActivity()

    fun onReceiverFXSelected() = viewState.showReceiverFXActivity()

    fun onTransmitterFXSelected() = viewState.showTransmitterFXActivity()

    fun onSourceCodeSelected() = viewState.showSourceCodeActivity()

    fun onDevSiteSelected() = viewState.showDevSiteActivity()

    private fun updateTpFiles() {
        settings.currentFileList = files
        context.sendBroadcast(Intent(TP_SET_FILE_LIST))
    }

    private fun updateTrackInfoStorageFiles() {
        trackInfoStorage.files = files
    }

    private fun startService() {
        val intent = Intent(context, MusicTransmitterService::class.java)
        context.startService(intent)
    }

    private fun stopService() {
        val intent = Intent(STOP_SERVICE)
        context.sendBroadcast(intent)
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

    private fun switchNetworkingOrLocalMode() {
        val intent = Intent(SWITCH_NETWORKING_OR_LOCAL_MODE)
        context.sendBroadcast(intent)
    }

    private fun checkReadPhoneStatePermission() {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {}
            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
        }

        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setPermissions(Manifest.permission.READ_PHONE_STATE)
            .check()
    }

    private fun checkNotificationPermissions() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {}

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
        }

        if (Build.VERSION.SDK_INT >= 33) {
            TedPermission.create()
                .setPermissionListener(permissionListener)
                .setPermissions(
                    Manifest.permission.POST_NOTIFICATIONS
                )
                .check()
        }
    }

    private fun initBroadcastReceivers() {
        setCurrentTimeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val currentMs = intent.getFloatExtra(KEY_CURRENT_MS, 0f)
                val trackLengthMs = intent.getFloatExtra(KEY_TRACK_LENGTH_MS, 0f)
                viewState.showCurrentTime(currentMs, trackLengthMs)
            }
        }
        context.registerExportedReceiver(setCurrentTimeReceiver, IntentFilter(SET_CURRENT_TIME))

        setWifiStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getBooleanExtra(EXTRA_WIFI_STATUS, false)
                viewState.showWifiStatus(status)
                val count = intent.getIntExtra(EXTRA_WIFI_RECEIVER_COUNT, 0)
                viewState.showWifiReceiverCount(count)
            }
        }
        context.registerExportedReceiver(setWifiStatusReceiver, IntentFilter(SET_WIFI_STATUS))

        nextTrackReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onFwdClick()
            }
        }
        context.registerExportedReceiver(nextTrackReceiver, IntentFilter(NEXT_TRACK))
        context.registerExportedReceiver(nextTrackReceiver, IntentFilter(CLICK_FWD))

        prevTrackReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onRewClick()
            }
        }
        context.registerExportedReceiver(prevTrackReceiver, IntentFilter(CLICK_REW))

        clickPlayReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPlayClick()
            }
        }
        context.registerExportedReceiver(clickPlayReceiver, IntentFilter(CLICK_PLAY))

        clickPauseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPauseClick(needSendBroadcast = true, needShowNotification = true)
            }
        }
        context.registerExportedReceiver(clickPauseReceiver, IntentFilter(CLICK_PAUSE))

        incomingCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) == TelephonyManager.EXTRA_STATE_RINGING) {
                    onPauseClick(needSendBroadcast = true, needShowNotification = true)
                }
            }
        }
        context.registerReceiver(incomingCallReceiver, IntentFilter(ACTION_PHONE_STATE))
    }

    private fun unregisterReceivers() {
        context.unregisterReceiver(setCurrentTimeReceiver)
        context.unregisterReceiver(setWifiStatusReceiver)
        context.unregisterReceiver(nextTrackReceiver)
        context.unregisterReceiver(prevTrackReceiver)
        context.unregisterReceiver(clickPlayReceiver)
        context.unregisterReceiver(clickPauseReceiver)
        context.unregisterReceiver(incomingCallReceiver)
    }
}