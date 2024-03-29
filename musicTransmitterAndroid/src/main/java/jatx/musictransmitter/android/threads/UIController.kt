package jatx.musictransmitter.android.threads

import androidx.annotation.StringRes

interface UIController {
    fun updateWifiStatus(count: Int)
    fun setCurrentTime(currentMs: Float, trackLengthMs: Float)
    fun nextTrack()
    fun errorMsg(msg: String)
    fun errorMsg(@StringRes resId: Int)
}