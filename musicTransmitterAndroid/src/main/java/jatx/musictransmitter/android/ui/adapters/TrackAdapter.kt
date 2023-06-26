package jatx.musictransmitter.android.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.data.MIC_PATH
import jatx.musictransmitter.android.databinding.ItemTrackBinding
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry

class TrackAdapter: ListAdapter<TrackElement, TrackAdapter.TrackViewHolder>(TrackDiffCallback) {
    var onItemClickListener: ((Int) -> Unit)? = null
    var onItemLongClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(
            trackElement = getItem(position),
            onClickListener = onItemClickListener,
            onLongClickListener = onItemLongClickListener
        )
    }

    class TrackViewHolder(private val binding: ItemTrackBinding): ViewHolder(binding.root) {
        fun bind(
            trackElement: TrackElement,
            onClickListener: ((Int) -> Unit)?,
            onLongClickListener: ((Int) -> Unit)?
        ) {
            binding.albumCoverIV.setImageBitmap(
                getAlbumCover(
                    binding.albumCoverIV.context,
                    trackElement
                )
            )

            binding.root.setOnClickListener { onClickListener?.invoke(absoluteAdapterPosition) }
            binding.root.setOnLongClickListener {
                onLongClickListener?.let {
                    it.invoke(absoluteAdapterPosition)
                    true
                } ?: false
            }

            with(trackElement) {
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
        }

        private fun getAlbumCover(context: Context, trackElement: TrackElement): Bitmap {
            with(trackElement) {
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
    }

    object TrackDiffCallback: DiffUtil.ItemCallback<TrackElement>() {
        override fun areItemsTheSame(oldItem: TrackElement, newItem: TrackElement) =
            oldItem.track.path == newItem.track.path

        override fun areContentsTheSame(oldItem: TrackElement, newItem: TrackElement) =
            oldItem.track == newItem.track &&
                    oldItem.isCurrent == newItem.isCurrent
    }
}