package jatx.musictransmitter.android.threads

import android.app.Application

fun Application.provideTransmitterController(
    initialVolume: Int, isNetworkingMode: Boolean): TransmitterController =
        TransmitterControllerImpl(initialVolume, isNetworkingMode)

fun Application.provideTransmitterPlayer(
    uiController: UIController
): TransmitterPlayer =
    TransmitterPlayerImpl(uiController)