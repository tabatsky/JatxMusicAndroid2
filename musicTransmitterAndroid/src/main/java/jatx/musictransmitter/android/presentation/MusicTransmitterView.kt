package jatx.musictransmitter.android.presentation

import jatx.musictransmitter.android.db.entity.Track
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

interface MusicTransmitterView: MvpView {
    @StateStrategyType(AddToEndSingleStrategy::class) fun showTracks(tracks: List<Track>, currentPosition: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun scrollToPosition(position: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showWifiStatus(isWifiOk: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showPlayingState(isPlaying: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showShuffleState(isShuffle: Boolean)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenTrackDialog(initPath: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showOpenFolderDialog(initPath: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showCurrentTime(currentMs: Float, trackLengthMs: Float)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showVolume(volume: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showRemoveTrackMessage()
    @StateStrategyType(OneExecutionStateStrategy::class) fun showTrackLongClickDialog(position: Int)
    @StateStrategyType(OneExecutionStateStrategy::class) fun showIPAddress(ipAddress: String)
    @StateStrategyType(OneExecutionStateStrategy::class) fun tryAddMic()
    @StateStrategyType(SkipStrategy::class) fun close()
}