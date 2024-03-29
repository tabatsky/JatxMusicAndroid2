package jatx.musiccommons.frame

import java.io.IOException
import java.io.InputStream

const val FRAME_HEADER_SIZE = 64
val FRAME_RATES = intArrayOf(32000, 44100, 48000, 96000, 192000)

data class Frame(
    val size: Int,
    val freq: Int,
    val channels: Int,
    val depth: Int,
    val position: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        return (other is Frame) &&
                (other.size == size) &&
                (other.freq == freq) &&
                (other.channels == channels) &&
                (other.depth == depth) &&
                (other.position == position) &&
                (other.data.contentEquals(data))
    }

    override fun hashCode(): Int {
        return size.hashCode() +
                freq.hashCode() +
                channels.hashCode() +
                depth.hashCode() +
                position.hashCode() +
                data.contentHashCode()
    }
}

fun frameToByteArray(frame: Frame): ByteArray {
    require(frame.size == frame.data.size)

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
    val dpth = (frame.depth and 0xff).toByte()

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
    result[9] = dpth

    result[12] = pos1
    result[13] = pos2
    result[14] = pos3
    result[15] = pos4

    return result
}

@Throws(IOException::class, InterruptedException::class)
fun frameFromInputStream(inputStream: InputStream): Frame {
    var freq1 = 0
    var freq2 = 0
    var freq3 = 0
    var freq4 = 0

    var size1 = 0
    var size2 = 0
    var size3 = 0
    var size4 = 0

    var pos1 = 0
    var pos2 = 0
    var pos3 = 0
    var pos4 = 0

    var channels = 0
    var depth = 0

    val header = ByteArray(1)
    var bytesRead = 0
    while (bytesRead < FRAME_HEADER_SIZE) {
        if (inputStream.available() > 0) {
            val justRead = inputStream.read(header, 0, 1)
            if (justRead > 0) {
                when (bytesRead) {
                    0 -> {
                        size1 = header[0].toInt() and 0xff
                    }
                    1 -> {
                        size2 = header[0].toInt() and 0xff
                    }
                    2 -> {
                        size3 = header[0].toInt() and 0xff
                    }
                    3 -> {
                        size4 = header[0].toInt() and 0xff
                    }
                    4 -> {
                        freq1 = header[0].toInt() and 0xff
                    }
                    5 -> {
                        freq2 = header[0].toInt() and 0xff
                    }
                    6 -> {
                        freq3 = header[0].toInt() and 0xff
                    }
                    7 -> {
                        freq4 = header[0].toInt() and 0xff
                    }
                    8 -> {
                        channels = header[0].toInt() and 0xff
                    }
                    9 -> {
                        depth = header[0].toInt() and 0xff
                    }
                    12 -> {
                        pos1 = header[0].toInt() and 0xff
                    }
                    13 -> {
                        pos2 = header[0].toInt() and 0xff
                    }
                    14 -> {
                        pos3 = header[0].toInt() and 0xff
                    }
                    15 -> {
                        pos4 = header[0].toInt() and 0xff
                    }
                }
                bytesRead += justRead
            }
        } else {
            Thread.sleep(20)
        }
    }

    val size = size1 shl 24 or (size2 shl 16) or (size3 shl 8) or size4
    val freq = freq1 shl 24 or (freq2 shl 16) or (freq3 shl 8) or freq4
    val pos = pos1 shl 24 or (pos2 shl 16) or (pos3 shl 8) or pos4

    bytesRead = 0
    val data = ByteArray(size)
    while (bytesRead < size) {
        if (inputStream.available() > 0) {
            val justRead = inputStream.read(data, bytesRead, size - bytesRead)
            if (justRead > 0) {
                bytesRead += justRead
            }
        } else {
            Thread.sleep(20)
        }
    }

    return Frame(size, freq, channels, depth, pos, data)
}

@Throws(IOException::class, InterruptedException::class)
fun frameFromRawData(rawData: ByteArray) = frameFromInputStream(rawData.inputStream())

class WrongFrameException(msg: String): Exception(msg)