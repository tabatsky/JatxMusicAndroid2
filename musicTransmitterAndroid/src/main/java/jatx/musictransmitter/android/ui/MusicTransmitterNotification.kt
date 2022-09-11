package jatx.musictransmitter.android.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import jatx.musictransmitter.android.R
import kotlin.random.Random

const val CHANNEL_ID = "jatxMusicTransmitter"
const val CHANNEL_NAME = "jatxMusicTransmitter"

const val CLICK_PLAY = "jatx.musictransmitter.android.CLICK_PLAY"
const val CLICK_PAUSE = "jatx.musictransmitter.android.CLICK_PAUSE"
const val CLICK_REV = "jatx.musictransmitter.android.CLICK_REV"
const val CLICK_FWD = "jatx.musictransmitter.android.CLICK_FWD"

object MusicTransmitterNotification {
    private val notificationId by lazy {
        Random(1237).nextInt()
    }

    fun showNotification(context: Context, artist: String, title: String, isPlaying: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID , CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val contentView = RemoteViews(context.packageName, R.layout.notification)

        contentView.setTextViewText(R.id.text_title, title)
        contentView.setTextViewText(R.id.text_artist, artist)

        contentView.setViewVisibility(R.id.pause, if (isPlaying) View.VISIBLE else View.GONE)
        contentView.setViewVisibility(R.id.play, if (isPlaying) View.GONE else View.VISIBLE)

        val flags = if (Build.VERSION.SDK_INT < 23) {
            0
        } else {
            PendingIntent.FLAG_IMMUTABLE
        }

        val playIntent = Intent(CLICK_PLAY)
        val pPlayIntent = PendingIntent.getBroadcast(context, 0, playIntent, flags)
        contentView.setOnClickPendingIntent(R.id.play, pPlayIntent)

        val pauseIntent = Intent(CLICK_PAUSE)
        val pPauseIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, flags)
        contentView.setOnClickPendingIntent(R.id.pause, pPauseIntent)

        val revIntent = Intent(CLICK_REV)
        val pRevIntent = PendingIntent.getBroadcast(context, 0, revIntent, flags)
        contentView.setOnClickPendingIntent(R.id.rev, pRevIntent)

        val fwdIntent = Intent(CLICK_FWD)
        val pFwdIntent = PendingIntent.getBroadcast(context, 0, fwdIntent, flags)
        contentView.setOnClickPendingIntent(R.id.fwd, pFwdIntent)

        val mainActivityIntent = Intent(context, MusicTransmitterActivity::class.java)
        val contentIntent =
            PendingIntent.getActivity(context, 0, mainActivityIntent, flags)

        val notification = builder
            .setTicker("JatxMusicTransmitter")
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCustomBigContentView(contentView)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun hideNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
