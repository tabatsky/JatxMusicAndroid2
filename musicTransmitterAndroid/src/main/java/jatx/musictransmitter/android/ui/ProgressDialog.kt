package jatx.musictransmitter.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import jatx.musictransmitter.android.R

class ProgressDialog  : DialogFragment() {
    var msg: String = ""

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
        val v = inflater.inflate(R.layout.dialog_progress, container, false)

        val msgTV = v.findViewById<TextView>(R.id.msgTV)
        msgTV.text = msg

        return v
    }
}