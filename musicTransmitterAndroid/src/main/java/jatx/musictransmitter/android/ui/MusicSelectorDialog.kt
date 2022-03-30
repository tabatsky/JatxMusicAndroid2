package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.media.MusicEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicSelectorDialog: DialogFragment() {

    private var items: List<MusicSelectorItem> = listOf()
    private lateinit var adapter: GroupAdapter<GroupieViewHolder>

    var entries: List<MusicEntry> = listOf()
        set(value) {
            field = value
            items = value.map { entry ->
                MusicSelectorItem(entry) {
                    dismiss()
                    onEntrySelected(entry)
                }
            }
        }
    var onEntrySelected: (MusicEntry) -> Unit = {}

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
        val v = inflater.inflate(R.layout.dialog_music_selector, container, false)

        val musicSelectorRV = v.findViewById<RecyclerView>(R.id.musicSelectorRV)
        musicSelectorRV.layoutManager = LinearLayoutManager(v.context)
        adapter = GroupAdapter<GroupieViewHolder>().also {
            musicSelectorRV.adapter = it
            it.update(items)
        }

        val searchET = v.findViewById<EditText>(R.id.searchET)
        searchET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) = filterItems(p0.toString())
        })

        return v
    }

    private fun filterItems(searchString: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val filteredItems = items
                    .filter {
                        it.entry.searchString
                            .contains(searchString.lowercase().trim())
                    }
                adapter.updateAsync(filteredItems, true, null)
            }
        }
    }
}
