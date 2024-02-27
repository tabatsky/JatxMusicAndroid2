package jatx.musictransmitter.android.domain

import jatx.musictransmitter.android.db.entity.Track
import java.io.File

interface TrackInfoStorage {
    var files: List<File>
    fun getTrackFromFile(file: File): Track
    fun setOnUpdateTrackListListener(onUpdate: (tracks: List<Track>) -> Unit)
}

class FileDoesNotExistException: Exception()