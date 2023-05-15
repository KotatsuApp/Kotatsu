package org.koitharu.kotatsu.utils.ext

import android.view.View
import androidx.core.graphics.Insets

fun Insets.end(view: View): Int {
	return if (view.isRtl) left else right
}

fun Insets.start(view: View): Int {
	return if (view.isRtl) right else left
}
