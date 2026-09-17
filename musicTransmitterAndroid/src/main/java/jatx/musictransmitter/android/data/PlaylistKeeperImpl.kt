package jatx.musictransmitter.android.data

import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.domain.PlaylistKeeper
import jatx.musictransmitter.android.domain.Settings
import java.io.File

class PlaylistKeeperImpl(
    private val settings: Settings
): PlaylistKeeper {
    override val files = arrayListOf<File>()
    override var currentPosition = -1
    override var tracks = listOf<Track>()

    override val shuffledList = arrayListOf<Int>()
    override var isShuffle: Boolean
        get() = settings.isShuffle
        set(value) {
            settings.isShuffle = value
        }

    override val realPosition: Int
        get() = when {
            currentPosition < 0 -> {
                currentPosition
            }
            isShuffle -> {
                shuffledList[currentPosition % shuffledList.size] % files.size
            }
            else -> {
                (currentPosition % shuffledList.size) % files.size
            }
        }

    override fun reset() {
        files.clear()
        currentPosition = -1
        tracks = listOf()
        shuffledList.clear()
    }
}