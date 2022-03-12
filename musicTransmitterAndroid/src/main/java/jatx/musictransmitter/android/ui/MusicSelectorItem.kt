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
        viewHolder.musicSelectorItemBtn.text = entry.asString
        viewHolder.musicSelectorItemBtn.setOnClickListener {
            onClick()
        }
    }
}

sealed class MusicEntry {
    abstract val asString: String
    val searchString by lazy {
        asString.lowercase().trim()
    }
}

data class ArtistEntry(
    val artist: String
): MusicEntry() {
    override val asString = artist
}

data class AlbumEntry(
    val artist: String,
    val album: String
): MusicEntry() {
    override val asString = "$album ($artist)"
}

data class TrackEntry(
    val artist: String,
    val album: String,
    val title: String
): MusicEntry() {
    override val asString = "$title ($album, $artist)"
}