package jatx.musictransmitter.android.threads

import android.app.Application
import jatx.musictransmitter.android.App

fun Application.provideTransmitterController(
    initialVolume: Int, isNetworkingMode: Boolean): TransmitterController =
        TransmitterControllerImpl(initialVolume, isNetworkingMode)

fun Application.provideTransmitterPlayer(
    uiController: UIController
): TransmitterPlayer =
    TransmitterPlayerImpl(uiController)

fun Application.provideTransmitterPlayerConnectionKeeper(
    uiController: UIController
): TransmitterPlayerConnectionKeeper =
    if (this is App) {
        TransmitterPlayerConnectionKeeperImpl(uiController)
    } else {
        TransmitterPlayerConnectionKeeperTestImpl(uiController)
    }