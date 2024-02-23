package jatx.musictransmitter.android.threads

abstract class TransmitterPlayerDataAcceptor: Thread() {
    @Volatile
    var tk: ThreadKeeper? = null
    abstract fun writeData(data: ByteArray)
}