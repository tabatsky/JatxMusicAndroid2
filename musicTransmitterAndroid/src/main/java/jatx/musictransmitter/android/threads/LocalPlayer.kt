package jatx.musictransmitter.android.threads

import android.util.Log
import jatx.musiccommons.audio.AndroidSoundOut
import jatx.musiccommons.audio.SoundOut
import jatx.musiccommons.frame.frameFromRawData
import java.util.concurrent.ArrayBlockingQueue

class LocalPlayer: TransmitterPlayerDataAcceptor() {

    private val soundOut: SoundOut = AndroidSoundOut()
    @Volatile private var volume = 100

    private val queue = ArrayBlockingQueue<ByteArray>(20)

    override fun writeData(data: ByteArray) {
        queue.put(data)
    }

    override fun run() {
        Log.e("starting","local player")
        var frameRate = 44100
        var channels = 2
        var position = 0
        restartPlayer(frameRate, channels)

        try {
            while (true) {
                val data = queue.poll()
                if (data == null) {
                    sleep(5L)
                } else {
                    val f = frameFromRawData(data)
                    if (frameRate != f.freq || channels != f.channels || position != f.position) {
                        frameRate = f.freq
                        channels = f.channels
                        position = f.position
                        restartPlayer(frameRate, channels)
                    }
                    soundOut.write(f.data, 0, f.size)
                }
            }
        } catch (e: InterruptedException) {
            soundOut.destroy()
        }
    }

    private fun restartPlayer(frameRate: Int, channels: Int) {
        soundOut.renew(frameRate, channels)
        soundOut.setVolume(volume)
        println("(player) player restarted")
        println("(player) frame rate: $frameRate")
        println("(player) channels: $channels")
    }
}