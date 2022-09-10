package jatx.musictransmitter.android.threads

import java.io.IOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

const val CONNECT_PORT_PLAYER = 7171

class TransmitterPlayerConnectionKeeper(
    @Volatile private var uiController: UIController
): Thread() {
    private var ss: ServerSocket? = null

    @Volatile
    private var workers = ConcurrentHashMap<String, TransmitterPlayerWorker>()

    @Volatile
    var tk: ThreadKeeper? = null

    fun getWorkerByHost(host: String) = workers[host]

    fun writeData(data: ByteArray) {
        workers.values.forEach {
            it.writeData(data)
        }
    }

    override fun run() {
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
                        val worker = TransmitterPlayerWorker(s, host, tk)
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
            workers.values.forEach {
                it.finishWorkerFlag = true
            }
            println("(player) workers interrupted")
            ss?.close()
            println("(player) server socket closed")
        } finally {
            println("(player) thread finished")
        }
    }
}