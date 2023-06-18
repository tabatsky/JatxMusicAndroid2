package jatx.musicreceiver.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import jatx.extensions.onItemSelected
import jatx.extensions.setContent
import jatx.extensions.showToast
import jatx.musicreceiver.android.App
import jatx.musicreceiver.android.R
import jatx.musicreceiver.android.data.Settings
import jatx.musicreceiver.android.databinding.DialogSelectHostBinding
import javax.inject.Inject

class SelectHostDialog : DialogFragment() {
    @Inject
    lateinit var settings: Settings

    var onOk: () -> Unit = {}
    var onExit: () -> Unit = {}

    private val binding: DialogSelectHostBinding by viewBinding(createMethod = CreateMethod.INFLATE)

    init {
        this.isCancelable = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent?.injectSelectHostDialog(this)

        super.onCreate(savedInstanceState)

        val style = STYLE_NORMAL
        val theme: Int = androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog

        setStyle(style, theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        with(binding) {
            hostSpinner.setContent(settings.allHosts)
            hostSpinner.setSelection(settings.allHosts.indexOf(settings.host))

            hostSpinner.onItemSelected { position ->
                if (position > 0) {
                    hostnameET.setText(settings.allHosts[position])
                } else {
                    hostnameET.setText("")
                }
            }

            hostnameET.setText(settings.host)

            okBtn.setOnClickListener {
                val host = hostnameET.text.toString()

                if (host.isEmpty()) {
                    context?.showToast(R.string.toast_empty_host)
                    return@setOnClickListener
                }

                settings.host = host

                if (!settings.allHosts.contains(host)) {
                    val allHosts = arrayListOf<String>()
                    allHosts.addAll(settings.allHosts)
                    allHosts.add(host)
                    settings.allHosts = allHosts
                }

                dismiss()
                onOk()
            }

            exitBtn.setOnClickListener {
                dismiss()
                onExit()
            }

            return root
        }
    }
}