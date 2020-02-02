package org.koitharu.kotatsu.utils.ext

import android.content.Context
import androidx.appcompat.app.AlertDialog

fun Context.showDialog(block: AlertDialog.Builder.() -> Unit): AlertDialog {
    return AlertDialog.Builder(this)
        .apply(block)
        .show()
}