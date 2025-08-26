package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
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

	var onTranslationSettingsClick: (() -> Unit)? = null

	init {
		gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
		setBackgroundResource(R.drawable.bg_reader_indicator)
		val padding = resources.getDimensionPixelOffset(R.dimen.margin_normal)
		setPadding(padding, padding / 2, padding, padding / 2)
		
		// Make it less intrusive by using smaller text
		textSize = 14f
		setTextColor(ContextCompat.getColor(context, android.R.color.white))
		
		// Enable link movement for clickable spans
		movementMethod = LinkMovementMethod.getInstance()
	}

	fun showTranslationSwitch(translationName: String) {
		val message = context.getString(R.string.translation_switched_to, translationName)
		show(message)
	}

	fun showNoFallback() {
		show(context.getString(R.string.translation_no_fallback))
	}

	fun showNavigationMessage(message: String) {
		// Check if this is a gap notification (contains "Skipped")
		if (message.contains("Skipped", ignoreCase = true)) {
			showGapWarning(message)
		} else if (message.contains("|SHOW_SETTINGS_LINK")) {
			// This is a translation switch - show with question mark link
			val cleanMessage = message.replace("|SHOW_SETTINGS_LINK", "")
			showTranslationSwitchWithSettings(cleanMessage)
		} else {
			show(message)
		}
	}
	
	private fun showGapWarning(message: String) {
		// Use reddish background for gap warnings
		val drawable = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(0xFFD32F2F.toInt()) // Material Design red 700
			cornerRadius = 12f // 12dp corner radius
		}
		background = drawable
		text = message
		visibility = View.VISIBLE
		animate()
			.alpha(1f)
			.setDuration(200)
			.withEndAction {
				// Keep gap warnings visible longer (4 seconds)
				postDelayed({
					hide()
				}, 4000)
			}
			.start()
	}

	private fun showTranslationSwitchWithSettings(message: String) {
		// Restore original background for normal messages
		setBackgroundResource(R.drawable.bg_reader_indicator)
		
		// Create spannable text with question mark link
		val spannableText = SpannableStringBuilder("$message ")
		val questionMark = "?"
		val startIndex = spannableText.length
		spannableText.append(questionMark)
		val endIndex = spannableText.length
		
		// Create clickable span for question mark
		val clickableSpan = object : ClickableSpan() {
			override fun onClick(widget: View) {
				onTranslationSettingsClick?.invoke()
			}
		}
		
		// Apply blue color and clickable behavior to question mark
		spannableText.setSpan(
			clickableSpan,
			startIndex,
			endIndex,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		
		// Make question mark blue
		val blueColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
		spannableText.setSpan(
			ForegroundColorSpan(blueColor),
			startIndex,
			endIndex,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		
		text = spannableText
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
	
	private fun show(message: String) {
		// Restore original background for normal messages
		setBackgroundResource(R.drawable.bg_reader_indicator)
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