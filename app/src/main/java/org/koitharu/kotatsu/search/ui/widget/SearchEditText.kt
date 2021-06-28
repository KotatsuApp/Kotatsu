package org.koitharu.kotatsu.search.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.R

class SearchEditText @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

	override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
			if (hasFocus()) {
				clearFocus()
				return true
			}
		}
		return super.onKeyPreIme(keyCode, event)
	}

	override fun clearFocus() {
		super.clearFocus()
		text?.clear()
	}
} 