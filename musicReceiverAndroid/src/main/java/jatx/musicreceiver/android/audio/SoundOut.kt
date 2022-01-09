package jatx.musicreceiver.android.audio

interface SoundOut {
    fun renew(frameRate: Int, channels: Int, depth: Int = 16)
    fun setVolume(volume: Int)
    fun write(data: ByteArray, offset: Int, size: Int)
    fun destroy()
    fun play()
    fun pause()
}