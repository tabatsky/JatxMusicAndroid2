package jatx.musictransmitter.android.threads

import jatx.debug.logError
import jatx.musiccommons.frame.WrongFrameException
import jatx.musiccommons.frame.frameToByteArray
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.audio.*
import jatx.musictransmitter.android.data.MIC_PATH
import java.io.File
import java.io.IOException

class TransmitterPlayer(
    @Volatile private var uiController: UIController
): Thread() {
    @Volatile var microphoneOk = false

    @Volatile var isNetworkingMode = false

    @Volatile
    var count = 0
    @Volatile
    var isPlaying = false

    @Volatile
    var path: String? = null

    @Volatile
    var files: List<File> = listOf()
        set(value) {
            field = value
            count = value.size
        }

    @Volatile
    var position = 0
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
    var startTime: Long = 0
    @Volatile
    var currentTime: Long = 0
    @Volatile
    var deltaTimeExtraSentToReceiver = 0f

    var tk: ThreadKeeper? = null

    override fun run() {
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

    fun play() {
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

    fun pause() {
        isPlaying = false
        if (path == MIC_PATH) {
            Microphone.stop()
            microphoneOk = false
        }
    }

    fun seek(progress: Double) {
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
                    nextTrack()
                    sleep(200)
                    null
                } catch (e: WrongFrameException) {
                    println("(player) wrong frame")
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