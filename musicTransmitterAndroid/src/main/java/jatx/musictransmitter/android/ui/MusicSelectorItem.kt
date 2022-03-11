package jatx.musictransmitter.android.ui

import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import jatx.musictransmitter.android.R
import kotlinx.android.synthetic.main.item_music_selector.*
import java.time.Year

class MusicSelectorItem(
    val entry: MusicEntry,
    val onClick: () -> Unit
): Item() {
    override fun getLayout() = R.layout.item_music_selector

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.musicSelectorItemBtn.text = entry.asString()
        viewHolder.musicSelectorItemBtn.setOnClickListener {
            onClick()
        }
    }
}

sealed class MusicEntry {
    open fun asString() = ""
}

data class ArtistEntry(
    val artist: String
): MusicEntry() {
    override fun asString() = artist
}

data class AlbumEntry(
    val artist: String,
    val album: String
): MusicEntry() {
    override fun asString() = "$album ($artist)"
}

data class TrackEntry(
    val artist: String,
    val album: String,
    val title: String
): MusicEntry() {
    override fun asString() = "$title ($album, $artist)"
}