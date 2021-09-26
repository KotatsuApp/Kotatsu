package org.koitharu.kotatsu.search.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.R
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener

class SearchEditText @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

	var searchSuggestionListener: SearchSuggestionListener? = null

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
		searchSuggestionListener?.onQueryChanged(query)
	}

	override fun clearFocus() {
		super.clearFocus()
		text?.clear()
	}
} 