package jatx.musictransmitter.android.threads

abstract class TransmitterPlayerDataAcceptor: Thread() {
    abstract fun writeData(data: ByteArray)
}