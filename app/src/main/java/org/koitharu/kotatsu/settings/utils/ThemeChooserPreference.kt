package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.customview.view.AbsSavedState
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
	private val lastScrollPosition = intArrayOf(-1)
	private val itemClickListener = View.OnClickListener {
		val tag = it.tag as? ColorScheme ?: return@OnClickListener
		setValueInternal(tag.name, true)
	}
	private var scrollPersistListener: ScrollPersistListener? = null

	var value: String
		get() = currentValue.name
		set(value) = setValueInternal(value, notifyChanged = true)

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val layout = holder.findViewById(R.id.linear) as? LinearLayout ?: return
		val scrollView = holder.findViewById(R.id.scrollView) as? HorizontalScrollView ?: return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			scrollView.suppressLayout(true)
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
		if (lastScrollPosition[0] >= 0) {
			val scroller = Scroller(scrollView, lastScrollPosition[0])
			scroller.run()
			scrollView.post(scroller)
		}
		scrollView.viewTreeObserver.run {
			scrollPersistListener?.let { removeOnScrollChangedListener(it) }
			scrollPersistListener = ScrollPersistListener(scrollView, lastScrollPosition)
			addOnScrollChangedListener(scrollPersistListener)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			layout.suppressLayout(false)
			scrollView.suppressLayout(false)
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

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState() ?: return null
		return SavedState(
			superState = superState,
			scrollPosition = lastScrollPosition[0],
		)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state !is SavedState) {
			super.onRestoreInstanceState(state)
			return
		}
		super.onRestoreInstanceState(state.superState)
		lastScrollPosition[0] = state.scrollPosition
		// notifyChanged()
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

	private class SavedState : AbsSavedState {

		val scrollPosition: Int

		constructor(
			superState: Parcelable,
			scrollPosition: Int
		) : super(superState) {
			this.scrollPosition = scrollPosition
		}

		constructor(source: Parcel, classLoader: ClassLoader?) : super(source, classLoader) {
			scrollPosition = source.readInt()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(scrollPosition)
		}

		companion object {
			@Suppress("unused")
			@JvmField
			val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
				override fun createFromParcel(`in`: Parcel) = SavedState(`in`, SavedState::class.java.classLoader)

				override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
			}
		}
	}

	private class ScrollPersistListener(
		private val scrollView: HorizontalScrollView,
		private val lastScrollPosition: IntArray,
	) : ViewTreeObserver.OnScrollChangedListener {

		override fun onScrollChanged() {
			lastScrollPosition[0] = scrollView.scrollX
		}
	}

	private class Scroller(
		private val scrollView: HorizontalScrollView,
		private val position: Int,
	) : Runnable {

		override fun run() {
			scrollView.scrollTo(position, 0)
		}
	}
}
