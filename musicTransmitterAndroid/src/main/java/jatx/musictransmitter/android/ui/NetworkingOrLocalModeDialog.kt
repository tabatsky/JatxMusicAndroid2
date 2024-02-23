package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.databinding.DialogNetworkingOrLocalModeBinding

class NetworkingOrLocalModeDialog() : DialogFragment() {
    var isLocalMode = false
    var onSetLocalMode: (Boolean) -> Unit = {}

    private val binding: DialogNetworkingOrLocalModeBinding by viewBinding(createMethod = CreateMethod.INFLATE)

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
            networkingOrLocalModeCB.isChecked = isLocalMode
            networkingOrLocalModeCB.setOnCheckedChangeListener { _, isChecked ->
                onSetLocalMode(isChecked)
            }
            return root
        }
    }
}