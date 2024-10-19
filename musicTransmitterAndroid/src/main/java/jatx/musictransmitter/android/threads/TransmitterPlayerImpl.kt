package jatx.musictransmitter.android.threads

import android.util.Log
import jatx.debug.logError
import jatx.musiccommons.frame.WrongFrameException
import jatx.musiccommons.frame.frameToByteArray
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.audio.*
import jatx.musictransmitter.android.data.MIC_PATH
import java.io.File
import java.io.IOException

abstract class TransmitterPlayer: Thread() {
    abstract var microphoneOk: Boolean
    abstract var isNetworkingMode: Boolean
    abstract var count: Int
    abstract var isPlaying: Boolean
    abstract var path: String?
    abstract var files: List<File>
    abstract var position: Int
    abstract var startTime: Long
    abstract var currentTime: Long
    abstract var deltaTimeExtraSentToReceiver: Float
    abstract var tk: ThreadKeeper?
    abstract fun play()
    abstract fun pause()
    abstract fun seek(progress: Double)
}

class TransmitterPlayerImpl(
    @Volatile private var uiController: UIController
): TransmitterPlayer() {
    @Volatile
    override var microphoneOk = false

    @Volatile
    override var isNetworkingMode = false

    @Volatile
    override var count = 0
    @Volatile
    override var isPlaying = false

    @Volatile
    override var path: String? = null

    @Volatile
    override var files: List<File> = listOf()
        set(value) {
            field = value
            count = value.size
        }

    @Volatile
    override var position = 0
        set(value) {
            pause()

            if (value < 0 || count <= 0) return

            field = if (value >= count) {
                0
            } else {
                value
            }

            println("(player) position: $field")

            path = files[field].absolutePath
            println("(player) path: $path")

            try {
                MusicDecoder.setPath(path!!)
                MusicDecoder.position = field
            } catch (e: MusicDecoderException) {
                logError(e)
            }

            play()
        }

    @Volatile
    override var startTime: Long = 0
    @Volatile
    override var currentTime: Long = 0
    @Volatile
    override var deltaTimeExtraSentToReceiver = 0f

    override var tk: ThreadKeeper? = null

    override fun run() {
        Log.e("starting","transmitter player")
        try {
            translateMusic()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            println("(player) thread finished")
        }
    }

    override fun play() {
        isPlaying = true
        if (path == MIC_PATH) {
            try {
                Microphone.start()
                microphoneOk = true
            } catch (e: MicrophoneInitException) {
                logError(e)
                microphoneOk = false
                uiController.errorMsg(R.string.toast_cannot_init_microphone)
            }
        }
    }

    override fun pause() {
        isPlaying = false
        if (path == MIC_PATH) {
            Microphone.stop()
            microphoneOk = false
        }
    }

    override fun seek(progress: Double) {
        val needToPlay = isPlaying
        pause()
        try {
            MusicDecoder.INSTANCE?.seek(progress)
        } catch (e: MusicDecoderException) {
            logError(e)
        } catch (e: Exception) {
            logError(e)
        }
        if (needToPlay) play()
    }

    private fun translateMusic(): Nothing {
        startTime = System.currentTimeMillis()
        currentTime = startTime
        deltaTimeExtraSentToReceiver = 0f

        while (true) {
            if (isPlaying) {
                if (MusicDecoder.resetTimeFlag) {
                    resetTimeWithFlushingSentToReceiver()
                }
                val data = try {
                    if (path == MIC_PATH) {
                        tryReadFrameFromMicrophone()
                    } else {
                        tryReadFrameFromDecoder()
                    }
                } catch (e: MusicDecoderException) {
                    println("(player) decoder exception")
                    sleep(200)
                    null
                } catch (e: MicrophoneReadException) {
                    println("(player) microphone read exception")
                    sleep(200)
                    null
                } catch (e: TrackFinishException) {
                    println("(player) track finish")
                    pause()
                    nextTrack()
                    sleep(200)
                    null
                } catch (e: WrongFrameException) {
                    println("(player) wrong frame")
                    pause()
                    nextTrack()
                    sleep(200)
                    null
                }
                if (data != null) {
                    tk?.tpda?.writeData(data)
                }
            } else {
                sleep(10)
                resetTimeIfNotPlaying()
            }
            flushSentToReceiverEvery300msOfReadFromFile()
        }
    }

    private fun resetTimeWithFlushingSentToReceiver() {
        do {
            currentTime = System.currentTimeMillis()
            deltaTimeExtraSentToReceiver = (MusicDecoder.INSTANCE?.msSentToReceiver ?: 0f) - (currentTime - startTime)
            sleep(10)
        } while (deltaTimeExtraSentToReceiver > 0)
        MusicDecoder.INSTANCE?.msReadFromFile = 0f
        MusicDecoder.INSTANCE?.msSentToReceiver = 0f
        startTime = System.currentTimeMillis()
        currentTime = startTime
        MusicDecoder.resetTimeFlag = false
    }

    private fun resetTimeIfNotPlaying() {
        MusicDecoder.INSTANCE?.msReadFromFile = 0f
        MusicDecoder.INSTANCE?.msSentToReceiver = 0f
        startTime = System.currentTimeMillis()
        currentTime = startTime
        deltaTimeExtraSentToReceiver = 0f
    }

    private fun flushSentToReceiverEvery300msOfReadFromFile() {
        val multiplier = if (isNetworkingMode) {
            10
        } else {
            1
        }
        if ((MusicDecoder.INSTANCE?.msReadFromFile ?: 0f) > 30 * multiplier) {
            do {
                currentTime = System.currentTimeMillis()
                deltaTimeExtraSentToReceiver = (MusicDecoder.INSTANCE?.msSentToReceiver ?: 0f) - (currentTime - startTime)
                sleep(10)
            } while (deltaTimeExtraSentToReceiver > 20 * multiplier)
            MusicDecoder.INSTANCE?.msReadFromFile = 0f
        }
    }

    private fun tryReadFrameFromMicrophone() = microphoneOk.takeIf { it }?.let {
        Microphone.readFrame(position)?.let {
            frameToByteArray(it)
        }
    }

    private fun tryReadFrameFromDecoder() = MusicDecoder.INSTANCE?.readFrame()?.let {
        frameToByteArray(it)
    }


    private fun nextTrack() {
        uiController.nextTrack()
    }
}