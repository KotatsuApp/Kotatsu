package org.koitharu.kotatsu.utils.ext

import android.os.Build
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import com.google.android.material.progressindicator.BaseProgressIndicator

fun ProgressBar.setProgressCompat(progress: Int, animate: Boolean) = when {
	this is BaseProgressIndicator<*> -> setProgressCompat(progress, animate)
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> setProgress(progress, animate)
	else -> setProgress(progress)
}

fun ProgressBar.showCompat() = when (this) {
	is BaseProgressIndicator<*> -> show()
	is ContentLoadingProgressBar -> show()
	else -> isVisible = true
}

fun ProgressBar.hideCompat() = when (this) {
	is BaseProgressIndicator<*> -> hide()
	is ContentLoadingProgressBar -> hide()
	else -> isVisible = false
}