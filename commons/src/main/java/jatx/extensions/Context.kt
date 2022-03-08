package jatx.extensions

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import jatx.debug.R


fun Context.showToast(text: String) {
    val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
    val view = toast.view

    view?.setBackgroundResource(R.drawable.background_toast)

    val text = view?.findViewById(android.R.id.message) as TextView?
    text?.setBackgroundColor(resources.getColor(R.color.gray_aa))
    text?.setTextColor(resources.getColor(R.color.black))

    toast.show()
}

fun Context.showToast(@StringRes resId: Int) {
    showToast(getString(resId))
}