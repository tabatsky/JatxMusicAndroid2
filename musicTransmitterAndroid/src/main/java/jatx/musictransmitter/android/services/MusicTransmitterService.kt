package jatx.musictransmitter.android.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.data.Settings
import jatx.musictransmitter.android.audio.JLayerMp3Decoder
import jatx.musictransmitter.android.extensions.showToast
import jatx.musictransmitter.android.threads.TimeUpdater
import jatx.musictransmitter.android.threads.TransmitterController
import jatx.musictransmitter.android.threads.TransmitterPlayer
import jatx.musictransmitter.android.threads.UIController
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Inject

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

const val EXTRA_WIFI_STATUS = "isWifiOk"

const val KEY_POSITION = "position"
const val KEY_PROGRESS = "progress"
const val KEY_VOLUME = "volume"
const val KEY_CURRENT_MS = "currentMs"
const val KEY_TRACK_LENGTH_MS = "trackLengthMs"

class MusicTransmitterService: Service() {
    @Inject
    lateinit var settings: Settings

    private lateinit var tu: TimeUpdater
    private lateinit var tc: TransmitterController
    private lateinit var tp: TransmitterPlayer

    @Volatile
    private var position = 0
    @Volatile
    private var wifiStatus = false
    @Volatile
    private var currentMs = 0f
    @Volatile
    private var trackLengthMs = 0f

    private val uiController = object: UIController {
        override fun setWifiStatus(status: Boolean) {
            wifiStatus = status
            val intent = Intent(SET_WIFI_STATUS)
            intent.putExtra(EXTRA_WIFI_STATUS, status)
            sendBroadcast(intent)
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

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("service", "started")

        App.appComponent?.injectMusicTransmitterService(this)

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

    override fun onDestroy() {
        tu.interrupt()
        tc.interrupt()
        tp.interrupt()

        stopForeground(true)
        super.onDestroy()
    }

    private fun prepareAndStart(intent: Intent?) {
        initBroadcastReceivers()

        val decoder = JLayerMp3Decoder()

        tu = TimeUpdater(uiController, decoder)
        tc = TransmitterController(settings.volume)
        tp = TransmitterPlayer(uiController, decoder)

        tc.tp = tp
        tp.tc = tc

        tp.files = settings.currentFileList

        tu.start()
        tc.start()
        tp.start()
    }

    private fun initBroadcastReceivers() {
        val stopSelfReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopSelf()
            }
        }
        registerReceiver(stopSelfReceiver, IntentFilter(STOP_SERVICE))

        val tpSetPositionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val position = intent.getIntExtra(KEY_POSITION, 0)
                tp.position = position
            }
        }
        registerReceiver(tpSetPositionReceiver, IntentFilter(TP_SET_POSITION))

        val tpPlayReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tp.play()
                tc.play()
            }
        }
        registerReceiver(tpPlayReceiver, IntentFilter(TP_AND_TC_PLAY))

        val tpPauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tp.pause()
                tc.pause()
            }
        }
        registerReceiver(tpPauseReceiver, IntentFilter(TP_AND_TC_PAUSE))

        val tpSeekReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val progress = intent.getDoubleExtra(KEY_PROGRESS, 0.0)
                tp.seek(progress)
            }
        }
        registerReceiver(tpSeekReceiver, IntentFilter(TP_SEEK))

        val tpSetFileListReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                tp.files = settings.currentFileList
            }
        }
        registerReceiver(tpSetFileListReceiver, IntentFilter(TP_SET_FILE_LIST))

        val tcSetVolumeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val volume = intent.getIntExtra("volume", 0)
                tc.setVolume(volume)
            }
        }
        registerReceiver(tcSetVolumeReceiver, IntentFilter(TC_SET_VOLUME))
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