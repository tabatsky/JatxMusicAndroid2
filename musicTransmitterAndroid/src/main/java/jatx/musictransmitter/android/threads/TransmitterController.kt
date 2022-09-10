package jatx.musictransmitter.android.threads

import jatx.debug.logError
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

const val CONNECT_PORT_CONTROLLER = 7172

const val COMMAND_EMPTY = 255.toByte()
const val COMMAND_STOP = 127.toByte()
const val COMMAND_PAUSE = 126.toByte()
const val COMMAND_PLAY = 125.toByte()

const val SO_TIMEOUT = 1000

class TransmitterController(
    initialVolume: Int
) : Thread() {
    @Volatile
    var volume: Int = initialVolume
        set(value) {
            println("(controller) set volume: " + Integer.valueOf(value).toString())
            workers.values.forEach { it.volume = value }
            field = value
        }

    @Volatile
    var finishFlag = false

    @Volatile
    var tk: ThreadKeeper? = null

    private var ss: ServerSocket? = null

    @Volatile
    private var workers = ConcurrentHashMap<String, TransmitterControllerWorker>()

    fun play() {
        println("(controller) play")
        workers.values.forEach { it.play() }
    }

    fun pause() {
        println("(controller) pause")
        workers.values.forEach { it.pause() }
    }

    override fun run() {
        println("(controller) thread started")
        try {
            while (!finishFlag) {
                sleep(100)
                ss = ServerSocket(CONNECT_PORT_CONTROLLER)
                println("(controller) new server socket")
                try {
                    ss?.soTimeout = SO_TIMEOUT
                    val s = ss?.accept()
                    println("(controller) server socket accept")
                    s?.inetAddress?.hostAddress?.let {
                        val worker = TransmitterControllerWorker(s, it, tk)
                        worker.start()
                        println("(controller) worker $it started")
                        workers[it] = worker
                        worker.onWorkerStopped = {
                            println("(controller) worker $it stopped")
                            workers.remove(it)
                        }
                    }
                    val vol = volume
                    volume = vol
                } catch (e: SocketTimeoutException) {
                    println("(controller) socket timeout")
                } finally {
                    ss?.close()
                    println("(controller) server socket closed")
                }
            }
        } catch (e: IOException) {
            logError(e)
        } catch (e: InterruptedException) {
            println("(controller) thread interrupted")
            workers.values.forEach {
                it.finishWorkerFlag = true
            }
            println("(controller) workers")
            ss?.close()
            println("(controller) server socket closed")
        } finally {
            println("(controller) thread finished")
        }
    }
}