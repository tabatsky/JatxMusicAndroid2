package jatx.musictransmitter.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import jatx.musictransmitter.android.R

object AlbumArtKeeper {
    val albumArts: HashMap<AlbumEntry, Bitmap> = hashMapOf()

    fun retrieveAlbumArt(context: Context, path: String): Bitmap {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        } catch (e: Exception) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        }
    }
}