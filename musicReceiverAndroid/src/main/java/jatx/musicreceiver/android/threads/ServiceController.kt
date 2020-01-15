package jatx.musicreceiver.android.threads

interface ServiceController {
    fun startJob()
    fun stopJob()
    fun play()
    fun pause()
    fun setVolume(volume: Int)
}