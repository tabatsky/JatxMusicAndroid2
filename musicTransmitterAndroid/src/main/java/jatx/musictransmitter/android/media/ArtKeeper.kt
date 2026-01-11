package jatx.musictransmitter.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import jatx.musictransmitter.android.R

object ArtKeeper {
    val theArts: HashMap<MusicEntry, Bitmap> = hashMapOf()

    fun retrieveArt(context: Context, path: String): Bitmap {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        } catch (_: Exception) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        }
    }
}