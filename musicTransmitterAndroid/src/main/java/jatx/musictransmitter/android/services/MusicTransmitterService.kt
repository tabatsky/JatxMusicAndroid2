package jatx.musictransmitter.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import jatx.extensions.registerExportedReceiver
import jatx.extensions.showToast
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.TestApp
import jatx.musictransmitter.android.domain.PlaylistKeeper
import jatx.musictransmitter.android.domain.Settings
import jatx.musictransmitter.android.domain.TrackInfoStorage
import jatx.musictransmitter.android.media.ArtKeeper
import jatx.musictransmitter.android.threads.LocalPlayer
import jatx.musictransmitter.android.threads.ThreadKeeper
import jatx.musictransmitter.android.threads.TimeUpdater
import jatx.musictransmitter.android.threads.UIController
import jatx.musictransmitter.android.threads.provideTransmitterController
import jatx.musictransmitter.android.threads.provideTransmitterPlayer
import jatx.musictransmitter.android.threads.provideTransmitterPlayerConnectionKeeper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.Delegates

const val CHANNEL_ID = "jatxMusicTransmitter"
const val CHANNEL_NAME = "jatxMusicTransmitter"
const val NOTIFICATION_ID_MEDIA = 1237

const val WAKE_LOCK_TAG = "jatxMusicTransmitterService::wakeLock"
const val WIFI_LOCK_TAG = "music-transmitter-wifi-lock"

const val CLICK_PLAY = "jatx.musictransmitter.android.CLICK_PLAY"
const val CLICK_PAUSE = "jatx.musictransmitter.android.CLICK_PAUSE"
const val CLICK_REW = "jatx.musictransmitter.android.CLICK_REW"
const val CLICK_FWD = "jatx.musictransmitter.android.CLICK_FWD"
const val CLICK_SHUFFLE_NOTIFICATION = "jatx.musictransmitter.android.CLICK_SHUFFLE_NOTIFICATION"
const val CLICK_LOCAL_MODE_NOTIFICATION = "jatx.musictransmitter.android.CLICK_LOCAL_MODE_NOTIFICATION"

const val SET_WIFI_STATUS = "jatx.musictransmitter.android.SET_WIFI_STATUS"
const val SET_CURRENT_TIME = "jatx.musictransmitter.android.SET_CURRENT_TIME"
const val NEXT_TRACK = "jatx.musictransmitter.android.NEXT_TRACK"
const val STOP_SERVICE = "jatx.musictransmitter.android.STOP_SERVICE"

const val TP_AND_TC_PLAY = "jatx.musictransmitter.android.TP_PLAY"
const val TP_AND_TC_PAUSE = "jatx.musictransmitter.android.TP_PAUSE"
const val TP_SET_POSITION = "jatx.musictransmitter.android.TP_SET_POSITION"
const val TP_SEEK = "jatx.musictransmitter.android.TP_SEEK"
const val TP_SET_FILE_LIST = "jatx.musictransmitter.android.TP_SET_FILE_LIST"
const val TC_SET_VOLUME = "jatx.musictransmitter.android.TC_SET_VOLUME"
const val SWITCH_NETWORKING_OR_LOCAL_MODE = "jatx.musictransmitter.android.SWITCH_NETWORKING_OR_LOCAL_MODE"
const val APPLY_SHUFFLE = "jatx.musictransmitter.android.APPLY_SHUFFLE"

const val EXTRA_WIFI_STATUS = "isWifiOk"
const val EXTRA_WIFI_RECEIVER_COUNT = "wifiReceiverCount"

const val KEY_POSITION = "position"
const val KEY_PROGRESS = "progress"
const val KEY_VOLUME = "volume"
const val KEY_CURRENT_MS = "currentMs"
const val KEY_TRACK_LENGTH_MS = "trackLengthMs"

val COMMAND_TOGGLE_LOCAL_MODE = SessionCommand(
    "jatx.musictransmitter.android.TOGGLE_LOCAL_MODE", Bundle.EMPTY
)

@OptIn(UnstableApi::class)
private fun toggleLocalModeButton(isLocalMode: Boolean) = CommandButton.Builder(
    CommandButton.ICON_UNDEFINED
)
    .setDisplayName("LocalMode")
    .setSessionCommand(COMMAND_TOGGLE_LOCAL_MODE)
    .setIconResId(
        if (isLocalMode) {
            R.drawable.ic_sound
        } else {
            R.drawable.ic_wifi_ok
        }
    )
    .setEnabled(true)
    .build()

