package jatx.musictransmitter.android.threads

class TransmitterPlayerConnectionKeeperTestImpl(
    @Volatile private var uiController: UIController
): TransmitterPlayerConnectionKeeper() {

    var workerCount: Int = 0
        set(value) {
            field = value
            uiController.updateWifiStatus(value)
        }

    override fun getWorkerByHost(host: String) = null
    override fun writeData(data: ByteArray) = Unit
}