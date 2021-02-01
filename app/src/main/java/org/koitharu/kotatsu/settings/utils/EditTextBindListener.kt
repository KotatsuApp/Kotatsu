package org.koitharu.kotatsu.settings.utils

import android.widget.EditText
import androidx.preference.EditTextPreference

class EditTextBindListener(
	private val inputType: Int,
	private val hint: String
) : EditTextPreference.OnBindEditTextListener {

	override fun onBindEditText(editText: EditText) {
		editText.inputType = inputType
		editText.hint = hint
	}
}