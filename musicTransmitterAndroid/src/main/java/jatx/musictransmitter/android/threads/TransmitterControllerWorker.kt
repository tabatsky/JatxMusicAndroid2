package jatx.musictransmitter.android.threads

import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class TransmitterControllerWorker(
    private val s: Socket?,
    private val host: String,
    private val tk: ThreadKeeper?
): Thread() {
    @Volatile
    var threadId = 0L
    @Volatile
    var finishWorkerFlag = false
    @Volatile
    var fifo: BlockingQueue<Byte> = ArrayBlockingQueue(2048)

    @Volatile
    var volume: Int = 0
        set(value) {
            println("(controller $threadId) set volume: " + Integer.valueOf(value).toString())
            if (value in 0..100) {
                fifo.offer(value.toByte())
            }
            field = value
        }

    var onWorkerStopped: () -> Unit = {}

    private var os: OutputStream? = null

    fun play() {
        println("(controller $threadId) play")
        fifo.offer(COMMAND_PLAY)
    }

    fun pause() {
        println("(controller $threadId) pause")
        fifo.offer(COMMAND_PAUSE)
    }

    override fun run() {
        threadId = currentThread().id
        try {
            os = s?.getOutputStream()
            println("(controller $threadId) socket connect")
            while (!finishWorkerFlag) {
                val cmd = fifo.poll() ?: COMMAND_EMPTY
                val data = byteArrayOf(cmd)
                os?.write(data)
                os?.flush()
                sleep(10)
            }
            val data = byteArrayOf(COMMAND_STOP)
            os?.write(data)
            os?.flush()
        } catch (e: IOException) {
            println("(controller $threadId) socket disconnect")
            println("(controller $threadId) " + Date().time % 10000)
        } finally {
            tk?.tpck?.getWorkerByHost(host)?.finishWorkerFlag = true
            sleep(250)
            os?.close()
            println("(controller $threadId) output stream closed")
            onWorkerStopped()
            println("(controller $threadId) finished")
        }
    }
}