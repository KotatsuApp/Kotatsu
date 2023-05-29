package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.withStyledAttributes
import androidx.preference.EditTextPreference
import org.koitharu.kotatsu.R

class AutoCompleteTextViewPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = R.attr.autoCompleteTextViewPreferenceStyle,
	@StyleRes defStyleRes: Int = R.style.Preference_AutoCompleteTextView,
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

	private val autoCompleteBindListener = AutoCompleteBindListener()
	var entries: Array<String> = emptyArray()

	init {
		super.setOnBindEditTextListener(autoCompleteBindListener)
		context.withStyledAttributes(attrs, R.styleable.AutoCompleteTextViewPreference, defStyleAttr, defStyleRes) {
			val entriesId = getResourceId(R.styleable.AutoCompleteTextViewPreference_android_entries, 0)
			if (entriesId != 0) {
				setEntries(entriesId)
			}
		}
	}

	fun setEntries(@ArrayRes arrayResId: Int) {
		this.entries = context.resources.getStringArray(arrayResId)
	}

	fun setEntries(entries: Collection<String>) {
		this.entries = entries.toTypedArray()
	}

	override fun setOnBindEditTextListener(onBindEditTextListener: OnBindEditTextListener?) {
		autoCompleteBindListener.delegate = onBindEditTextListener
	}

	private inner class AutoCompleteBindListener : OnBindEditTextListener {

		var delegate: OnBindEditTextListener? = null

		override fun onBindEditText(editText: EditText) {
			delegate?.onBindEditText(editText)
			if (editText !is AutoCompleteTextView || entries.isEmpty()) {
				return
			}
			editText.threshold = 0
			editText.setAdapter(ArrayAdapter(editText.context, android.R.layout.simple_spinner_dropdown_item, entries))
			(editText.parent as? ViewGroup)?.findViewById<View>(R.id.dropdown)?.setOnClickListener {
				editText.showDropDown()
			}
		}
	}
}
