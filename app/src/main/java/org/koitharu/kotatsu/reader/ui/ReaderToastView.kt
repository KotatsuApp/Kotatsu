package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.textview.MaterialTextView
import org.koitharu.kotatsu.utils.anim.Duration
import org.koitharu.kotatsu.utils.anim.Motion
import org.koitharu.kotatsu.utils.ext.hideAnimated
import org.koitharu.kotatsu.utils.ext.showAnimated

class ReaderToastView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {

	private var hideRunnable = Runnable {
		hide()
	}

	fun show(message: CharSequence, isLoading: Boolean) {
		removeCallbacks(hideRunnable)
		text = message
		this.showAnimated(Motion.Toast, Duration.SHORT)
	}

	fun show(@StringRes messageId: Int, isLoading: Boolean) {
		show(context.getString(messageId), isLoading)
	}

	fun showTemporary(message: CharSequence, duration: Long) {
		show(message, false)
		postDelayed(hideRunnable, duration)
	}

	fun hide() {
		removeCallbacks(hideRunnable)
		this.hideAnimated(Motion.Toast, Duration.SHORT)
	}

	override fun onDetachedFromWindow() {
		removeCallbacks(hideRunnable)
		super.onDetachedFromWindow()
	}

	// FIXME use it as compound drawable
	private fun createProgressDrawable(): CircularProgressDrawable {
		val drawable = CircularProgressDrawable(context)
		drawable.setStyle(CircularProgressDrawable.DEFAULT)
		drawable.arrowEnabled = false
		drawable.setColorSchemeColors(Color.WHITE)
		drawable.centerRadius = lineHeight / 3f
		return drawable
	}
}