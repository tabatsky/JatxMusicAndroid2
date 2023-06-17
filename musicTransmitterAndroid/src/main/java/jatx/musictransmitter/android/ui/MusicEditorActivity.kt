package jatx.musictransmitter.android.ui

import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.OnBackPressedCallback
import dagger.Lazy
import jatx.extensions.showToast
import jatx.musictransmitter.android.App
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.presentation.MusicEditorPresenter
import jatx.musictransmitter.android.presentation.MusicEditorView
import kotlinx.android.synthetic.main.activity_music_editor.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import java.io.File
import javax.inject.Inject


private const val EDIT_REQUEST_CODE = 1237

class MusicEditorActivity : MvpAppCompatActivity(), MusicEditorView {
    @Inject
    lateinit var presenterProvider: Lazy<MusicEditorPresenter>
    private val presenter by moxyPresenter { presenterProvider.get() }

    private var needQuit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent?.injectMusicEditorActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_editor)

        val path = intent.data?.path
        path?.apply {
            presenter.onPathParsed(this)
        }

        saveBtn.setOnClickListener {
            presenter.onSaveClick(false)
        }

        winToUtfBtn.setOnClickListener {
            presenter.onWinToUtfClick(
                artist = artistET.text.toString(),
                album = albumET.text.toString(),
                title = titleET.text.toString()
            )
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                presenter.onBackPressed(
                    artist = artistET.text.toString(),
                    album = albumET.text.toString(),
                    title = titleET.text.toString(),
                    year = yearET.text.toString(),
                    number = numberET.text.toString()
                )
            }
        })
    }

    override fun showFileName(fileName: String) {
        fileNameTV.text = getString(R.string.label_filename, fileName)
    }

    override fun showTags(
        artist: String,
        album: String,
        title: String,
        year: String,
        number: String
    ) {
        artistET.setText(artist)
        albumET.setText(album)
        titleET.setText(title)
        yearET.setText(year)
        numberET.setText(number)
    }

    override fun saveTags(needQuit: Boolean) {
        this.needQuit = needQuit
        if (Build.VERSION.SDK_INT >= 30) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri("external"),
                mediaIDByFile(presenter.file)
            )
            val editPendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(uri))
            startIntentSenderForResult(editPendingIntent.intentSender, EDIT_REQUEST_CODE,
                null, 0, 0, 0)
        } else {
            onSaveTagsPermissionGranted()
        }
    }

    private fun mediaIDByFile(file: File): Long {
        var id: Long = 0
        val uri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.Audio.Media.DATA + " = ? "
        val selectionArgs = arrayOf(file.absolutePath)
        val projection = arrayOf(MediaStore.Audio.Media._ID)

        contentResolver
            .query(uri, projection, selection, selectionArgs, null)
            ?.use {
                while (it.moveToNext()) {
                    val idIndex = it.getColumnIndex(MediaStore.Audio.Media._ID)
                    id = it.getString(idIndex).toLong()
                }
            }

        return id
    }

    private fun onSaveTagsPermissionGranted() {
        presenter.onSaveTags(
            artist = artistET.text.toString(),
            album = albumET.text.toString(),
            title = titleET.text.toString(),
            year = yearET.text.toString(),
            number = numberET.text.toString()
        )
        MediaScannerConnection.scanFile(this, arrayOf(presenter.file.absolutePath), null, null)
        if (needQuit) {
            presenter.onNeedQuit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EDIT_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    onSaveTagsPermissionGranted()
                } else {
                    showToast(R.string.toast_no_sdcard_write_access)
                }
            }
        }
    }

    override fun showNeedToSaveDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Сохранить изменения?")
            .setPositiveButton("Да") { dialog, _ ->
                presenter.onSaveClick(true)
                dialog.dismiss()
            }
            .setNegativeButton(
                "Нет") { dialog, _ ->
                dialog.dismiss()
                presenter.onNeedQuit()
            }
            .create()
            .show()
    }

    override fun quit() {
        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun saveTagErrorToast() {
        showToast(R.string.toast_saving_tag_error)
    }
}