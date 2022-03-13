package jatx.musictransmitter.android.ui

import android.view.View
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.media.*
import kotlinx.android.synthetic.main.item_music_selector.*

class MusicSelectorItem(
    val entry: MusicEntry,
    val onClick: () -> Unit
): Item() {
    override fun getLayout() = R.layout.item_music_selector

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        when (entry) {
            is ArtistEntry -> {
                viewHolder.musicSelectorAlbumArtIV.visibility = View.GONE
            }
            is AlbumEntry -> {
                viewHolder.musicSelectorAlbumArtIV.visibility = View.VISIBLE
                viewHolder.musicSelectorAlbumArtIV.setImageBitmap(AlbumArtKeeper.albumArts[entry])
            }
            is TrackEntry -> {
                viewHolder.musicSelectorAlbumArtIV.visibility = View.VISIBLE
                viewHolder.musicSelectorAlbumArtIV.setImageBitmap(AlbumArtKeeper.albumArts[entry.albumEntry])
            }
        }

        viewHolder.musicSelectorItemBtn.text = entry.asString
        viewHolder.musicSelectorItemBtn.setOnClickListener {
            onClick()
        }
    }
}
