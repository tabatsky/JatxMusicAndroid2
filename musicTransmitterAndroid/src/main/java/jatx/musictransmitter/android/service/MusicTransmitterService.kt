package jatx.musictransmitter.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.data.Settings
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Inject

class MusicTransmitterService: Service() {
    @Inject
    lateinit var settings: Settings

    companion object {
        var isInstanceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isInstanceRunning) {
            stopSelf()
            return START_STICKY_COMPATIBILITY
        }

        App.appComponent?.injectMusicTransmitterService(this)

        isInstanceRunning = true

        val actIntent = Intent()
        actIntent.setClass(this, MusicTransmitterActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, actIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("music transmitter service", "Music transmitter service")
        } else {
            ""
        }

        val builder = NotificationCompat.Builder(this, channelId)
        builder.setContentTitle("JatxMusicReceiver")
        builder.setContentText("Foreground service is running")
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        startForeground(2315, notification)

        prepareAndStart(intent)

        return START_STICKY_COMPATIBILITY
    }

    private fun prepareAndStart(intent: Intent?) {

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