package jatx.musictransmitter.android.threads

import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class TransmitterPlayerWorker(
    private val s: Socket?
): Thread() {
    @Volatile
    var threadId = 0L

    @Volatile
    var finishWorkerFlag = false

    var onWorkerStopped: () -> Unit = {}

    private var os: OutputStream? = null

    fun writeData(data: ByteArray) {
        try {
            os?.write(data)
            os?.flush()
        } catch (e: IOException) {
            println("(player $threadId) write data error")
        }
    }

    override fun run() {
        threadId = currentThread().id
        try {
            os = s?.getOutputStream()
            println("(player $threadId) socket connect")
            try {
                while (!finishWorkerFlag) {
                    sleep(10)
                }
            } catch (e: InterruptedException) {
                println("(player $threadId) interrupted")
            }
        } catch (e: IOException) {
            println("(player) $threadId socket disconnect")
        } finally {
            sleep(250)
            os?.close()
            println("(player $threadId) output stream closed")
            onWorkerStopped()
            println("(player $threadId) finished")
        }
    }
}