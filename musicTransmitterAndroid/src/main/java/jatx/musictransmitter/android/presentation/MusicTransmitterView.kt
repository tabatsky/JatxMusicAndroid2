package jatx.musictransmitter.android.presentation

import jatx.musictransmitter.android.db.entity.Track
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType

interface MusicTransmitterView: MvpView {
    @StateStrategyType(AddToEndSingleStrategy::class) fun showTracks(tracks: List<Track>, currentPosition: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showPlayingState(isPlaying: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showShuffleState(isShuffle: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenTrackDialog(initPath: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenFolderDialog(initPath: String)
}