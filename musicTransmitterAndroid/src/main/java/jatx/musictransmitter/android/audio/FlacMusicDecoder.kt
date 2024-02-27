package jatx.musictransmitter.android.audio

import android.util.Log
import io.nayuki.flac.common.StreamInfo
import io.nayuki.flac.decode.FlacDecoder
import jatx.debug.logError
import jatx.musiccommons.frame.FRAME_RATES
import jatx.musiccommons.frame.Frame
import jatx.musiccommons.frame.WrongFrameException
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.domain.FileDoesNotExistException
import org.jaudiotagger.audio.AudioFileIO
import java.io.*


class FlacMusicDecoder: MusicDecoder() {
    private var decoder: FlacDecoder? = null
    private var streamInfo: StreamInfo? = null
    private var msPerFrame = 0f

    private var freq = 0
    private var channels = 0
    private var depth = 0
    private var numSamples = 0L

    override var file: File? = null
        set(value) {
            if (value != null && value.absolutePath == MIC_PATH) {
                field = value
                return
            }

            if (value == null || !value.exists()) {
                throw MusicDecoderException(FileDoesNotExistException())
            }

            if (value.extension != "flac") {
                throw MusicDecoderException("File extension is not flac")
            }

            field = value

            try {
                val af = AudioFileIO.read(value)
                trackLengthSec = af.audioHeader.trackLength
            } catch (e: Exception) {
                logError(e)
            }

            decoder = FlacDecoder(value)
            while (decoder?.readAndHandleMetadataBlock() != null) {}
            streamInfo = decoder?.streamInfo
            numSamples = streamInfo?.numSamples ?: 0L
            if (numSamples == 0L) {
                throw MusicDecoderException("Unknown audio length")
            }

            streamInfo?.apply {
                Log.e("sampleRate", sampleRate.toString())
                Log.e("sampleDepth", sampleDepth.toString())
                Log.e("numChannels", numChannels.toString())
            }

            currentMs = 0f
            resetTimeFlag = true
        }

    @Synchronized
    override fun readFrame(): Frame {
        val position = position

        if (streamInfo == null) {
            throw MusicDecoderException("streamInfo is null")
        }

        freq = streamInfo!!.sampleRate
        channels = streamInfo!!.numChannels
        depth = streamInfo!!.sampleDepth

        var wrongRate = true
        for (rate in FRAME_RATES) {
            if (rate == freq) wrongRate = false
        }
        if (wrongRate) throw WrongFrameException("(player) wrong frame rate: $freq")

        val bytesPerSample = depth / 8
        val outStream = ByteArrayOutputStream(65536 * channels * bytesPerSample)

        val samples = Array(channels) {
            IntArray(
                65536
            )
        }
        val blockSamples = decoder?.let {
            try {
                it.readAudioBlock(samples, 0)
            } catch (e: Exception) {
                logError(e)
                0
            }
        } ?: 0

        when (channels) {
            2 -> {
                for (i in 0 until blockSamples) {
                    for (ch in 0 until channels) {
                        val value = samples[ch][i]
                        when (bytesPerSample) {
                            4 -> {
                                var j = 0
                                val value16 = value shr 16
                                while (j < 2) {
                                    outStream.write(value16 shr (j * 8) and 0xFF)
                                    j++
                                }
                            }
                            3 -> {
                                var j = 0
                                val value16 = value shr 8
                                while (j < 2) {
                                    outStream.write(value16 shr (j * 8) and 0xFF)
                                    j++
                                }
                            }
                            else -> {
                                var j = 0
                                while (j < bytesPerSample) {
                                    outStream.write(value shr (j * 8) and 0xFF)
                                    j++
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                throw WrongFrameException("(player) mono sound")
            }
            else -> {
                throw WrongFrameException("(player) $channels channels")
            }
        }

        msPerFrame = blockSamples * 1e3.toFloat() / freq
        msReadFromFile += msPerFrame
        msSentToReceiver += msPerFrame
        currentMs += msPerFrame

        if (msPerFrame == 0f) {
            throw TrackFinishException()
        }

        val data = outStream.toByteArray()
        return Frame(data.size, freq, channels, depth, position, data)
    }

    @Synchronized
    override fun seek(progress: Double) {
        val samples = Array(channels) {
            IntArray(
                65536
            )
        }
        val seekPosition = (numSamples * progress).toLong()
        decoder?.seekAndReadAudioBlock(seekPosition, samples, 0)
        msReadFromFile = msPerFrame * (trackLengthSec * 1000f * progress / msPerFrame).toInt()
        currentMs = msPerFrame * (trackLengthSec * 1000f * progress / msPerFrame).toInt()

        resetTimeFlag = true
    }
}