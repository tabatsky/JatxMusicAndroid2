package jatx.musictransmitter.android.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jatx.musictransmitter.android.databinding.ItemMusicSelectorBinding
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry
import jatx.musictransmitter.android.media.ArtistEntry
import jatx.musictransmitter.android.media.MusicEntry
import jatx.musictransmitter.android.media.TrackEntry

class MusicSelectorAdapter:
    ListAdapter<MusicEntry, MusicSelectorAdapter.MusicSelectorViewHolder>(MusicEntryDiffCallback) {
    var onItemClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicSelectorViewHolder {
        val binding = ItemMusicSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MusicSelectorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicSelectorViewHolder, position: Int) {
        holder.bind(
            entry = getItem(position),
            onClickListener = onItemClickListener
        )
    }

    class MusicSelectorViewHolder(private val binding: ItemMusicSelectorBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: MusicEntry, onClickListener: ((Int) -> Unit)?) {
            when (entry) {
                is ArtistEntry -> {
                    binding.musicSelectorAlbumArtIV.visibility = View.GONE
                }
                is AlbumEntry -> {
                    binding.musicSelectorAlbumArtIV.visibility = View.VISIBLE
                    binding.musicSelectorAlbumArtIV.setImageBitmap(AlbumArtKeeper.albumArts[entry])
                }
                is TrackEntry -> {
                    binding.musicSelectorAlbumArtIV.visibility = View.VISIBLE
                    binding.musicSelectorAlbumArtIV.setImageBitmap(AlbumArtKeeper.albumArts[entry.albumEntry])
                }
            }

            binding.musicSelectorItemBtn.text = entry.asString
            binding.musicSelectorItemBtn.setOnClickListener {
                onClickListener?.invoke(layoutPosition)
            }
        }
    }

    object MusicEntryDiffCallback: DiffUtil.ItemCallback<MusicEntry>() {
        override fun areItemsTheSame(oldItem: MusicEntry, newItem: MusicEntry) =
            oldItem.asString == newItem.asString

        override fun areContentsTheSame(oldItem: MusicEntry, newItem: MusicEntry) =
            oldItem.asString == newItem.asString
    }
}