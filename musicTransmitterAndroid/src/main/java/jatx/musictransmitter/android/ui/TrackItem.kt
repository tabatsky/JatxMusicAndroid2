package jatx.musictransmitter.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.databinding.ItemTrackBinding
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry

class TrackItem(val track: Track, val position: Int, private val isCurrent: Boolean):
    BindableItem<ItemTrackBinding>() {
    override fun getLayout() = R.layout.item_track
    override fun initializeViewBinding(view: View) =
        ItemTrackBinding.bind(view)

    override fun bind(binding: ItemTrackBinding, position: Int) {
        binding.albumCoverIV.setImageBitmap(getAlbumCover(binding.albumCoverIV.context))

        binding.titleTV.text = track.title
        var meta = track.artist
        if (meta.isNotEmpty()) {
            meta += " | ${track.length}"
        } else {
            meta = track.length
        }
        binding.metaTV.text = meta
        val context = binding.root.context
        val theme = context.theme
        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValuePrimary, true)
        val colorPrimary = typedValuePrimary.data
        val typedValueOnPrimary = TypedValue()
        theme.resolveAttribute(R.attr.colorOnPrimary, typedValueOnPrimary, true)
        val colorOnPrimary = typedValueOnPrimary.data
        if (isCurrent) {
            binding.wholeLayout.setBackgroundColor(colorPrimary)
            binding.innerLayout.setBackgroundColor(colorPrimary)
            binding.titleTV.setBackgroundColor(colorPrimary)
            binding.metaTV.setBackgroundColor(colorPrimary)
            binding.titleTV.setTextColor(colorOnPrimary)
            binding.metaTV.setTextColor(colorOnPrimary)
        } else {
            binding.wholeLayout.setBackgroundColor(colorOnPrimary)
            binding.innerLayout.setBackgroundColor(colorOnPrimary)
            binding.titleTV.setBackgroundColor(colorOnPrimary)
            binding.metaTV.setBackgroundColor(colorOnPrimary)
            binding.titleTV.setTextColor(colorPrimary)
            binding.metaTV.setTextColor(colorPrimary)
        }
    }

    override fun isSameAs(other: Item<*>) =
        (other is TrackItem) &&
                (other.track == track) &&
                (other.isCurrent == isCurrent)

    override fun hasSameContentAs(other: Item<*>) =
        (other is TrackItem) &&
                (other.track == track) &&
                (other.isCurrent == isCurrent)

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
}