@OptIn(UnstableApi::class)
private fun toggleShuffleButton(isShuffleEnabled: Boolean) = CommandButton.Builder(
//    if (isShuffleEnabled) {
//        CommandButton.ICON_SHUFFLE_ON
//    } else {
//        CommandButton.ICON_SHUFFLE_OFF
//    }
//)
    CommandButton.ICON_UNDEFINED
)
    .setDisplayName("Shuffle")
    .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, !isShuffleEnabled) // клик выставит противоположное значение
    .setIconResId(
        if (isShuffleEnabled) {
            R.drawable.ic_shuffle
        } else {
            R.drawable.ic_repeat
        }
    )
    .setEnabled(true)
    .build()

@UnstableApi
class MusicTransmitterService: MediaSessionService() {
    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var trackInfoStorage: TrackInfoStorage

    @Inject
    lateinit var playlistKeeper: PlaylistKeeper

    private lateinit var stopSelfReceiver: BroadcastReceiver
    private lateinit var tpSetPositionReceiver: BroadcastReceiver
    private lateinit var tpAndTcPlayReceiver: BroadcastReceiver
    private lateinit var tpAndTcPauseReceiver: BroadcastReceiver
    private lateinit var tpSeekReceiver: BroadcastReceiver
    private lateinit var tpSetFileListReceiver: BroadcastReceiver
    private lateinit var tcSetVolumeReceiver: BroadcastReceiver
    private lateinit var tcSwitchNetworkingOrLocalModeReceiver: BroadcastReceiver
    private lateinit var applyShuffleReceiver: BroadcastReceiver

    private var isPlaying = false
    private var mediaItems = listOf<MediaItem>()

