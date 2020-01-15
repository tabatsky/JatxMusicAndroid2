package jatx.musicreceiver.android.threads

import jatx.musicreceiver.android.data.Settings

class AutoConnectThread(
    @Volatile private var settings: Settings,
    @Volatile private var serviceController: ServiceController
) : Thread() {

    override fun run() {
        try {
            while (true) {
                if (settings.isAutoConnect) {
                    serviceController.startJob()
                }
                sleep(5000)
            }
        } catch (e: InterruptedException) {
            println("(auto connect thread) interrupted")
        }
    }
}