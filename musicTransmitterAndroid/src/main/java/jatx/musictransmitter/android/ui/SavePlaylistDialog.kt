package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import jatx.extensions.showToast
import jatx.musictransmitter.android.R

class SavePlaylistDialog: DialogFragment() {
    var onSavePlaylist: (String) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val style = STYLE_NORMAL
        val theme = R.style.Theme_MusicTransmitter_Dialog

        setStyle(style, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_save_playlist, container, false)

        val cancelBtn = v.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = v.findViewById<Button>(R.id.saveBtn)
        val playlistNameEt = v.findViewById<EditText>(R.id.playlistNameEt)

        cancelBtn.setOnClickListener { dismiss() }
        saveBtn.setOnClickListener {
            val playlistName = playlistNameEt.text.toString().trim()
            if (playlistName.isNotEmpty()) {
                dismiss()
                onSavePlaylist(playlistName)
            } else {
                v.context.showToast(R.string.toast_empty_playlist_name)
            }
        }

        return v
    }
}