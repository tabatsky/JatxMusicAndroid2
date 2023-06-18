package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.databinding.DialogTrackLongClickBinding

class TrackLongClickDialog : DialogFragment() {
    var onRemoveThisTrack: () -> Unit = {}
    var onOpenTagEditor: () -> Unit = {}

    private val binding: DialogTrackLongClickBinding by viewBinding(createMethod = CreateMethod.INFLATE)

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
            cancelBtn.setOnClickListener { dismiss() }
            removeThisTrackBtn.setOnClickListener {
                onRemoveThisTrack()
                dismiss()
            }
            openTagEditorBtn.setOnClickListener {
                onOpenTagEditor()
                dismiss()
            }

            return root
        }
    }
}