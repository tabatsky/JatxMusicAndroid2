package jatx.musicreceiver.android.presentation

import android.content.Context
import android.content.Intent
import jatx.musicreceiver.android.R
import jatx.musicreceiver.android.data.Settings
import jatx.musicreceiver.android.services.MusicReceiverService
import jatx.musicreceiver.android.services.SERVICE_START_JOB
import jatx.musicreceiver.android.services.SERVICE_STOP_JOB
import jatx.musicreceiver.android.services.STOP_SERVICE
import moxy.InjectViewState
import moxy.MvpPresenter
import javax.inject.Inject

@InjectViewState
class MusicReceiverPresenter @Inject constructor(
    private val context: Context,
    private val settings: Settings
): MvpPresenter<MusicReceiverView>() {

    private var isRunning = false

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        initBroadcastReceivers()
        startService()

        viewState.showSelectHostDialog()
    }

    override fun onDestroy() {
        stopService()

        unregisterReceivers()

        super.onDestroy()
    }

    fun onToggleClick(host: String) {
        if (!isRunning) {
            uiStartJob(host)
            serviceStartJob()
        } else {
            uiStopJob()
            serviceStopJob()
        }
    }

    fun onAutoConnectClick(checked: Boolean) {
        settings.isAutoConnect = checked
    }

    fun onBackPressed() = viewState.showQuitDialog()

    fun onQuit() = viewState.quit()

    fun onDialogOkClick() {
        prepareAndStart()
    }

    fun onDialogExitClick() = viewState.quit()

    private fun prepareAndStart() {
        viewState.showHost(settings.host)
        viewState.showAutoConnect(settings.isAutoConnect)
    }

    private fun uiStartJob(host: String) {
        if (isRunning) return
        isRunning = true
        viewState.showToggleText(context.getString(R.string.string_stop))
        settings.host = host
    }
    
    private fun uiStopJob() {
        if (!isRunning) return
        isRunning = false
        viewState.showToggleText(context.getString(R.string.string_start))
        viewState.showDisconnectOccured()
    }

    private fun serviceStartJob() {
        val intent = Intent(SERVICE_START_JOB)
        context.sendBroadcast(intent)
    }

    private fun serviceStopJob() {
        val intent = Intent(SERVICE_STOP_JOB)
        context.sendBroadcast(intent)
    }

    private fun initBroadcastReceivers() {

    }

    private fun unregisterReceivers() {

    }

    private fun startService() {
        val intent = Intent(context, MusicReceiverService::class.java)
        context.startService(intent)
    }

    private fun stopService() {
        val intent = Intent(STOP_SERVICE)
        context.sendBroadcast(intent)
    }
}