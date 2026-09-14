package jatx.musictransmitter.android.audio

import jatx.musiccommons.frame.Frame
import java.io.File
import java.lang.Exception

abstract class MusicDecoder {
    companion object {
        var resetTimeFlag = true

        var position = 0

        fun setPath(path: String) {
            println("set path: $path")
            val file = File(path)
            when (file.extension) {
                "mp3" -> {
                    INSTANCE = JLayerMp3Decoder()
                }
                "flac" -> {
                    INSTANCE = FlacMusicDecoder()
                }
            }
            INSTANCE?.file = file
        }

        var INSTANCE: MusicDecoder? = null
    }

    var msReadFromFile = 0f
    var msSentToReceiver = 0f

    @Volatile var currentMs = 0f
    @Volatile var trackLengthSec = 0

    abstract var file: File?

    abstract fun readFrame(): Frame?
    abstract fun seek(progress: Double)
}

class MusicDecoderException: Exception {
    constructor(cause: Exception): super(cause)
    constructor(msg: String): super(msg)
}

class TrackFinishException: Exception()