    private inner class MyPlayer: SimpleBasePlayer(Looper.getMainLooper()) {
        @Volatile var currentState: Int = STATE_IDLE
        @Volatile var itemPosition: Int = 0
        @Volatile private var playWhenReady: Boolean = false
        var isShuffle: Boolean
            get() = settings.isShuffle
            set(value) {
                settings.isShuffle = value
            }

        private val handler = Handler(applicationLooper)

        fun invalidate() {
            println("MyPlayer.invalidate() called")   // ①
            handler.postDelayed( {
                println("MyPlayer.invalidate() -> invalidateState() executing")   // ②
                invalidateState()
            }, 500L)
        }

        override fun getState(): State {
            val availableCommands = Player.Commands.Builder()
                .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(COMMAND_PLAY_PAUSE)
                .add(COMMAND_SET_SHUFFLE_MODE)
                .add(COMMAND_GET_METADATA)
                .build()

            itemPosition = playlistKeeper.realPosition

            println("$itemPosition $currentMs $trackLengthMs $isShuffle")

            return State.Builder()
                .setAvailableCommands(availableCommands)
//                .setContentPositionMs(
//                    if (playWhenReady) {
//                        PositionSupplier.getExtrapolating(currentMs.toLong(), 1f)
//                    } else {
//                        PositionSupplier.getConstant(currentMs.toLong())
//                    }
//                )
                .setContentPositionMs(currentMs.toLong())
                .setPlaylist(mediaItems.mapIndexed { index, item ->
                    val builder = MediaItemData.Builder(item.mediaId)
                        .setMediaItem(item)
                    if (index == itemPosition) {
                        builder.setDurationUs(trackLengthMs.toLong() * 1000)
                        val artUri = ArtKeeper.retrieveArtUri(applicationContext, playlistKeeper.tracks[index].path)
                        val metadata = MediaMetadata.Builder()
                            .setTitle(playlistKeeper.tracks[index].title)
                            .setArtist(playlistKeeper.tracks[index].artist)
                            .setArtworkUri(artUri)
                            .build()
                        builder.setMediaMetadata(metadata)
                    } else {
                        val metadata = MediaMetadata.Builder()
                            .setTitle("Music Transmitter")
                            .setArtist("Player is idle")
                            .build()
                        builder.setMediaMetadata(metadata)
                    }
                    builder.build()
                })
                .setCurrentMediaItemIndex(itemPosition)
                .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setShuffleModeEnabled(isShuffle)
                .setPlaybackState(currentState)
                .build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            if (this.playWhenReady != playWhenReady) {
                this.playWhenReady = playWhenReady
                if (currentState == STATE_READY) {
                    if (playWhenReady) {
                        val intent = Intent(TP_AND_TC_PLAY)
                        sendBroadcast(intent)
                    } else {
                        val intent = Intent(TP_AND_TC_PAUSE)
                        sendBroadcast(intent)
                    }
                }
            }
            return Futures.immediateFuture(Unit)
        }

        override fun handleSeek(
            mediaItemIndex: Int,
            positionMs: Long,
            seekCommand: Int
        ): ListenableFuture<*> {
            this.playWhenReady = true

            when (seekCommand) {
                COMMAND_SEEK_TO_MEDIA_ITEM -> {
                    this.itemPosition = mediaItemIndex
                }

                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                    if (trackLengthMs > 0f) {
                        currentMs = positionMs.toFloat()
                        val progress = (positionMs.toDouble() / trackLengthMs.toDouble()).coerceIn(0.0, 1.0)
                        val intent = Intent(TP_SEEK)
                        intent.putExtra(KEY_PROGRESS, progress)
                        sendBroadcast(intent)
                    }
                }

                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    Log.e("click", "rew")
                    val intent = Intent(CLICK_REW)
                    sendBroadcast(intent)
                }

                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    Log.e("click", "fwd")
                    val intent = Intent(CLICK_FWD)
                    sendBroadcast(intent)
                }
            }

            return Futures.immediateFuture(Unit)
        }

        override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
            println("set shuffle mode: $shuffleModeEnabled")
            sendBroadcast(Intent(CLICK_SHUFFLE_NOTIFICATION))   // ваша существующая логика тоггла в презентере остаётся как есть

            // обновляем иконку кнопки под новое состояние
            mediaSession.setMediaButtonPreferences(
                ImmutableList.of(
                    toggleShuffleButton(shuffleModeEnabled),
                    toggleLocalModeButton(settings.isLocalMode)
                )
            )
            return Futures.immediateFuture(Unit)
        }
    }


    private val player = MyPlayer().also {
        it.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                println("onEvents: pos=${player.currentPosition} dur=${player.duration}")
            }
        })
    }

    private val mediaSessionCallback: MediaSession.Callback =
        object : MediaSession.Callback {

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(COMMAND_TOGGLE_LOCAL_MODE)
                    .build()
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == COMMAND_TOGGLE_LOCAL_MODE.customAction) {
                    // ваша логика по клику
                    sendBroadcast(Intent(CLICK_LOCAL_MODE_NOTIFICATION))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            @UnstableApi
            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent =
                    intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                Log.e("keyEvent", keyEvent.toString())
                if (keyEvent?.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode
                    in listOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE)) {

                    if (isPlaying) {
                        val theIntent = Intent(CLICK_PAUSE)
                        sendBroadcast(theIntent)
                    } else {
                        val theIntent = Intent(CLICK_PLAY)
                        sendBroadcast(theIntent)
                    }

                    return true
                } else if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {

                    val theIntent = Intent(CLICK_FWD)
                    sendBroadcast(theIntent)

                    return true
                } else if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {

                    val theIntent = Intent(CLICK_REW)
                    sendBroadcast(theIntent)

                    return true
                }

                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }


    companion object {
        private var _tk: ThreadKeeper? = null
        val tk: ThreadKeeper
            get() = _tk ?: throw IllegalStateException("null thread keeper")

        private fun setTk(tk: ThreadKeeper?) {
            _tk = tk
        }

        var mediaSession by Delegates.notNull<MediaSession>()
    }

    @Volatile
    private var wifiStatus = false
    @Volatile
    private var currentMs = 0f
    @Volatile
    private var trackLengthMs = 0f

    private var wifiLock: WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val uiController = object: UIController {
        override fun updateWifiStatus(count: Int) {
            println("(UIController) update wifi status: $count")
            wifiStatus = count > 0
            val intent = Intent(SET_WIFI_STATUS)
            intent.putExtra(EXTRA_WIFI_STATUS, wifiStatus)
            intent.putExtra(EXTRA_WIFI_RECEIVER_COUNT, count)
            sendBroadcast(intent)
            if (!wifiStatus) {
                val intent2 = Intent(TP_AND_TC_PAUSE)
                sendBroadcast(intent2)
            }
        }

        override fun setCurrentTime(currentMs: Float, trackLengthMs: Float) {
            this@MusicTransmitterService.currentMs = currentMs
            this@MusicTransmitterService.trackLengthMs = trackLengthMs
            val intent = Intent(SET_CURRENT_TIME)
            intent.putExtra(KEY_CURRENT_MS, currentMs)
            intent.putExtra(KEY_TRACK_LENGTH_MS, trackLengthMs)
            sendBroadcast(intent)
        }

        override fun errorMsg(msg: String) {
            showToast(msg)
        }

        override fun errorMsg(resId: Int) {
            showToast(resId)
        }

        override fun nextTrack() {
            val intent = Intent(NEXT_TRACK)
            sendBroadcast(intent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun injectDependencies() {
        if (application is App) {
            App.appComponent?.injectMusicTransmitterService(this)
        } else if (application is TestApp) {
            TestApp.appComponent?.injectMusicTransmitterService(this)
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        injectDependencies()
        initMediaSession()

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID, CHANNEL_NAME)
        } else {
            ""
        }

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(channelId)
            .setNotificationId(NOTIFICATION_ID_MEDIA)
            .build()

        setMediaNotificationProvider(provider)

        lockWifi()
        lockWake()
        prepareAndStart()
    }

    @UnstableApi
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        Log.e("notif_debug", "onUpdateNotification CALLED, startInForegroundRequired=$startInForegroundRequired")
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (t: Throwable) {
            Log.e("notif_debug", "onUpdateNotification THREW", t)
        }
    }

    override fun onDestroy() {
        unlockWake()
        unlockWifi()

        tk.tu.interrupt()
        tk.tc.finishFlag = true
        tk.tp.interrupt()
        tk.tpda.interrupt()

        setTk(null)

        mediaSession.release()

        unregisterReceivers()

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun lockWifi() {
        val wifiManager =
            applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiMode = if (Build.VERSION.SDK_INT >= 29) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifiManager.createWifiLock(wifiMode, WIFI_LOCK_TAG)
        wifiLock?.setReferenceCounted(false)
        wifiLock?.acquire()
    }

    private fun unlockWifi() {
        wifiLock?.release()
    }

    private fun lockWake() {
        Log.e("wakeLock", WAKE_LOCK_TAG)
        val wifiManager =
            applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = wifiManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire(1200 * 1000L)
        }
    }

    private fun unlockWake() {
        Log.e("wakeUnlock", WAKE_LOCK_TAG)
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    @UnstableApi
    private fun prepareAndStart() {
        initBroadcastReceivers()
        updatePlaylist()

        val tu = TimeUpdater(uiController)
        val tc = application.provideTransmitterController(settings.volume, !settings.isLocalMode)
        val tp = application.provideTransmitterPlayer(uiController)
        val tpda = if (settings.isLocalMode) {
            LocalPlayer()
        } else {
            application.provideTransmitterPlayerConnectionKeeper(uiController)
        }

        setTk(ThreadKeeper(tu, tc, tp, tpda))

        tc.tk = tk
        tp.tk = tk
        tpda.tk = tk

        tp.isNetworkingMode = !settings.isLocalMode
        tp.files = settings.currentFileList

        tu.start()
        tc.start()
        tpda.start()
        tp.start()
    }

    @UnstableApi
    private fun initMediaSession() {
        mediaSession = MediaSession
            .Builder(this, player)
            .setCallback(mediaSessionCallback)
            .setMediaButtonPreferences(
                ImmutableList.of(
                    toggleShuffleButton(settings.isShuffle),
                    toggleLocalModeButton(settings.isLocalMode)
                )
            )   // ← вместо setCustomLayout
            .build()

        addSession(mediaSession)
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private fun updatePlaylist() {
        lifecycleScope.launch {
            mediaItems = withContext(Dispatchers.IO) {
                settings.currentFileList.mapNotNull {
                    try {
                        val uri = Uri.fromFile(it)
                        val mediaItem = MediaItem
                            .Builder()
                            .setUri(uri)
                            .setMediaId(randomAlphanumeric(16))
                            .build()
                        mediaItem
                    } catch (t: Throwable) {
                        null
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (mediaItems.isEmpty()) {
                    player.currentState = Player.STATE_IDLE
                } else {
                    player.currentState = Player.STATE_READY
                }
                player.invalidate()
            }
        }
    }

    private fun initBroadcastReceivers() {
        stopSelfReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopSelf()
            }
        }
        registerExportedReceiver(stopSelfReceiver, IntentFilter(STOP_SERVICE))

        tpSetPositionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val position = intent.getIntExtra(KEY_POSITION, 0)
                tk.tp.position = position
                player.seekTo(position, 0)
                player.invalidate()
            }
        }
        registerExportedReceiver(tpSetPositionReceiver, IntentFilter(TP_SET_POSITION))

        tpAndTcPlayReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPlaying = true
                tk.tp.play()
                tk.tc.play()
                player.play()
            }
        }
        registerExportedReceiver(tpAndTcPlayReceiver, IntentFilter(TP_AND_TC_PLAY))

        tpAndTcPauseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPlaying = false
                tk.tp.pause()
                tk.tc.pause()
                player.pause()
            }
        }
        registerExportedReceiver(tpAndTcPauseReceiver, IntentFilter(TP_AND_TC_PAUSE))

        tpSeekReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val progress = intent.getDoubleExtra(KEY_PROGRESS, 0.0)
                tk.tp.seek(progress)
                val mediaItemIndex = tk.tp.position
                player.seekTo(mediaItemIndex, (trackLengthMs * progress).toLong())
                player.invalidate()
            }
        }
        registerExportedReceiver(tpSeekReceiver, IntentFilter(TP_SEEK))

        tpSetFileListReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tk.tp.files = settings.currentFileList
                updatePlaylist()
            }
        }
        registerExportedReceiver(tpSetFileListReceiver, IntentFilter(TP_SET_FILE_LIST))

        tcSetVolumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val volume = intent.getIntExtra("volume", 0)
                tk.tc.volume = volume
            }
        }
        registerExportedReceiver(tcSetVolumeReceiver, IntentFilter(TC_SET_VOLUME))

        tcSwitchNetworkingOrLocalModeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                switchNetworkingOrLocalMode()
                refreshShuffleAndLocalMode()
            }
        }
        registerExportedReceiver(tcSwitchNetworkingOrLocalModeReceiver,
            IntentFilter(SWITCH_NETWORKING_OR_LOCAL_MODE)
        )

        applyShuffleReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                refreshShuffleAndLocalMode()
            }
        }
        registerExportedReceiver(applyShuffleReceiver,
            IntentFilter(APPLY_SHUFFLE)
        )
    }

    private fun unregisterReceivers() {
        unregisterReceiver(stopSelfReceiver)
        unregisterReceiver(tpSetPositionReceiver)
        unregisterReceiver(tpAndTcPlayReceiver)
        unregisterReceiver(tpAndTcPauseReceiver)
        unregisterReceiver(tpSeekReceiver)
        unregisterReceiver(tpSetFileListReceiver)
        unregisterReceiver(tcSetVolumeReceiver)
        unregisterReceiver(tcSwitchNetworkingOrLocalModeReceiver)
    }

    private fun switchNetworkingOrLocalMode() {
        tk.tpda.interrupt()
        tk.tc.interrupt()
        val tpda = if (settings.isLocalMode) {
            LocalPlayer()
        } else {
            application.provideTransmitterPlayerConnectionKeeper(uiController)
        }
        val tc = application.provideTransmitterController(settings.volume, !settings.isLocalMode)
        setTk(ThreadKeeper(tk.tu, tc, tk.tp, tpda))
        tc.tk = tk
        tk.tp.tk = tk
        tk.tp.isNetworkingMode = !settings.isLocalMode
        tpda.tk = tk
        tc.start()
        tpda.start()
    }

    private fun refreshShuffleAndLocalMode() {
        mediaSession
            .setMediaButtonPreferences(
                ImmutableList.of(
                    toggleShuffleButton(settings.isShuffle),
                    toggleLocalModeButton(settings.isLocalMode)
                )
            )
        player.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = NotificationManagerCompat.from(this)
        service.createNotificationChannel(channel)
        return channelId
    }
}

fun randomAlphanumeric(length: Int): String {
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { charPool.random() }
        .joinToString("")
}