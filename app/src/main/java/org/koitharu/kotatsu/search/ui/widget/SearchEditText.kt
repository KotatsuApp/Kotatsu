package org.koitharu.kotatsu.search.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.google.android.material.R as materialR
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.utils.ext.drawableEnd
import org.koitharu.kotatsu.utils.ext.drawableStart

private const val DRAWABLE_END = 2

class SearchEditText @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = materialR.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

	var searchSuggestionListener: SearchSuggestionListener? = null
	private val clearIcon = ContextCompat.getDrawable(context, materialR.drawable.abc_ic_clear_material)
	private val voiceIcon = ContextCompat.getDrawable(context, R.drawable.ic_voice_input)
	private var isEmpty = text.isNullOrEmpty()

	var isVoiceSearchEnabled: Boolean = false
		set(value) {
			field = value
			updateActionIcon()
		}

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
		val empty = text.isNullOrEmpty()
		if (isEmpty != empty) {
			isEmpty = empty
			updateActionIcon()
		}
		searchSuggestionListener?.onQueryChanged(query)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		super.onRestoreInstanceState(state)
		updateActionIcon()
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
				sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
				playSoundEffect(SoundEffectConstants.CLICK)
				onActionIconClick()
				return true
			}
		}
		return super.onTouchEvent(event)
	}

	override fun clearFocus() {
		super.clearFocus()
		text?.clear()
	}

	private fun onActionIconClick() {
		when {
			!text.isNullOrEmpty() -> text?.clear()
			isVoiceSearchEnabled -> searchSuggestionListener?.onVoiceSearchClick()
		}
	}

	private fun updateActionIcon() {
		val icon = when {
			!text.isNullOrEmpty() -> clearIcon
			isVoiceSearchEnabled -> voiceIcon
			else -> null
		}
		if (icon !== drawableEnd) {
			setCompoundDrawablesRelativeWithIntrinsicBounds(drawableStart, null, icon, null)
		}
	}
}