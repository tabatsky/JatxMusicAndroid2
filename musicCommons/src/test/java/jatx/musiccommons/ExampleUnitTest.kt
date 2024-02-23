package jatx.musiccommons

import jatx.musiccommons.frame.Frame
import jatx.musiccommons.frame.frameFromInputStream
import jatx.musiccommons.frame.frameToByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.IllegalArgumentException
import kotlin.random.Random

class ExampleUnitTest {
    @Test
    fun frameWritingAndReading_isCorrect() {
        val data = Random.nextBytes(4096)
        val frame = Frame(
            4096,
            44100,
            2,
            16,
            137,
            data
        )
        val frame2 = Frame(
            2048,
            44100,
            2,
            16,
            137,
            data
        )

        val inputStream = PipedInputStream()
        val outputStream = PipedOutputStream(inputStream)

        object : Thread() {
            override fun run() {
                frameToByteArray(frame).let {
                    outputStream.write(it)
                    outputStream.flush()
                    outputStream.write(it)
                    outputStream.flush()
                }
                assertThrows(IllegalArgumentException::class.java) {
                    frameToByteArray(frame2).let {
                        outputStream.write(it)
                        outputStream.flush()
                    }
                }
            }
        }.start()
        val frame3 = frameFromInputStream(inputStream)
        val frame4 = frameFromInputStream(inputStream)
        outputStream.close()
        inputStream.close()

        assert(frame == frame3)
        assert(frame == frame4)

        val frame6 = Frame(
            4096,
            48000,
            8,
            24,
            1523,
            data
        )
        assert(frame != frame6)
    }
}