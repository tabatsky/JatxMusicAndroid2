package jatx.musictransmitter.android.data

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import jatx.extensions.showToast
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.domain.ContentStorage
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry
import jatx.musictransmitter.android.media.ArtistEntry
import jatx.musictransmitter.android.media.MusicEntry
import jatx.musictransmitter.android.media.TrackEntry
import java.io.File

class ContentStorageImpl(
    private val context: Context
): ContentStorage {
    override fun getFilesByEntry(entry: MusicEntry): List<File> {
        val selection = when (entry) {
            is ArtistEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ?"
            is AlbumEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ? AND " +
                        MediaStore.Audio.Media.ALBUM + " = ?"
            is TrackEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ? AND " +
                        MediaStore.Audio.Media.ALBUM + " = ? AND " +
                        MediaStore.Audio.Media.TITLE + " = ?"
        }

        val sortOrder = MediaStore.Audio.Media.ARTIST + " || " +
                MediaStore.Audio.Media.YEAR + " || " +
                MediaStore.Audio.Media.ALBUM + " || " +
                " (10000 + " + MediaStore.Audio.Media.TRACK + ") || " +
                MediaStore.Audio.Media.TITLE + " ASC"


        val selectionArgs = when (entry) {
            is ArtistEntry -> arrayOf(entry.artist)
            is AlbumEntry -> arrayOf(entry.artist, entry.album)
            is TrackEntry -> arrayOf(entry.artist, entry.album, entry.title)
        }

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(
            getMusicExternalContentUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        val files = arrayListOf<File>()
        cursor?.use {
            while (it.moveToNext()) {
                val path = it.getString(0)
                files.add(File(path))
            }
        }

        val filteredFiles = files
            .filter { it.name.endsWith(".mp3") || it.name.endsWith(".flac") }

        if (files.size != filteredFiles.size) {
            context.showToast(R.string.toast_unsupported_files_format)
        }

        return filteredFiles
    }

    override fun getAlbumEntries(): List<MusicEntry> {
        val sortOrder = MediaStore.Audio.Media.ALBUM + " || " +
                MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                val album = it.getString(1)
                val albumEntry = AlbumEntry(artist, album)
                entries.add(albumEntry)

                val path = it.getString(2)

                if (!AlbumArtKeeper.albumArts.containsKey(albumEntry)) {
                    AlbumArtKeeper.albumArts[albumEntry] = AlbumArtKeeper.retrieveAlbumArt(context, path)
                }
            }
        }

        return entries.distinct()
    }

    override fun getArtistEntries(): List<MusicEntry> {
        val sortOrder = MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                entries.add(ArtistEntry(artist))
            }
        }

        return entries.distinct()
    }

    override fun getTrackEntries(): List<MusicEntry> {
        val sortOrder =
            MediaStore.Audio.Media.TITLE + " || " +
                    MediaStore.Audio.Media.ALBUM + " || " +
                    MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                val album = it.getString(1)
                val title = it.getString(2)

                val trackEntry = TrackEntry(artist, album, title)

                entries.add(trackEntry)

                val path = it.getString(3)

                if (!AlbumArtKeeper.albumArts.containsKey(trackEntry.albumEntry)) {
                    AlbumArtKeeper.albumArts[trackEntry.albumEntry] = AlbumArtKeeper.retrieveAlbumArt(context, path)
                }
            }
        }

        return entries.distinct()
    }

    private fun allMusicQuery(projection: Array<String>, sortOrder: String): Cursor? {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                MediaStore.Files.FileColumns.MIME_TYPE + " = ?)"
        val mimeTypeMP3 = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
        val mimeTypeFLAC = MimeTypeMap.getSingleton().getMimeTypeFromExtension("flac")

        return context.contentResolver.query(
            getMusicExternalContentUri(),
            projection,
            selection,
            arrayOf(mimeTypeMP3, mimeTypeFLAC),
            sortOrder
        )
    }

    private fun getMusicExternalContentUri() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
}