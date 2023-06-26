package jatx.musictransmitter.android.ui.adapters

import jatx.musictransmitter.android.db.entity.Track

data class TrackElement(
    val track: Track,
    val isCurrent: Boolean
)