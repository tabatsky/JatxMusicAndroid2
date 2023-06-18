package jatx.musictransmitter.android.ui

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.databinding.ItemMusicSelectorBinding
import jatx.musictransmitter.android.media.AlbumArtKeeper
import jatx.musictransmitter.android.media.AlbumEntry
import jatx.musictransmitter.android.media.ArtistEntry
import jatx.musictransmitter.android.media.MusicEntry
import jatx.musictransmitter.android.media.TrackEntry

class MusicSelectorItem(
    val entry: MusicEntry,
    val onClick: () -> Unit
): BindableItem<ItemMusicSelectorBinding>() {
    override fun getLayout() = R.layout.item_music_selector
    override fun initializeViewBinding(view: View) =
        ItemMusicSelectorBinding.bind(view)

    override fun bind(binding: ItemMusicSelectorBinding, position: Int) {
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
            onClick()
        }
    }
}
