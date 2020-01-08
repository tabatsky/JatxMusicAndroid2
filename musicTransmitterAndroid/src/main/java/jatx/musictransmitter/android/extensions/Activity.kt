package jatx.musictransmitter.android.extensions

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes

fun Activity.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun Activity.showToast(@StringRes resId: Int) {
    showToast(getString(resId))
}