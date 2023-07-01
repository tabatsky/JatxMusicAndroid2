package jatx.musictransmitter.android.threads

import jatx.debug.logError
import jatx.musiccommons.WrongFrameException
import jatx.musiccommons.frameToByteArray
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.audio.*
import jatx.musictransmitter.android.data.MIC_PATH
import java.io.File
import java.io.IOException

class TransmitterPlayer(
    @Volatile private var uiController: UIController
): Thread() {
    @Volatile var microphoneOk = false

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
    var t1: Long = 0
    @Volatile
    var t2: Long = 0
    @Volatile
    var dt = 0f

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
        var data: ByteArray?

        t1 = System.currentTimeMillis()
        t2 = t1
        dt = 0f

        while (true) {
            if (isPlaying) {
                if (MusicDecoder.resetTimeFlag) {
                    do {
                        t2 = System.currentTimeMillis()
                        dt = (MusicDecoder.INSTANCE?.msSentToReceiver ?: 0f) - (t2 - t1)
                        sleep(10)
                    } while (dt > 0)
                    MusicDecoder.INSTANCE?.msReadFromFile = 0f
                    MusicDecoder.INSTANCE?.msSentToReceiver = 0f
                    t1 = System.currentTimeMillis()
                    t2 = t1
                    MusicDecoder.resetTimeFlag = false
                }
                if (MusicDecoder.disconnectResetTimeFlag) {
                    t1 = System.currentTimeMillis()
                    t2 = t1
                    MusicDecoder.INSTANCE?.msReadFromFile = 0f
                    MusicDecoder.INSTANCE?.msSentToReceiver = 0f
                    MusicDecoder.disconnectResetTimeFlag = false
                }
                try {
                    data = if (path == MIC_PATH) {
                        if (microphoneOk)
                            Microphone.readFrame(position)?.let {
                                frameToByteArray(it)
                            }
                        else
                            null
                    } else {
                        MusicDecoder.INSTANCE?.readFrame()?.let {
                            frameToByteArray(it)
                        }
                    }
                } catch (e: MusicDecoderException) {
                    println("(player) decoder exception")
                    data = null
                    sleep(200)
                } catch (e: MicrophoneReadException) {
                    println("(player) microphone read exception")
                    data = null
                    sleep(200)
                } catch (e: TrackFinishException) {
                    println("(player) track finish")
                    nextTrack()
                    data = null
                    sleep(200)
                } catch (e: WrongFrameException) {
                    println("(player) wrong frame")
                    nextTrack()
                    data = null
                    sleep(200)
                }
                if (data != null) {
                    tk?.tpck?.writeData(data)
                }
            } else {
                sleep(10)
                MusicDecoder.INSTANCE?.msReadFromFile = 0f
                MusicDecoder.INSTANCE?.msSentToReceiver = 0f
                t1 = System.currentTimeMillis()
                t2 = t1
                dt = 0f
            }
            if ((MusicDecoder.INSTANCE?.msReadFromFile ?: 0f) > 300) {
                do {
                    t2 = System.currentTimeMillis()
                    dt = (MusicDecoder.INSTANCE?.msSentToReceiver ?: 0f) - (t2 - t1)
                    sleep(10)
                } while (dt > 200)
                MusicDecoder.INSTANCE?.msReadFromFile = 0f
            }
        }
    }

    private fun nextTrack() {
        uiController.nextTrack()
    }
}