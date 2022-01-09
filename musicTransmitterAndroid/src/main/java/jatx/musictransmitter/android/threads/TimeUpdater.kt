package jatx.musictransmitter.android.threads

import jatx.musictransmitter.android.audio.MusicDecoder

class TimeUpdater(
    @Volatile private var uiController: UIController
): Thread() {

    override fun run() {
        try {
            while (true) {
                uiController.setCurrentTime(
                    MusicDecoder.INSTANCE?.currentMs ?: 0f,
                    (MusicDecoder.INSTANCE?.trackLengthSec ?: 1) * 1000f)
                sleep(500)
            }
        } catch (e: InterruptedException) {
            println("time updater interrupted")
        }
    }
}