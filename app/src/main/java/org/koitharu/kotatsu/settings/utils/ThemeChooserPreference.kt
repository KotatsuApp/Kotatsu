package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ColorScheme
import org.koitharu.kotatsu.databinding.ItemColorSchemeBinding

class ThemeChooserPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.themeChooserPreferenceStyle,
	defStyleRes: Int = R.style.Preference_ThemeChooser,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

	private val entries = ColorScheme.getAvailableList()
	private var currentValue: ColorScheme = ColorScheme.default
	private val itemClickListener = View.OnClickListener {
		val tag = it.tag as? ColorScheme ?: return@OnClickListener
		setValueInternal(tag.name, true)
	}

	var value: String
		get() = currentValue.name
		set(value) = setValueInternal(value, notifyChanged = true)

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val layout = holder.findViewById(R.id.linear) as? LinearLayout ?: return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			layout.suppressLayout(true)
		}
		layout.removeAllViews()
		for (theme in entries) {
			val context = ContextThemeWrapper(context, theme.styleResId)
			val item = ItemColorSchemeBinding.inflate(LayoutInflater.from(context), layout, false)
			item.card.isChecked = theme == currentValue
			item.textViewTitle.setText(theme.titleResId)
			item.root.tag = theme
			item.card.tag = theme
			item.imageViewCheck.isVisible = theme == currentValue
			item.root.setOnClickListener(itemClickListener)
			item.card.setOnClickListener(itemClickListener)
			layout.addView(item.root)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			layout.suppressLayout(false)
		}
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		value = getPersistedString(
			when (defaultValue) {
				is String -> ColorScheme.safeValueOf(defaultValue) ?: ColorScheme.default
				is ColorScheme -> defaultValue
				else -> ColorScheme.default
			}.name,
		)
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		return a.getInt(index, 0)
	}

	private fun setValueInternal(enumName: String, notifyChanged: Boolean) {
		val newValue = ColorScheme.safeValueOf(enumName) ?: return
		if (newValue != currentValue) {
			currentValue = newValue
			persistString(newValue.name)
			if (notifyChanged) {
				notifyChanged()
			}
		}
	}
}
