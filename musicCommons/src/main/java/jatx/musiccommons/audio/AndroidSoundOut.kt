package jatx.musiccommons.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class AndroidSoundOut : SoundOut {
    private var audioTrack: AudioTrack? = null

    override fun renew(frameRate: Int, channels: Int, depth: Int) {
        audioTrack?.stop()
        audioTrack?.release()

        var chFormat = 0

        if (channels == 1) {
            chFormat = AudioFormat.CHANNEL_OUT_MONO
        } else if (channels == 2) {
            chFormat = AudioFormat.CHANNEL_OUT_STEREO
        }

        val encoding = when (depth) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            else -> AudioFormat.ENCODING_PCM_16BIT
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            frameRate,
            chFormat,
            encoding
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            frameRate, chFormat, encoding,
            bufferSize, AudioTrack.MODE_STREAM
        )

        audioTrack?.play()
    }

    override fun setVolume(volume: Int) {
        audioTrack?.setVolume(volume*0.01f)
    }

    override fun write(data: ByteArray, offset: Int, size: Int) {
        audioTrack?.write(data, offset, size)
    }

    override fun destroy() {
        audioTrack?.stop()
        audioTrack?.release()
    }

    override fun play() {
        audioTrack?.play()
    }

    override fun pause() {
        audioTrack?.pause()
    }

}