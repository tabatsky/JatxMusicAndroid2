package jatx.musicreceiver.android.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import jatx.musicreceiver.android.App
import jatx.musicreceiver.android.audio.AndroidSoundOut
import jatx.musicreceiver.android.data.Settings
import jatx.musicreceiver.android.presentation.UI_START_JOB
import jatx.musicreceiver.android.presentation.UI_STOP_JOB
import jatx.musicreceiver.android.threads.AutoConnectThread
import jatx.musicreceiver.android.threads.ReceiverController
import jatx.musicreceiver.android.threads.ReceiverPlayer
import jatx.musicreceiver.android.threads.UIController
import jatx.musicreceiver.android.ui.MusicReceiverActivity
import javax.inject.Inject

const val CHANNEL_ID_SERVICE = "jatxMusicReceiverService"
const val CHANNEL_NAME_SERVICE = "jatxMusicReceiverService"
const val WAKE_LOCK_TAG = "jatxMusicReceiverService::wakeLock"
const val WIFI_LOCK_TAG = "music-receiver-wifi-lock"

const val STOP_SERVICE = "jatx.musicreceiver.android.STOP_SERVICE"
const val SERVICE_START_JOB = "jatx.musicreceiver.android.SERVICE_START_JOB"
const val SERVICE_STOP_JOB = "jatx.musicreceiver.android.SERVICE_STOP_JOB"

class MusicReceiverService : Service() {
    @Inject
    lateinit var settings: Settings

    private lateinit var stopSelfReceiver: BroadcastReceiver
    private lateinit var serviceStartJobReceiver: BroadcastReceiver
    private lateinit var serviceStopJobReceiver: BroadcastReceiver

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isRunning = false

    @Volatile private var rp: ReceiverPlayer? = null
    @Volatile private var rc: ReceiverController? = null
    private var act: AutoConnectThread? = null

    private val uiController = object : UIController {
        override fun startJob() {
            this@MusicReceiverService.startJob()
        }

        override fun stopJob() {
            this@MusicReceiverService.stopJob()
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        App.appComponent?.injectMusicReceiverService(this)

        startForeground()
        lockWifi()
        lockWake()
        prepareAndStart(intent)

        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        unlockWake()
        unlockWifi()

        act?.interrupt()
        stopJob()

        unregisterReceivers()

        stopForeground(true)
        super.onDestroy()
    }

    private fun startForeground() {
        val actIntent = Intent()
        actIntent.setClass(this, MusicReceiverActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, actIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(CHANNEL_ID_SERVICE, CHANNEL_NAME_SERVICE)
        } else {
            ""
        }

        val builder = NotificationCompat.Builder(this, channelId)

        val notification = builder
            .setContentTitle("JatxMusicReceiver")
            .setContentText("Foreground service is running")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1523, notification)
    }

    private fun lockWifi() {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG)
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
            acquire()
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

    private fun prepareAndStart(intent: Intent?) {
        initBroadcastReceivers()

        act = AutoConnectThread(settings, uiController)
        act?.start()
    }

    private fun initBroadcastReceivers() {
        stopSelfReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopSelf()
            }
        }
        registerReceiver(stopSelfReceiver, IntentFilter(STOP_SERVICE))

        serviceStartJobReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                startJob()
            }
        }
        registerReceiver(serviceStartJobReceiver, IntentFilter(SERVICE_START_JOB))

        serviceStopJobReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopJob()
            }
        }
        registerReceiver(serviceStopJobReceiver, IntentFilter(SERVICE_STOP_JOB))
    }

    private fun unregisterReceivers() {
        unregisterReceiver(stopSelfReceiver)
        unregisterReceiver(serviceStartJobReceiver)
        unregisterReceiver(serviceStopJobReceiver)
    }

    private fun startJob() {
        if (isRunning) return
        isRunning = true
        rp = ReceiverPlayer(settings.host, uiController, AndroidSoundOut())
        rc = ReceiverController(settings.host, uiController)
        rc?.rp = rp
        rp?.start()
        rc?.start()
        val intent = Intent(UI_START_JOB)
        sendBroadcast(intent)
    }

    private fun stopJob() {
        if (!isRunning) return
        isRunning = false
        rp?.setupFinishFlag()
        rc?.setupFinishFlag()
        val intent = Intent(UI_STOP_JOB)
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}