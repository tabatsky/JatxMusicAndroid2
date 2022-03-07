package jatx.musictransmitter.android.ui

import android.util.TypedValue
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.db.entity.Track
import kotlinx.android.synthetic.main.item_track.*


class TrackItem(val track: Track, val position: Int, private val isCurrent: Boolean): Item() {
    override fun getLayout() = R.layout.item_track

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
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
            viewHolder.titleTV.setBackgroundColor(colorPrimary)
            viewHolder.metaTV.setBackgroundColor(colorPrimary)
            viewHolder.titleTV.setTextColor(colorOnPrimary)
            viewHolder.metaTV.setTextColor(colorOnPrimary)
        } else {
            viewHolder.wholeLayout.setBackgroundColor(colorOnPrimary)
            viewHolder.titleTV.setBackgroundColor(colorOnPrimary)
            viewHolder.metaTV.setBackgroundColor(colorOnPrimary)
            viewHolder.titleTV.setTextColor(colorPrimary)
            viewHolder.metaTV.setTextColor(colorPrimary)
        }
    }

//    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean =
//        if (other is TrackItem) (track.path == other.track.path)
//            .and(position == other.position)
//            .and(isCurrent == other.isCurrent)
//        else false
//
//    override fun hasSameContentAs(other: com.xwray.groupie.Item<*>?): Boolean =
//        if (other is TrackItem) (track == other.track)
//            .and(isCurrent == other.isCurrent)
//        else false
}