package jatx.musictransmitter.android.data

import android.util.Log
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.domain.ContentStorage
import jatx.musictransmitter.android.domain.TrackInfoStorage
import jatx.musictransmitter.android.media.AlbumEntry
import jatx.musictransmitter.android.media.ArtistEntry
import jatx.musictransmitter.android.media.MusicEntry
import jatx.musictransmitter.android.media.TrackEntry
import java.io.File

const val commonTrackLength = "1:37"

fun makeTrackEntry(artistId: Int, albumId: Int, titleId: Int) = TrackEntry(
    artist = "Artist $artistId",
    album = "Album $artistId $albumId",
    title = "Title $albumId $titleId"
)

val tracks = arrayListOf<Track>().also { theTracks ->
    (1..5).forEach { artistId ->
        (1..5).forEach { albumId ->
            (1..5).forEach { titleId ->
                val track = Track(
                    path = "/sdcard/Music/${artistId}_${albumId}_${titleId}.mp3",
                    artist = "Artist $artistId",
                    album = "Album $artistId $albumId",
                    title = "Title $albumId $titleId",
                    year = "1999",
                    length = commonTrackLength,
                    number = titleId.toString(),
                    lastModified = System.currentTimeMillis() / (1000L * 60 * 60 * 24)
                )
                theTracks.add(track)
            }
        }
    }
}

val artistEntries = tracks.groupBy {
    ArtistEntry(artist = it.artist)
}.map {
    val key = it.key
    val value = it.value.map { File(it.path) }
    key to value
}.toMap()

val albumEntries = tracks.groupBy {
    AlbumEntry(artist = it.artist, album = it.album)
}.map {
    val key = it.key
    val value = it.value.map { File(it.path) }
    key to value
}.toMap()

val trackEntries = tracks.associate {
    TrackEntry(artist = it.artist, album = it.album, title = it.title) to File(it.path)
}

val trackByFileMap = tracks.associateBy { File(it.path) }

class ContentStorageTestImpl: ContentStorage {
    override fun getFilesByEntry(entry: MusicEntry): List<File> {
        return when (entry) {
            is ArtistEntry -> artistEntries[entry]!!
            is AlbumEntry -> albumEntries[entry]!!
            is TrackEntry -> listOf(trackEntries[entry]!!)
        }
    }

    override fun getAlbumEntries() = albumEntries.keys.toList().sortedBy { it.searchString }

    override fun getArtistEntries() = artistEntries.keys.toList().sortedBy { it.searchString }

    override fun getTrackEntries() = trackEntries.keys.toList().sortedBy { it.searchString }
}

class TrackInfoStorageTestImpl : TrackInfoStorage {
    private var onUpdateTrackListListener: OnUpdateTrackListListener? = null

    override var files: List<File> = listOf()
        set(value) {
            field = value
            try {
                onUpdateTrackListListener?.onUpdateTrackList(value.map { getTrackFromFile(it) })
            } catch (e: Exception) {
                Log.e("test error", "when set files")
                field = listOf()
            }
        }
    override fun getTrackFromFile(file: File): Track = trackByFileMap[file]!!

    override fun setOnUpdateTrackListListener(onUpdate: (tracks: List<Track>) -> Unit)  {
        onUpdateTrackListListener = object : OnUpdateTrackListListener {
            override fun onUpdateTrackList(tracks: List<Track>) {
                onUpdate(tracks)
            }
        }
    }

}