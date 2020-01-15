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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import jatx.musicreceiver.android.ui.MusicReceiverActivity

const val STOP_SERVICE = "jatx.musicreceiver.android.STOP_SERVICE"
const val SERVICE_START_JOB = "jatx.musicreceiver.android.SERVICE_START_JOB"
const val SERVICE_STOP_JOB = "jatx.musicreceiver.android.SERVICE_STOP_JOB"

class MusicReceiverService : Service() {

    private lateinit var stopSelfReceiver: BroadcastReceiver

    private var lock: WifiManager.WifiLock? = null

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        lockWifi()
        prepareAndStart(intent)

        return START_STICKY_COMPATIBILITY
    }

    override fun onDestroy() {
        unlockWifi()

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
            createNotificationChannel("music receiver service", "Music receiver service")
        } else {
            ""
        }

        val builder = NotificationCompat.Builder(this, channelId)
        builder.setContentTitle("JatxMusicReceiver")
        builder.setContentText("Foreground service is running")
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        startForeground(1523, notification)
    }

    private fun lockWifi() {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        lock = wifiManager.createWifiLock("music-transmitter-wifi-lock")
        lock?.setReferenceCounted(false)
        lock?.acquire()
    }

    private fun unlockWifi() {
        lock?.release()
    }

    private fun prepareAndStart(intent: Intent?) {
        initBroadcastReceivers()
    }

    private fun initBroadcastReceivers() {
        stopSelfReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopSelf()
            }
        }
        registerReceiver(stopSelfReceiver, IntentFilter(STOP_SERVICE))
    }

    private fun unregisterReceivers() {
        unregisterReceiver(stopSelfReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}