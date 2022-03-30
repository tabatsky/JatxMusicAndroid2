package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import jatx.musictransmitter.android.R

class LoadPlaylistDialog: DialogFragment() {
    var onLoadPlaylist: (String) -> Unit = {}
    var playlistNames = listOf<String>()

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
        val v = inflater.inflate(R.layout.dialog_load_playlist, container, false)

        val cancelBtn = v.findViewById<Button>(R.id.cancelBtn)
        val loadBtn = v.findViewById<Button>(R.id.loadBtn)
        val playlistNamesSpinner = v.findViewById<Spinner>(R.id.playlistNamesSpinner)

        ArrayAdapter(
            v.context,
            android.R.layout.simple_spinner_item,
            playlistNames
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            playlistNamesSpinner.adapter = adapter
        }

        cancelBtn.setOnClickListener { dismiss() }
        loadBtn.setOnClickListener {
            dismiss()
            val position = playlistNamesSpinner.selectedItemPosition
            val playlistName = playlistNames[position]
            onLoadPlaylist(playlistName)
        }

        return v
    }
}