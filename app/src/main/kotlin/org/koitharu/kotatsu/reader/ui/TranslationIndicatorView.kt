package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import org.koitharu.kotatsu.R

class TranslationIndicatorView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : MaterialTextView(context, attrs, defStyleAttr) {

	init {
		gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
		setBackgroundResource(R.drawable.bg_reader_indicator)
		val padding = resources.getDimensionPixelOffset(R.dimen.margin_normal)
		setPadding(padding, padding / 2, padding, padding / 2)
		
		// Make it less intrusive by using smaller text
		textSize = 14f
		setTextColor(ContextCompat.getColor(context, android.R.color.white))
	}

	fun showTranslationSwitch(translationName: String) {
		val message = context.getString(R.string.translation_switched_to, translationName)
		show(message)
	}

	fun showNoFallback() {
		show(context.getString(R.string.translation_no_fallback))
	}

	fun showNavigationMessage(message: String) {
		show(message)
	}

	private fun show(message: String) {
		text = message
		visibility = View.VISIBLE
		animate()
			.alpha(1f)
			.setDuration(200)
			.withEndAction {
				// Auto-hide after 3 seconds
				postDelayed({
					hide()
				}, 3000)
			}
			.start()
	}

	private fun hide() {
		animate()
			.alpha(0f)
			.setDuration(200)
			.withEndAction {
				visibility = View.GONE
			}
			.start()
	}
}