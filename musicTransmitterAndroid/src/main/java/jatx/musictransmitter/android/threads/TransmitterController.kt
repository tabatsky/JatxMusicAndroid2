package jatx.musictransmitter.android.threads

import android.util.Log
import jatx.debug.logError
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

const val CONNECT_PORT_CONTROLLER = 7172

const val COMMAND_EMPTY = 255.toByte()
const val COMMAND_STOP = 127.toByte()
const val COMMAND_PAUSE = 126.toByte()
const val COMMAND_PLAY = 125.toByte()

const val SO_TIMEOUT = 1000

class TransmitterController(
    initialVolume: Int,
    private val isNetworkingMode: Boolean
) : Thread() {
    @Volatile
    var volume: Int = initialVolume
        set(value) {
            println("(controller) set volume: " + Integer.valueOf(value).toString())
            if (isNetworkingMode) {
                workers.values.forEach { it.volume = value }
            } else {
                localPlayer?.setVolume(value)
            }
            field = value
        }

    @Volatile
    var finishFlag = false

    @Volatile
    var tk: ThreadKeeper? = null

    private var ss: ServerSocket? = null

    @Volatile
    private var workers = ConcurrentHashMap<String, TransmitterControllerWorker>()

    private val localPlayer: LocalPlayer?
        get() = tk?.tpda as? LocalPlayer

    fun play() {
        println("(controller) play")
        if (isNetworkingMode) {
            workers.values.forEach { it.play() }
        } else {
            localPlayer?.play()
        }
    }

    fun pause() {
        println("(controller) pause")
        if (isNetworkingMode) {
            workers.values.forEach { it.pause() }
        } else {
            localPlayer?.pause()
        }
    }

    override fun run() {
        Log.e("starting","transmitter controller")
        val vol = volume
        volume = vol
        try {
            while (!finishFlag) {
                sleep(100)
                if (isNetworkingMode) {
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
            }
        } catch (e: IOException) {
            logError(e)
        } catch (e: InterruptedException) {
            println("(controller) thread interrupted")
        } finally {
            workers.values.forEach {
                it.finishWorkerFlag = true
            }
            println("(controller) workers finished")
            ss?.close()
            println("(controller) server socket closed")
            println("(controller) thread finished")
        }
    }
}