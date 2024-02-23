package jatx.musictransmitter.android.threads

class ThreadKeeper(
    val tu: TimeUpdater,
    val tc: TransmitterController,
    val tp: TransmitterPlayer,
    val tpda: TransmitterPlayerDataAcceptor
)