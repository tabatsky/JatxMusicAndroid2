package jatx.musictransmitter.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import jatx.musictransmitter.android.R
import java.io.ByteArrayOutputStream
import java.io.File


object ArtKeeper {
    val theArts: HashMap<MusicEntry, Bitmap> = hashMapOf()
    val theArtsByPath: HashMap<String, Bitmap> = hashMapOf()

    fun retrieveArt(context: Context, path: String): Bitmap {
        theArtsByPath[path]?.let {
            return it
        }
        val bitmap = try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
//            BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        } catch (_: Exception) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
        }
        theArtsByPath[path] = bitmap
        return bitmap
    }

    fun retrieveArtUri(context: Context, path: String): Uri {
        val fileName = "art_${path.hashCode()}.png"
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            val bitmap = retrieveArt(context, path)
            file.outputStream().use { out ->
                bitmap.compress(CompressFormat.PNG, 100, out)
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun retrieveArtByteArray(context: Context, path: String): ByteArray? {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.embeddedPicture
        } catch (_: Exception) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album)
                .toByteArray()
        }
    }
}

fun Bitmap.toByteArray(): ByteArray {
    val os = ByteArrayOutputStream()
    this.compress(CompressFormat.PNG, 0, os)
    return os.toByteArray()
}


