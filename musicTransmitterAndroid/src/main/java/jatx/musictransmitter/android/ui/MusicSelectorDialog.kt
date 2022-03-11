package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import jatx.musictransmitter.android.R

class MusicSelectorDialog: DialogFragment() {

    var entries: List<MusicEntry> = listOf()
    var onEntrySelected: (MusicEntry) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val style = STYLE_NORMAL
        val theme = R.style.Theme_AppCompat_DayNight_Dialog

        setStyle(style, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_music_selector, container, false)

        val musicSelectorRV = v.findViewById<RecyclerView>(R.id.musicSelectorRV)
        musicSelectorRV.layoutManager = LinearLayoutManager(v.context)
        GroupAdapter<GroupieViewHolder>().also {
            musicSelectorRV.adapter = it
            it.update(
                entries.map { entry ->
                    MusicSelectorItem(entry) {
                        dismiss()
                        onEntrySelected(entry)
                    }
                }
            )
        }

        return v
    }
}
