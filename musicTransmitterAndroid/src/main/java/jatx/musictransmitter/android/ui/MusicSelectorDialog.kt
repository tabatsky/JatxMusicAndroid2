package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.databinding.DialogMusicSelectorBinding
import jatx.musictransmitter.android.media.MusicEntry
import jatx.musictransmitter.android.ui.adapters.MusicSelectorAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicSelectorDialog: DialogFragment() {

    private lateinit var adapter: MusicSelectorAdapter

    private val binding: DialogMusicSelectorBinding by viewBinding(createMethod = CreateMethod.INFLATE)

    var entries: List<MusicEntry> = listOf()
        set(value) {
            field = value
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
    ): View {
        with(binding) {
            musicSelectorRV.layoutManager = LinearLayoutManager(root.context)
            adapter = MusicSelectorAdapter().also {
                musicSelectorRV.adapter = it
                it.onItemClickListener = { position ->
                    onEntrySelected.invoke(entries[position])
                    dismiss()
                }
                it.submitList(entries)
            }

            searchET.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(p0: Editable?) = filterItems(p0.toString())
            })

            return root
        }
    }

    private fun filterItems(searchString: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val filteredEntries = entries
                    .filter {
                        it.searchString
                            .contains(searchString.lowercase().trim())
                    }
                withContext(Dispatchers.Main) {
                    adapter.onItemClickListener = { position ->
                        onEntrySelected.invoke(filteredEntries[position])
                        dismiss()
                    }
                    adapter.submitList(filteredEntries)
                }
            }
        }
    }
}
