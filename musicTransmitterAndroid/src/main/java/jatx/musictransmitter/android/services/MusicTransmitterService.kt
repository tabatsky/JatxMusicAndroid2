package jatx.musictransmitter.android.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import jatx.extensions.registerExportedReceiver
import jatx.extensions.showToast
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.TestApp
import jatx.musictransmitter.android.domain.Settings
import jatx.musictransmitter.android.threads.LocalPlayer
import jatx.musictransmitter.android.threads.ThreadKeeper
import jatx.musictransmitter.android.threads.TimeUpdater
import jatx.musictransmitter.android.threads.UIController
import jatx.musictransmitter.android.threads.provideTransmitterController
import jatx.musictransmitter.android.threads.provideTransmitterPlayer
import jatx.musictransmitter.android.threads.provideTransmitterPlayerConnectionKeeper
import jatx.musictransmitter.android.ui.CLICK_FWD
import jatx.musictransmitter.android.ui.CLICK_PAUSE
import jatx.musictransmitter.android.ui.CLICK_PLAY
import jatx.musictransmitter.android.ui.CLICK_REW
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Inject
import kotlin.properties.Delegates


const val CHANNEL_ID_SERVICE = "jatxMusicTransmitterService"
const val CHANNEL_NAME_SERVICE = "jatxMusicTransmitterService"
const val WAKE_LOCK_TAG = "jatxMusicTransmitterService::wakeLock"
const val WIFI_LOCK_TAG = "music-transmitter-wifi-lock"

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

const val EXTRA_WIFI_STATUS = "isWifiOk"
const val EXTRA_WIFI_RECEIVER_COUNT = "wifiReceiverCount"

const val KEY_POSITION = "position"
const val KEY_PROGRESS = "progress"
const val KEY_VOLUME = "volume"
const val KEY_CURRENT_MS = "currentMs"
const val KEY_TRACK_LENGTH_MS = "trackLengthMs"

class MusicTransmitterService: MediaBrowserServiceCompat() {
    @Inject
    lateinit var settings: Settings

    private lateinit var stopSelfReceiver: BroadcastReceiver
    private lateinit var tpSetPositionReceiver: BroadcastReceiver
    private lateinit var tpAndTcPlayReceiver: BroadcastReceiver
    private lateinit var tpAndTcPauseReceiver: BroadcastReceiver
    private lateinit var tpSeekReceiver: BroadcastReceiver
    private lateinit var tpSetFileListReceiver: BroadcastReceiver
    private lateinit var tcSetVolumeReceiver: BroadcastReceiver
    private lateinit var tcSwitchNetworkingOrLocalModeReceiver: BroadcastReceiver

    private var isPlaying = false

    private val mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {

            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent =
                    mediaButtonEvent!!.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                Log.e("keyEvent", keyEvent.toString())
                if (keyEvent?.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode
                    in listOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE)) {

                    if (isPlaying) {
                        val intent = Intent(CLICK_PAUSE)
                        sendBroadcast(intent)
                    } else {
                        val intent = Intent(CLICK_PLAY)
                        sendBroadcast(intent)
                    }

                    return true
                } else if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {

                    val intent = Intent(CLICK_FWD)
                    sendBroadcast(intent)

                    return true
                } else if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {

                    val intent = Intent(CLICK_REW)
                    sendBroadcast(intent)

                    return true
                }

                return super.onMediaButtonEvent(mediaButtonEvent)
            }
        }


    companion object {
        private var _tk: ThreadKeeper? = null
        val tk: ThreadKeeper
            get() = _tk ?: throw IllegalStateException("null thread keeper")

        private fun setTk(tk: ThreadKeeper?) {
            _tk = tk
        }

        var mediaSessionCompat by Delegates.notNull<MediaSessionCompat>()
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

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        result.sendResult(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun injectDependencies() {
        if (application is App) {
            App.appComponent?.injectMusicTransmitterService(this)
        } else if (application is TestApp) {
            TestApp.appComponent?.injectMusicTransmitterService(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        injectDependencies()

        startForeground()
        lockWifi()
        lockWake()
        prepareAndStart()

        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        unlockWake()
        unlockWifi()

        tk.tu.interrupt()
        tk.tc.finishFlag = true
        tk.tp.interrupt()
        tk.tpda.interrupt()

        setTk(null)

        unregisterReceivers()

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun startForeground() {
        val actIntent = Intent()
        actIntent.setClass(this, MusicTransmitterActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT < 23) {
            PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, actIntent, flags)

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val builder = NotificationCompat.Builder(this, channelId)

        val notification = builder
            .setContentTitle("JatxMusicTransmitter")
            .setContentText("Foreground service is running")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(2315, notification)

        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            Toast
                .makeText(
                    this,
                    R.string.toast_please_enable_notifications,
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }

    private fun lockWifi() {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
            applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
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

    private fun prepareAndStart() {
        initBroadcastReceivers()
        initMediaSession()

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

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(
            applicationContext,
            MediaButtonReceiver::class.java
        )
        mediaSessionCompat =
            MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)
        mediaSessionCompat.setCallback(mediaSessionCallback)
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val flags = if (Build.VERSION.SDK_INT < 23) {
            0
        } else {
            PendingIntent.FLAG_IMMUTABLE
        }

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, flags)
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent)
        sessionToken = mediaSessionCompat.sessionToken
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
            }
        }
        registerExportedReceiver(tpSetPositionReceiver, IntentFilter(TP_SET_POSITION))

        tpAndTcPlayReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPlaying = true
                tk.tp.play()
                tk.tc.play()
            }
        }
        registerExportedReceiver(tpAndTcPlayReceiver, IntentFilter(TP_AND_TC_PLAY))

        tpAndTcPauseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPlaying = false
                tk.tp.pause()
                tk.tc.pause()
            }
        }
        registerExportedReceiver(tpAndTcPauseReceiver, IntentFilter(TP_AND_TC_PAUSE))

        tpSeekReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val progress = intent.getDoubleExtra(KEY_PROGRESS, 0.0)
                tk.tp.seek(progress)
            }
        }
        registerExportedReceiver(tpSeekReceiver, IntentFilter(TP_SEEK))

        tpSetFileListReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tk.tp.files = settings.currentFileList
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
            }
        }
        registerExportedReceiver(tcSwitchNetworkingOrLocalModeReceiver,
            IntentFilter(SWITCH_NETWORKING_OR_LOCAL_MODE)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channel = NotificationChannel(CHANNEL_ID_SERVICE, CHANNEL_NAME_SERVICE, NotificationManager.IMPORTANCE_MIN)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = NotificationManagerCompat.from(this)
        service.createNotificationChannel(channel)
        return CHANNEL_ID_SERVICE
    }

}