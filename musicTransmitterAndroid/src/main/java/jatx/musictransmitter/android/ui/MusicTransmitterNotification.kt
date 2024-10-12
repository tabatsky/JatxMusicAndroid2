package jatx.musictransmitter.android.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.services.MusicTransmitterService

const val CHANNEL_ID = "jatxMusicTransmitter"
const val CHANNEL_NAME = "jatxMusicTransmitter"

const val CLICK_PLAY = "jatx.musictransmitter.android.CLICK_PLAY"
const val CLICK_PAUSE = "jatx.musictransmitter.android.CLICK_PAUSE"
const val CLICK_REW = "jatx.musictransmitter.android.CLICK_REW"
const val CLICK_FWD = "jatx.musictransmitter.android.CLICK_FWD"

const val NOTIFICATION_ID = 1237

object MusicTransmitterNotification {
    fun showNotification(context: Context, artist: String, title: String, albumArt: Bitmap, isPlaying: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)

        val flags = if (Build.VERSION.SDK_INT < 23) {
            0
        } else {
            PendingIntent.FLAG_IMMUTABLE
        }

        val playIntent = Intent(CLICK_PLAY)
        val pPlayIntent = PendingIntent.getBroadcast(context, 0, playIntent, flags)

        val pauseIntent = Intent(CLICK_PAUSE)
        val pPauseIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, flags)

        val revIntent = Intent(CLICK_REW)
        val pRevIntent = PendingIntent.getBroadcast(context, 0, revIntent, flags)

        val fwdIntent = Intent(CLICK_FWD)
        val pFwdIntent = PendingIntent.getBroadcast(context, 0, fwdIntent, flags)

        val mainActivityIntent = Intent(context, MusicTransmitterActivity::class.java)
        val contentIntent =
            PendingIntent.getActivity(context, 0, mainActivityIntent, flags)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(MusicTransmitterService.mediaSessionCompat.sessionToken)

        val notification = builder
            .setChannelId(CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(albumArt)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(R.drawable.ic_rew_vector, "Rew", pRevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause_vector else R.drawable.ic_play_vector,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) pPauseIntent else pPlayIntent)
            .addAction(R.drawable.ic_fwd_vector, "Fwd", pFwdIntent)
            .setStyle(
                mediaStyle)
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
