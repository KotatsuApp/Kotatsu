package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference

/**
 * A MultiSelectListPreference that ensures at least one item is always selected.
 * If the user tries to deselect all items, it automatically keeps the fallback value selected.
 */
class MinimumSelectionMultiSelectPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
	defStyleRes: Int = 0
) : MultiSelectListPreference(context, attrs, defStyleAttr, defStyleRes) {

	/**
	 * The fallback value to select when user tries to deselect all items.
	 * Defaults to "en" (English) for translation languages.
	 */
	var fallbackValue: String = "en"

	override fun setValues(values: MutableSet<String>?) {
		val safeValues = values?.toMutableSet() ?: mutableSetOf()
		
		// Ensure at least one value is selected
		if (safeValues.isEmpty()) {
			// Add fallback value if no values are selected
			safeValues.add(fallbackValue)
		}
		
		super.setValues(safeValues)
	}

	override fun callChangeListener(newValue: Any?): Boolean {
		// Ensure the new value also follows our minimum selection rule
		if (newValue is Set<*>) {
			val stringSet = newValue.filterIsInstance<String>().toMutableSet()
			if (stringSet.isEmpty()) {
				stringSet.add(fallbackValue)
				return super.callChangeListener(stringSet)
			}
		}
		return super.callChangeListener(newValue)
	}
}