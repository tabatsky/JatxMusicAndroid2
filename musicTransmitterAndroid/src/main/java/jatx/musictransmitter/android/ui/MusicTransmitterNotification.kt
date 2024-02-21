package jatx.musictransmitter.android.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import jatx.musictransmitter.android.R

const val CHANNEL_ID = "jatxMusicTransmitter"
const val CHANNEL_NAME = "jatxMusicTransmitter"

const val CLICK_PLAY = "jatx.musictransmitter.android.CLICK_PLAY"
const val CLICK_PAUSE = "jatx.musictransmitter.android.CLICK_PAUSE"
const val CLICK_REV = "jatx.musictransmitter.android.CLICK_REV"
const val CLICK_FWD = "jatx.musictransmitter.android.CLICK_FWD"

const val NOTIFICATION_ID = 1237

object MusicTransmitterNotification {
    fun showNotification(context: Context, artist: String, title: String, albumArt: Bitmap, isPlaying: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val contentView = RemoteViews(context.packageName, R.layout.notification)

        contentView.setTextViewText(R.id.text_title, title)
        contentView.setTextViewText(R.id.text_artist, artist)

        contentView.setBitmap(R.id.img_album_art, "setImageBitmap", albumArt)

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
            .setChannelId(CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .setOngoing(true)
            .setTicker("JatxMusicTransmitter")
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setOngoing(true)
            .setCustomBigContentView(contentView)
            .setContent(contentView)
            .setContentTitle("JatxMusicTransmitter")
            .build()

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            Toast
                .makeText(
                    context,
                    R.string.toast_please_enable_notifications,
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }

    fun hideNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
