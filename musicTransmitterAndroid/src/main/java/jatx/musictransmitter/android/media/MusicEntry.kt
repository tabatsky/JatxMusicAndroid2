package jatx.musictransmitter.android.media

sealed class MusicEntry {
    abstract val asString: String
    val searchString by lazy {
        asString.lowercase().trim()
    }
}

data class ArtistEntry(
    val artist: String
): MusicEntry() {
    override val asString = artist
}

data class AlbumEntry(
    val artist: String,
    val album: String
): MusicEntry() {
    override val asString = "$album ($artist)"
}

data class TrackEntry(
    val artist: String,
    val album: String,
    val title: String
): MusicEntry() {
    override val asString = "$title ($album, $artist)"
    val albumEntry = AlbumEntry(artist, album)
}