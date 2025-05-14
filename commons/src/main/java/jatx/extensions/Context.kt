package jatx.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import jatx.debug.R


fun Context.showToast(text: String) {
    val handler = Handler(mainLooper)

    handler.post {
        val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        val view = toast.view

        view?.setBackgroundResource(R.drawable.background_toast)

        val text = view?.findViewById(android.R.id.message) as TextView?
        text?.setBackgroundColor(resources.getColor(R.color.gray_aa))
        text?.setTextColor(resources.getColor(R.color.black))

        toast.show()
    }
}

fun Context.showToast(@StringRes resId: Int) {
    showToast(getString(resId))
}

fun Context.registerExportedReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
    ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
}