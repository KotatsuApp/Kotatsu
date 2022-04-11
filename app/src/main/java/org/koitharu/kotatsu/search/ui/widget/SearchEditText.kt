package org.koitharu.kotatsu.search.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.google.android.material.R
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.utils.ext.drawableStart

private const val DRAWABLE_END = 2

class SearchEditText @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

	var searchSuggestionListener: SearchSuggestionListener? = null
	private val clearIcon = ContextCompat.getDrawable(context, R.drawable.abc_ic_clear_material)

	var query: String
		get() = text?.trim()?.toString().orEmpty()
		set(value) {
			if (value != text?.toString()) {
				setText(value)
				setSelection(value.length)
			}
		}

	override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
			if (hasFocus()) {
				clearFocus()
				// return true
			}
		}
		return super.onKeyPreIme(keyCode, event)
	}

	override fun onEditorAction(actionCode: Int) {
		super.onEditorAction(actionCode)
		if (actionCode == EditorInfo.IME_ACTION_SEARCH) {
			searchSuggestionListener?.onQueryClick(query, submit = true)
		}
	}

	override fun onTextChanged(
		text: CharSequence?,
		start: Int,
		lengthBefore: Int,
		lengthAfter: Int,
	) {
		super.onTextChanged(text, start, lengthBefore, lengthAfter)
		setCompoundDrawablesRelativeWithIntrinsicBounds(
			drawableStart,
			null,
			if (text.isNullOrEmpty()) null else clearIcon,
			null,
		)
		searchSuggestionListener?.onQueryChanged(query)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.action == MotionEvent.ACTION_UP) {
			val drawable = compoundDrawablesRelative[DRAWABLE_END] ?: return super.onTouchEvent(event)
			val isOnDrawable = drawable.isVisible && if (layoutDirection == LAYOUT_DIRECTION_RTL) {
				event.x.toInt() in paddingLeft..(drawable.bounds.width() + paddingLeft)
			} else {
				event.x.toInt() in (width - drawable.bounds.width() - paddingRight)..(width - paddingRight)
			}
			if (isOnDrawable) {
				text?.clear()
				return true
			}
		}
		return super.onTouchEvent(event)
	}

	override fun clearFocus() {
		super.clearFocus()
		text?.clear()
	}
}