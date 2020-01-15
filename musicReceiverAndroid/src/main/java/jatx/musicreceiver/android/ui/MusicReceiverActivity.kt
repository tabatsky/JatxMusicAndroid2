package jatx.musicreceiver.android.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import dagger.Lazy
import jatx.debug.AppDebug
import jatx.extensions.showToast
import jatx.musicreceiver.android.App
import jatx.musicreceiver.android.R
import jatx.musicreceiver.android.presentation.MusicReceiverPresenter
import jatx.musicreceiver.android.presentation.MusicReceiverView
import kotlinx.android.synthetic.main.activity_music_receiver.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import javax.inject.Inject

class MusicReceiverActivity : MvpAppCompatActivity(), MusicReceiverView {
    @Inject
    lateinit var presenterProvider: Lazy<MusicReceiverPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppDebug.setAppCrashHandler()
        App.appComponent?.injectMusicReceiverActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_receiver)

        toggleBtn.setOnClickListener {
            presenter.onToggleClick(hostnameET.text.toString())
        }

        autoConnectCB.setOnClickListener {
            presenter.onAutoConnectClick(autoConnectCB.isChecked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_music_receiver, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() = presenter.onBackPressed()

    override fun showSelectHostDialog() {
        val dialog = SelectHostDialog()
        dialog.onOk = { presenter.onDialogOkClick() }
        dialog.onExit = { presenter.onDialogExitClick() }
        dialog.show(supportFragmentManager, "select-host-dialog")
    }

    override fun showToggleText(text: String) {
        toggleBtn.text = text
    }

    override fun showHost(host: String) {
        hostnameET.setText(host)
    }

    override fun showAutoConnect(isAutoConnect: Boolean) {
        autoConnectCB.isChecked = isAutoConnect
    }

    override fun showDisconnectOccured() {
        showToast(R.string.toast_disconnect)
    }

    override fun showQuitDialog() {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(getString(R.string.really_quit))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                presenter.onQuit()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun quit() {
        finish()
    }
}