package jatx.musictransmitter.android.audio

import jatx.debug.logError
import jatx.musiccommons.frame.FRAME_RATES
import jatx.musiccommons.frame.Frame
import jatx.musiccommons.frame.WrongFrameException
import jatx.musictransmitter.android.data.FileDoesNotExistException
import jatx.musictransmitter.android.data.MIC_PATH
import javazoom.jl.decoder.*
import org.jaudiotagger.audio.AudioFileIO
import java.io.*

class JLayerMp3Decoder : MusicDecoder() {
    private var decoder: Decoder? = null
    private var bitstream: Bitstream? = null
    private var msPerFrame = 0f

    override var file: File? = null
        set(value) {
            if (value != null && value.absolutePath == MIC_PATH) {
                field = value
                return
            }

            if (value == null || !value.exists()) {
                throw MusicDecoderException(FileDoesNotExistException())
            }

            if (value.extension != "mp3") {
                throw MusicDecoderException("File extension is not mp3")
            }

            field = value

            try {
                val af = AudioFileIO.read(value)
                trackLengthSec = af.audioHeader.trackLength
            } catch (e: Exception) {
                logError(e)
            }

            decoder = Decoder()

            bitstream?.apply {
                try {
                    this.close()
                } catch (e: BitstreamException) {
                    throw MusicDecoderException(e)
                }
                bitstream = null
            }

            try {
                val inputStream: InputStream = BufferedInputStream(FileInputStream(value), 32 * 1024)
                bitstream = Bitstream(inputStream)
            } catch (e: FileNotFoundException) {
                throw MusicDecoderException(e)
            }

            currentMs = 0f
            resetTimeFlag = true
        }

    override fun readFrame(): Frame? {
        var f: Frame? = null

        try {
            val frameHeader = (if (bitstream != null) {
                bitstream!!.readFrame()
            } else {
                throw MusicDecoderException("bitstream: null")
            })
                ?: throw TrackFinishException()
            msPerFrame = frameHeader.ms_per_frame()
            msReadFromFile += msPerFrame
            msSentToReceiver += msPerFrame
            currentMs += msPerFrame
            val output = try {
                decoder!!.decodeFrame(frameHeader, bitstream)
            } catch (e: ArrayIndexOutOfBoundsException) {
                bitstream!!.closeFrame()
                throw TrackFinishException()
            }
            if (output is SampleBuffer) {
                f = frameFromSampleBuffer(output, position)
            }
            bitstream!!.closeFrame()
        } catch (e: WrongFrameException) {
            throw e
        } catch (e: BitstreamException) {
            throw MusicDecoderException(e)
        } catch (e: DecoderException) {
            throw MusicDecoderException(e)
        }

        return f
    }

    override fun seek(progress: Double) {
        file = file
        try {
            while (currentMs < trackLengthSec * 1000.0 * progress) {
                val frameHeader = if (bitstream != null) {
                    bitstream!!.readFrame()
                } else {
                    throw MusicDecoderException("bitstream: null")
                }
                if (frameHeader != null) {
                    msPerFrame = frameHeader.ms_per_frame()
                    msReadFromFile += msPerFrame
                    currentMs += msPerFrame
                    bitstream!!.closeFrame()
                } else {
                    println("frame header is null $currentMs")
                    break
                }
            }
        } catch (e: BitstreamException) {
            throw MusicDecoderException(e)
        }

        resetTimeFlag = true
    }
}

private fun frameFromSampleBuffer(sampleBuffer: SampleBuffer, position: Int): Frame {
    val outStream = ByteArrayOutputStream(10240)

    val freq = sampleBuffer.sampleFrequency
    val channels = sampleBuffer.channelCount
    val depth = 16

    val pcm = sampleBuffer.buffer

    var wrongRate = true

    for (rate in FRAME_RATES) {
        if (rate == freq) wrongRate = false
    }

    if (wrongRate) throw WrongFrameException("(player) wrong frame rate: $freq")

    when (channels) {
        2 -> {
            for (i in 0 until pcm.size / 2) {
                val shrt1 = pcm[2 * i]
                val shrt2 = pcm[2 * i + 1]
                outStream.write(shrt1.toInt() and 0xff)
                outStream.write(shrt1.toInt() shr 8 and 0xff)
                outStream.write(shrt2.toInt() and 0xff)
                outStream.write(shrt2.toInt() shr 8 and 0xff)
            }
        }
        1 -> {
            throw WrongFrameException("(player) mono sound")
        }
        else -> {
            throw WrongFrameException("(player) $channels channels")
        }
    }

    val data = outStream.toByteArray()

    return Frame(data.size, freq, channels, depth, position, data)
}