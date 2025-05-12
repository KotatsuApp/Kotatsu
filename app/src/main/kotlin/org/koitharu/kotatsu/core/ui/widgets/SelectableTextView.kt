package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.os.Build
import android.text.Selection
import android.text.Spannable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.PointerIcon
import androidx.annotation.AttrRes
import androidx.core.view.PointerIconCompat
import com.google.android.material.textview.MaterialTextView

class SelectableTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
) : MaterialTextView(context, attrs, defStyleAttr) {

	init {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			pointerIcon = PointerIcon.getSystemIcon(context, PointerIconCompat.TYPE_TEXT)
		}
	}

	override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
		fixSelectionRange()
		return super.dispatchTouchEvent(event)
	}

	// https://stackoverflow.com/questions/22810147/error-when-selecting-text-from-textview-java-lang-indexoutofboundsexception-se
	private fun fixSelectionRange() {
		if (selectionStart < 0 || selectionEnd < 0) {
			val spannableText = text as? Spannable ?: return
			Selection.setSelection(spannableText, spannableText.length)
		}
	}

	override fun scrollTo(x: Int, y: Int) {
		super.scrollTo(0, 0)
	}

	fun selectAll() {
		val spannableText = text as? Spannable ?: return
		Selection.selectAll(spannableText)
	}
}
