package jatx.musictransmitter.android.audio

import jatx.debug.logError
import jatx.musiccommons.Frame
import jatx.musiccommons.WrongFrameException
import jatx.musiccommons.frameFromSampleBuffer
import jatx.musictransmitter.android.data.FileDoesNotExistException
import jatx.musictransmitter.android.data.MIC_PATH
import javazoom.jl.decoder.*
import org.jaudiotagger.audio.AudioFileIO
import java.io.*

class JLayerMp3Decoder : MusicDecoder() {
    private var decoder: Decoder? = null
    private var bitstream: Bitstream? = null
    private var msFrame = 0f

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
            msFrame = frameHeader.ms_per_frame()
            msRead += msFrame
            msTotal += msFrame
            currentMs += msFrame
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
                    msFrame = frameHeader.ms_per_frame()
                    msRead += msFrame
                    currentMs += msFrame
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