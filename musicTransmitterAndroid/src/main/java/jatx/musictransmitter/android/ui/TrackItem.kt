package jatx.musictransmitter.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.TypedValue
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry
import kotlinx.android.synthetic.main.item_track.*


class TrackItem(val track: Track, val position: Int, private val isCurrent: Boolean): Item() {
    override fun getLayout() = R.layout.item_track

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.albumCoverIV.setImageBitmap(getAlbumCover(viewHolder.albumCoverIV.context))

        viewHolder.titleTV.text = track.title
        var meta = track.artist
        if (meta.isNotEmpty()) {
            meta += " | ${track.length}"
        } else {
            meta = track.length
        }
        viewHolder.metaTV.text = meta
        val context = viewHolder.containerView.context
        val theme = context.theme
        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValuePrimary, true)
        val colorPrimary = typedValuePrimary.data
        val typedValueOnPrimary = TypedValue()
        theme.resolveAttribute(R.attr.colorOnPrimary, typedValueOnPrimary, true)
        val colorOnPrimary = typedValueOnPrimary.data
        if (isCurrent) {
            viewHolder.wholeLayout.setBackgroundColor(colorPrimary)
            viewHolder.innerLayout.setBackgroundColor(colorPrimary)
            viewHolder.titleTV.setBackgroundColor(colorPrimary)
            viewHolder.metaTV.setBackgroundColor(colorPrimary)
            viewHolder.titleTV.setTextColor(colorOnPrimary)
            viewHolder.metaTV.setTextColor(colorOnPrimary)
        } else {
            viewHolder.wholeLayout.setBackgroundColor(colorOnPrimary)
            viewHolder.innerLayout.setBackgroundColor(colorOnPrimary)
            viewHolder.titleTV.setBackgroundColor(colorOnPrimary)
            viewHolder.metaTV.setBackgroundColor(colorOnPrimary)
            viewHolder.titleTV.setTextColor(colorPrimary)
            viewHolder.metaTV.setTextColor(colorPrimary)
        }
    }

    private fun getAlbumCover(context: Context): Bitmap {
        AlbumArtKeeper.albumArts[AlbumEntry(track.artist, track.album)]?.let {
            return it
        }

        val bitmap = if (track.path == MIC_PATH) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_microphone)
        } else {
            AlbumArtKeeper.retrieveAlbumArt(context, track.path)
        }

        AlbumArtKeeper.albumArts[AlbumEntry(track.artist, track.album)] = bitmap

        return bitmap
    }

    companion object {

    }
}