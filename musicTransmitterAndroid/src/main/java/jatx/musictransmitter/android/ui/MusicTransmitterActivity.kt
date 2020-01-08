package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.obsez.android.lib.filechooser.ChooserDialog
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.Lazy
import jatx.debug.AppDebug
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.db.AppDatabase
import jatx.musictransmitter.android.db.entity.Track
import jatx.musictransmitter.android.extensions.showToast
import jatx.musictransmitter.android.presentation.MusicTransmitterPresenter
import jatx.musictransmitter.android.presentation.MusicTransmitterView
import kotlinx.android.synthetic.main.activity_music_transmitter.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import javax.inject.Inject

class MusicTransmitterActivity : MvpAppCompatActivity(), MusicTransmitterView {
    @Inject
    lateinit var presenterProvider: Lazy<MusicTransmitterPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    private val tracksAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppDebug.setAppCrashHandler()
        App.appComponent?.injectMusicTransmitterActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_transmitter)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        initTracksRV()

        playBtn.setOnClickListener { presenter.onPlayClick() }
        pauseBtn.setOnClickListener { presenter.onPauseClick() }

        repeatBtn.setOnClickListener { presenter.onRepeatClick() }
        shuffleBtn.setOnClickListener { presenter.onShuffleClick() }

        revBtn.setOnClickListener { presenter.onRevClick() }
        fwdBtn.setOnClickListener { presenter.onFwdClick() }

        volumeDownBtn.setOnClickListener { presenter.onVolumeDownClick() }
        volumeUpBtn.setOnClickListener { presenter.onVolumeUpClick() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_music_transmitter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_menu_add_track -> {
                presenter.onAddTrackSelected()
                true
            }
            R.id.item_menu_add_folder -> {
                presenter.onAddFolderSelected()
                true
            }
            R.id.item_menu_remove -> {
                true
            }
            R.id.item_menu_remove_all -> {
                presenter.onRemoveAllTracks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showTracks(tracks: List<Track>, currentPosition: Int) {
        val trackItems = tracks.mapIndexed { index, track ->
            TrackItem(track, index, index == currentPosition)
        }
        runOnUiThread {
            tracksAdapter.updateAsync(trackItems)
        }
    }

    override fun showPlayingState(isPlaying: Boolean) {
        if (isPlaying) {
            playBtn.visibility = View.GONE
            pauseBtn.visibility = View.VISIBLE
        } else {
            playBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.GONE
        }
    }

    override fun showShuffleState(isShuffle: Boolean) {
        if (isShuffle) {
            repeatBtn.visibility = View.GONE
            shuffleBtn.visibility = View.VISIBLE
        } else {
            repeatBtn.visibility = View.VISIBLE
            shuffleBtn.visibility = View.GONE
        }
    }

    override fun showOpenTrackDialog(initPath: String) {
        ChooserDialog(this)
            .withFilter(false, false, "mp3")
            .withStartFile(initPath)
            .withChosenListener { path, _ ->
                presenter.onTrackOpened(path)
            }
            .withOnCancelListener { dialog ->
                dialog.cancel()
            }
            .build()
            .show()
    }

    override fun showOpenFolderDialog(initPath: String) {
        ChooserDialog(this)
            .withFilter(true, false)
            .withStartFile(initPath)
            .withChosenListener { path, _ ->
                presenter.onFolderOpened(path)
            }
            .withOnCancelListener { dialog ->
                dialog.cancel()
            }
            .build()
            .show()
    }

    private fun initTracksRV() {
        tracksRV.adapter = tracksAdapter
        tracksAdapter.setOnItemClickListener { item, _ ->
            if (item is TrackItem) {
                presenter.onTrackClick(item.position)
            }
        }
        tracksAdapter.setOnItemLongClickListener { item, _ ->
            if (item is TrackItem) {
                showToast("On track #${item.position} long click")
                true
            } else {
                false
            }
        }
    }
}
