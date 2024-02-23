package jatx.musictransmitter.android.threads

import android.util.Log

class LocalPlayer: TransmitterPlayerDataAcceptor() {
    override fun writeData(data: ByteArray) {

    }

    override fun run() {
        Log.e("starting","local player")
    }
}