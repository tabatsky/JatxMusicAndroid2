package jatx.musictransmitter.android.threads

import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

const val CONNECT_PORT_PLAYER = 7171

abstract class TransmitterPlayerConnectionKeeper: TransmitterPlayerDataAcceptor() {
    abstract fun getWorkerByHost(host: String): TransmitterPlayerWorker?
}

class TransmitterPlayerConnectionKeeperImpl(
    @Volatile private var uiController: UIController
): TransmitterPlayerConnectionKeeper() {
    private var ss: ServerSocket? = null

    @Volatile
    private var workers = ConcurrentHashMap<String, TransmitterPlayerWorker>()

    override fun getWorkerByHost(host: String) = workers[host]

    override fun writeData(data: ByteArray) {
        workers.values.forEach {
            it.writeData(data)
        }
    }

    override fun run() {
        Log.e("starting","transmitter player connection keeper")
        try {
            while (true) {
                sleep(100)
                ss = ServerSocket(CONNECT_PORT_PLAYER)
                println("(player) new server socket")
                try {
                    ss?.soTimeout = SO_TIMEOUT
                    val s = ss?.accept()
                    println("(player) server socket accept")
                    s?.inetAddress?.hostAddress?.let { host ->
                        val worker = TransmitterPlayerWorker(s)
                        worker.start()
                        println("(player) worker $host started")
                        workers[host] = worker
                        println("(player) total workers: ${workers.size}")
                        worker.onWorkerStopped = {
                            println("(player) worker $host stopped")
                            workers.remove(host)
                            println("(player) total workers: ${workers.size}")
                            uiController.updateWifiStatus(workers.size)
                        }
                        uiController.updateWifiStatus(workers.size)
                    }
                } catch (e: SocketTimeoutException) {
                    println("(player) socket timeout")
                } finally {
                    ss?.close()
                    println("(player) server socket closed")
                    uiController.updateWifiStatus(workers.size)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            println("(player) thread interrupted")
        } finally {
            workers.values.forEach {
                it.finishWorkerFlag = true
            }
            println("(player) workers finished")
            ss?.close()
            println("(player) server socket closed")
            println("(player) thread finished")
        }
    }
}