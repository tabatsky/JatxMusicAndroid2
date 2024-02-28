package jatx.musictransmitter.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import jatx.musictransmitter.android.di.DaggerTestAppComponent
import jatx.musictransmitter.android.di.TestAppComponent
import jatx.musictransmitter.android.ui.CHANNEL_ID
import jatx.musictransmitter.android.ui.CHANNEL_NAME
import kotlin.properties.Delegates

class TestApp : Application() {
    companion object {
        var appComponent: TestAppComponent? = null
    }

    var notificationChannel by Delegates.notNull<NotificationChannel>()

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerTestAppComponent
            .builder()
            .context(this)
            .build()

        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            Log.e("can use lock screen", notificationManager.canUseFullScreenIntent().toString())
            val channel = NotificationChannel(CHANNEL_ID , CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
            notificationManager.createNotificationChannel(channel)
            notificationChannel = channel
        }
    }
}