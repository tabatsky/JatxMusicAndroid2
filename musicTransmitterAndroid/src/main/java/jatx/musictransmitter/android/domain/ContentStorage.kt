package jatx.musictransmitter.android.domain

import jatx.musictransmitter.android.media.MusicEntry
import java.io.File

interface ContentStorage {
    fun getFilesByEntry(entry: MusicEntry): List<File>
    fun getAlbumEntries(): List<MusicEntry>
    fun getArtistEntries(): List<MusicEntry>
    fun getTrackEntries(): List<MusicEntry>
}