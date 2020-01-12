package jatx.musiccommons

import javazoom.jl.decoder.SampleBuffer
import java.io.ByteArrayOutputStream
import java.util.*

const val FRAME_HEADER_SIZE = 64
val FRAME_RATES = intArrayOf(32000, 44100, 48000)

data class Frame(
    val size: Int,
    val freq: Int,
    val channels: Int,
    val position: Int,
    val data: ByteArray
)

fun frameToByteArray(frame: Frame?): ByteArray? {
    if (frame == null) return null

    val result = ByteArray(frame.size + FRAME_HEADER_SIZE)

    for (i in 0 until frame.size) {
        result[i + FRAME_HEADER_SIZE] = frame.data[i]
    }

    val freq1 = (frame.freq shr 24 and 0xff).toByte()
    val freq2 = (frame.freq shr 16 and 0xff).toByte()
    val freq3 = (frame.freq shr 8 and 0xff).toByte()
    val freq4 = (frame.freq shr 0 and 0xff).toByte()

    val size1 = (frame.size shr 24 and 0xff).toByte()
    val size2 = (frame.size shr 16 and 0xff).toByte()
    val size3 = (frame.size shr 8 and 0xff).toByte()
    val size4 = (frame.size shr 0 and 0xff).toByte()

    val pos1 = (frame.position shr 24 and 0xff).toByte()
    val pos2 = (frame.position shr 16 and 0xff).toByte()
    val pos3 = (frame.position shr 8 and 0xff).toByte()
    val pos4 = (frame.position shr 0 and 0xff).toByte()

    val ch = (frame.channels and 0xff).toByte()

    for (i in 0 until FRAME_HEADER_SIZE) {
        result[i] = 0x00.toByte()
    }

    result[0] = size1
    result[1] = size2
    result[2] = size3
    result[3] = size4

    result[4] = freq1
    result[5] = freq2
    result[6] = freq3
    result[7] = freq4

    result[8] = ch

    result[12] = pos1
    result[13] = pos2
    result[14] = pos3
    result[15] = pos4

    return result
}

fun frameFromSampleBuffer(sampleBuffer: SampleBuffer, position: Int): Frame {
    val position = position

    val outStream = ByteArrayOutputStream(10240)

    val freq = sampleBuffer.sampleFrequency
    val channels = sampleBuffer.channelCount

    val pcm = sampleBuffer.buffer

    var wrongRate = true

    for (rate in FRAME_RATES) {
        if (rate == freq) wrongRate = false
    }

    if (wrongRate) throw WrongFrameException("(player) wrong frame rate: $freq")

    if (channels == 2) {
        for (i in 0 until pcm.size / 2) {
            val shrt1 = pcm[2 * i]
            val shrt2 = pcm[2 * i + 1]
            outStream.write(shrt1.toInt() and 0xff)
            outStream.write(shrt1.toInt() shr 8 and 0xff)
            outStream.write(shrt2.toInt() and 0xff)
            outStream.write(shrt2.toInt() shr 8 and 0xff)
        }
    } else if (channels == 1) {
        throw WrongFrameException("(player) mono sound")
    } else {
        throw WrongFrameException("(player) $channels channels")
    }

    val data = outStream.toByteArray()

    return Frame(data.size, freq, channels, position, data)
}

fun frameFromMicRawData(rawData: ByteArray, dataSize: Int, position: Int, sampleRate: Int): Frame {
    return Frame(dataSize, sampleRate, 2, position, rawData.copyOf(dataSize))
}

class WrongFrameException(msg: String): Exception(msg)