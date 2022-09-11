package jatx.musictransmitter.android.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.obsez.android.lib.filechooser.ChooserDialog
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.Lazy
import jatx.constants.*
import jatx.debug.AppDebug
import jatx.extensions.onSeek
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.db.entity.Track
import jatx.extensions.showToast
import jatx.musictransmitter.android.media.*
import jatx.musictransmitter.android.presentation.MusicTransmitterPresenter
import jatx.musictransmitter.android.presentation.MusicTransmitterView
import jatx.musictransmitter.android.services.EXTRA_WIFI_STATUS
import jatx.musictransmitter.android.services.TP_AND_TC_PAUSE
import kotlinx.android.synthetic.main.activity_music_transmitter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import java.io.File
import javax.inject.Inject

const val REQUEST_TAG_EDITOR = 2222

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
        pauseBtn.setOnClickListener { presenter.onPauseClick(
            needSendBroadcast = true,
            needShowNotification = true
        ) }

        repeatBtn.setOnClickListener { presenter.onRepeatClick() }
        shuffleBtn.setOnClickListener { presenter.onShuffleClick() }

        revBtn.setOnClickListener { presenter.onRevClick() }
        fwdBtn.setOnClickListener { presenter.onFwdClick() }

        volumeDownBtn.setOnClickListener { presenter.onVolumeDownClick() }
        volumeUpBtn.setOnClickListener { presenter.onVolumeUpClick() }

        seekBar.max = 1000
        seekBar.onSeek { i -> presenter.onProgressChanged(if (i < 1000) (i / 1000.0) else 0.999) }

        val tpAndTcPauseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val wifiStatus = intent.getBooleanExtra(EXTRA_WIFI_STATUS, false)
                presenter.onPauseClick(false, wifiStatus)
            }
        }
        registerReceiver(tpAndTcPauseReceiver, IntentFilter(TP_AND_TC_PAUSE))

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                getAlbumEntries()
            }
        }
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
//            R.id.item_menu_add_folder -> {
//                presenter.onAddFolderSelected()
//                true
//            }
            R.id.item_menu_add_album -> {
                presenter.onAddAlbumSelected()
                true
            }
            R.id.item_menu_add_artist -> {
                presenter.onAddArtistSelected()
                true
            }
            R.id.item_menu_add_mic -> {
                presenter.onAddMicSelected()
                true
            }
            R.id.item_menu_remove_track -> {
                presenter.onRemoveTrackSelected()
                true
            }
            R.id.item_menu_remove_all -> {
                presenter.onRemoveAllTracksSelected()
                true
            }
            R.id.item_menu_export_playlist -> {
                presenter.onExportPlaylistSelected()
                true
            }
            R.id.item_menu_import_playlist -> {
                presenter.onImportPlaylistSelected()
                true
            }
            R.id.item_show_manual -> {
                presenter.onShowManualSelected()
                true
            }
            R.id.item_show_my_ip -> {
                presenter.onShowIPSelected()
                true
            }
            R.id.item_review_app -> {
                presenter.onReviewAppSelected()
                true
            }
            R.id.item_receiver_android -> {
                presenter.onReceiverAndroidSelected()
                true
            }
            R.id.item_receiver_javafx -> {
                presenter.onReceiverFXSelected()
                true
            }
            R.id.item_transmitter_javafx -> {
                presenter.onTransmitterFXSelected()
                true
            }
            R.id.item_source_code -> {
                presenter.onSourceCodeSelected()
                true
            }
            R.id.item_dev_site -> {
                presenter.onDevSiteSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TAG_EDITOR && resultCode == Activity.RESULT_OK) {
            presenter.onReturnFromTagEditor()
        }
    }

    override fun onBackPressed() = presenter.onBackPressed()

    override fun showTracks(tracks: List<Track>, currentPosition: Int) {
        val trackItems = tracks.mapIndexed { index, track ->
            TrackItem(track, index, index == currentPosition)
        }
        runOnUiThread {
            tracksAdapter.update(trackItems)
        }
    }

    override fun scrollToPosition(position: Int) {
        val layoutManager = tracksRV.layoutManager
        if (layoutManager is LinearLayoutManager) {
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    override fun showWifiStatus(isWifiOk: Boolean) {
        if (isWifiOk) {
            wifiNoIV.visibility = View.GONE
            wifiOkIV.visibility = View.VISIBLE
        } else {
            wifiNoIV.visibility = View.VISIBLE
            wifiOkIV.visibility = View.GONE
        }
    }

    override fun showWifiReceiverCount(count: Int) {
        wifiReceiverCount.text = count.toString()
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
            .withFilter(false, false, "mp3", "flac")
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

    override fun tryShowArtistSelectDialog() {
        tryShowMusicSelectDialog {
            getArtistEntries()
        }
    }

    override fun tryShowAlbumSelectDialog() {
        tryShowMusicSelectDialog {
            getAlbumEntries()
        }
    }

    override fun tryShowTrackSelectDialog() {
        tryShowMusicSelectDialog {
            getTrackEntries()
        }
    }

    private fun tryShowMusicSelectDialog(requestEntries: () -> List<MusicEntry>) {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        val pd = ProgressDialog()
                        pd.msg = getString(R.string.message_music_loading)
                        pd.isCancelable = false
                        pd.show(supportFragmentManager, "longClickDialog")
                        withContext(Dispatchers.IO) {
                            val entries = requestEntries()
                            withContext(Dispatchers.Main) {
                                pd.dismiss()
                                val dialog = MusicSelectorDialog()
                                dialog.entries = entries
                                dialog.onEntrySelected = { entry ->
                                    presenter.addFiles(getFilesByEntry(entry))
                                }
                                dialog.show(supportFragmentManager, "musicSelectorDialog")
                            }
                        }
                    }
                }
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                showToast(R.string.toast_no_sdcard_read_access)
            }
        }

        checkMediaPermissions(permissionListener)
    }

    private fun checkMediaPermissions(permissionListener: PermissionListener) {
        if (Build.VERSION.SDK_INT >= 30) {
            TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .check()
        } else {
            TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .check()
        }
    }

    private fun getMusicExternalContentUri() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    private fun allMusicQuery(projection: Array<String>, sortOrder: String): Cursor? {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                MediaStore.Files.FileColumns.MIME_TYPE + " = ? OR " +
                MediaStore.Files.FileColumns.MIME_TYPE + " = ?)"
        val mimeTypeMP3 = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
        val mimeTypeFLAC = MimeTypeMap.getSingleton().getMimeTypeFromExtension("flac")

        return contentResolver.query(
            getMusicExternalContentUri(),
            projection,
            selection,
            arrayOf(mimeTypeMP3, mimeTypeFLAC),
            sortOrder
        )
    }

    private fun getArtistEntries(): List<MusicEntry> {
        val sortOrder = MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                entries.add(ArtistEntry(artist))
            }
        }

        return entries.distinct()
    }

    private fun getAlbumEntries(): List<MusicEntry> {
        val sortOrder = MediaStore.Audio.Media.ALBUM + " || " +
            MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                val album = it.getString(1)
                val albumEntry = AlbumEntry(artist, album)
                entries.add(albumEntry)

                val path = it.getString(2)
                Log.e("path", path)

                if (!AlbumArtKeeper.albumArts.containsKey(albumEntry)) {
                    AlbumArtKeeper.albumArts[albumEntry] = AlbumArtKeeper.retrieveAlbumArt(this, path)
                }
            }
        }

        return entries.distinct()
    }

    private fun getTrackEntries(): List<MusicEntry> {
        val sortOrder =
                MediaStore.Audio.Media.TITLE + " || " +
                MediaStore.Audio.Media.ALBUM + " || " +
                MediaStore.Audio.Media.ARTIST + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )

        val cursor = allMusicQuery(projection, sortOrder)

        val entries = arrayListOf<MusicEntry>()

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(0)
                val album = it.getString(1)
                val title = it.getString(2)

                val trackEntry = TrackEntry(artist, album, title)

                entries.add(trackEntry)

                val path = it.getString(3)

                if (!AlbumArtKeeper.albumArts.containsKey(trackEntry.albumEntry)) {
                    AlbumArtKeeper.albumArts[trackEntry.albumEntry] = AlbumArtKeeper.retrieveAlbumArt(this, path)
                }
            }
        }

        return entries.distinct()
    }

    private fun getFilesByEntry(entry: MusicEntry): List<File> {
        val selection = when (entry) {
            is ArtistEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ?"
            is AlbumEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ? AND " +
                        MediaStore.Audio.Media.ALBUM + " = ?"
            is TrackEntry ->
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                        MediaStore.Audio.Media.ARTIST + " = ? AND " +
                        MediaStore.Audio.Media.ALBUM + " = ? AND " +
                        MediaStore.Audio.Media.TITLE + " = ?"
        }

        val sortOrder = MediaStore.Audio.Media.ARTIST + " || " +
                MediaStore.Audio.Media.ALBUM + " || " +
                " (10000 + " + MediaStore.Audio.Media.TRACK + ") || " +
                MediaStore.Audio.Media.TITLE + " ASC"


        val selectionArgs = when (entry) {
            is ArtistEntry -> arrayOf(entry.artist)
            is AlbumEntry -> arrayOf(entry.artist, entry.album)
            is TrackEntry -> arrayOf(entry.artist, entry.album, entry.title)
        }

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA
        )

        val cursor = contentResolver.query(
            getMusicExternalContentUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        val files = arrayListOf<File>()
        cursor?.use {
            while (it.moveToNext()) {
                val path = it.getString(0)
                files.add(File(path))
            }
        }

        val filteredFiles = files
            .filter { it.name.endsWith(".mp3") || it.name.endsWith(".flac") }

        if (files.size != filteredFiles.size) {
            showToast(R.string.toast_unsupported_files_format)
        }

        return filteredFiles
    }

    override fun showCurrentTime(currentMs: Float, trackLengthMs: Float) {
        if (trackLengthMs <= 0) return
        val progress = (currentMs * 1000 / trackLengthMs).toInt()
        seekBar.progress = progress
    }

    override fun showVolume(volume: Int) {
        volumeValueTV.text = getString(R.string.label_volume, volume)
    }

    override fun showRemoveTrackMessage() {
        showToast(R.string.toast_long_tap)
    }

    override fun showTrackLongClickDialog(position: Int) {
        val dialog = TrackLongClickDialog()
        dialog.onRemoveThisTrack = { presenter.onDeleteTrack(position) }
        dialog.onOpenTagEditor = { presenter.onOpenTagEditor(position) }
        dialog.show(supportFragmentManager, "longClickDialog")
    }

    override fun showSavePlaylistDialog() {
        val dialog = SavePlaylistDialog()
        dialog.onSavePlaylist = { presenter.onSavePlaylist(it) }
        dialog.show(supportFragmentManager, "savePlaylistDialog")
    }

    override fun showLoadPlaylistDialog(playlistNames: List<String>) {
        val dialog = LoadPlaylistDialog()
        dialog.playlistNames = playlistNames
        dialog.onLoadPlaylist = { presenter.onLoadPlaylist(it) }
        dialog.show(supportFragmentManager, "loadPlaylistDialog")
    }

    override fun showSavePlaylistSuccess() {
        showToast(R.string.toast_saving_playlist_success)
    }

    override fun showSavePlaylistError() {
        showToast(R.string.toast_saving_playlist_error)
    }

    override fun showLoadPlaylistSuccess() {
        showToast(R.string.toast_loading_playlist_success)
    }

    override fun showLoadPlaylistError() {
        showToast(R.string.toast_loading_playlist_error)
    }

    override fun showNoPlaylists() {
        showToast(R.string.toast_no_playlists)
    }


    override fun showIPAddress(ipAddress: String) {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.show_ip_title)
            .setMessage(ipAddress)
            .setNegativeButton(R.string.button_ok) { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun tryAddMic() {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                presenter.onAddMicPermissionsAccepted()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                showToast(R.string.toast_no_mic_access)
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setPermissions(Manifest.permission.RECORD_AUDIO)
            .check()
    }

    override fun trySavePlaylist(playlistName: String) {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                presenter.onSavePlaylistPermissionsAccepted(playlistName)
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                showToast(R.string.toast_no_sdcard_write_access)
            }
        }

        checkMediaPermissions(permissionListener)
    }

    override fun tryLoadPlaylists() {
        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                presenter.onLoadPlaylistsPermissionsAccepted()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                showToast(R.string.toast_no_sdcard_read_access)
            }
        }

        checkMediaPermissions(permissionListener)
    }

    override fun showTagEditor(uri: Uri) {
        val intent = Intent()
        intent.setClass(this, MusicEditorActivity::class.java)
        intent.data = uri
        startActivityForResult(intent, REQUEST_TAG_EDITOR)
    }

    override fun showManual() {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.manual_title)
            .setMessage(R.string.manual_message)
            .setNegativeButton(R.string.button_ok) { dialog, which -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun showReviewAppActivity() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TRANSMITTER_MARKET_URL1)
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TRANSMITTER_MARKET_URL2)
                )
            )
        }
    }

    override fun showReceiverAndroidActivity() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(RECEIVER_MARKET_URL1)
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(RECEIVER_MARKET_URL2)
                )
            )
        }
    }

    override fun showReceiverFXActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(FX_RECEIVER_URL)
            )
        )
    }

    override fun showTransmitterFXActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(FX_TRANSMITTER_URL)
            )
        )
    }

    override fun showSourceCodeActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(SOURCE_CODE_URL)
            )
        )
    }

    override fun showDevSiteActivity() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(DEV_SITE_URL)
            )
        )
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

    private fun initTracksRV() {
        tracksRV.adapter = tracksAdapter
        tracksAdapter.setOnItemClickListener { item, _ ->
            if (item is TrackItem) {
                presenter.onTrackClick(item.position)
            }
        }
        tracksAdapter.setOnItemLongClickListener { item, _ ->
            if (item is TrackItem) {
                presenter.onTrackLongClick(item.position)
                true
            } else {
                false
            }
        }
    }

    override fun showWrongFileFormatToast() {
        showToast(R.string.toast_wrong_file_format)
    }
}
