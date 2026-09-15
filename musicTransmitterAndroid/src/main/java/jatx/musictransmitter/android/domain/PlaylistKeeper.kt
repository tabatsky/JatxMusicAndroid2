package jatx.musictransmitter.android.domain

import jatx.musictransmitter.android.db.entity.Track
import java.io.File

interface PlaylistKeeper {
    val files: ArrayList<File>
    var currentPosition: Int
    var tracks: List<Track>
    val shuffledList: ArrayList<Int>
    var isShuffle: Boolean
    val realPosition: Int
}