package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.customview.view.AbsSavedState
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.setValueRounded

class SliderPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.sliderPreferenceStyle,
	defStyleRes: Int = R.style.Preference_Slider,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

	private var valueFrom: Int = 0
	private var valueTo: Int = 100
	private var stepSize: Int = 1
	private var currentValue: Int = 0
	private var isTickVisible: Boolean = false

	var value: Int
		get() = currentValue
		set(value) = setValueInternal(value, notifyChanged = true)

	private val sliderListener = Slider.OnChangeListener { _, value, fromUser ->
		if (fromUser) {
			syncValueInternal(value.toInt())
		}
	}

	init {
		context.withStyledAttributes(
			attrs,
			R.styleable.SliderPreference,
			defStyleAttr,
			defStyleRes,
		) {
			valueFrom = getFloat(
				R.styleable.SliderPreference_android_valueFrom,
				valueFrom.toFloat(),
			).toInt()
			valueTo = getFloat(R.styleable.SliderPreference_android_valueTo, valueTo.toFloat()).toInt()
			stepSize = getFloat(R.styleable.SliderPreference_android_stepSize, stepSize.toFloat()).toInt()
			isTickVisible = getBoolean(R.styleable.SliderPreference_tickVisible, isTickVisible)
		}
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val slider = holder.findViewById(R.id.slider) as? Slider ?: return
		slider.removeOnChangeListener(sliderListener)
		slider.addOnChangeListener(sliderListener)
		slider.valueFrom = valueFrom.toFloat()
		slider.valueTo = valueTo.toFloat()
		slider.stepSize = stepSize.toFloat()
		slider.isTickVisible = isTickVisible
		slider.setValueRounded(currentValue.toFloat())
		slider.isEnabled = isEnabled
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		value = getPersistedInt(defaultValue as? Int ?: 0)
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		return a.getInt(index, 0)
	}

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState()
		if (superState == null || isPersistent) {
			return superState
		}
		return SavedState(
			superState = superState,
			valueFrom = valueFrom,
			valueTo = valueTo,
			currentValue = currentValue,
		)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state !is SavedState) {
			super.onRestoreInstanceState(state)
			return
		}
		super.onRestoreInstanceState(state.superState)
		valueFrom = state.valueFrom
		valueTo = state.valueTo
		currentValue = state.currentValue
		notifyChanged()
	}

	private fun setValueInternal(sliderValue: Int, notifyChanged: Boolean) {
		val newValue = sliderValue.coerceIn(valueFrom, valueTo)
		if (newValue != currentValue) {
			currentValue = newValue
			persistInt(newValue)
			if (notifyChanged) {
				notifyChanged()
			}
		}
	}

	private fun syncValueInternal(sliderValue: Int) {
		if (sliderValue != currentValue) {
			if (callChangeListener(sliderValue)) {
				setValueInternal(sliderValue, notifyChanged = true)
			}
		}
	}

	private class SavedState : AbsSavedState {

		val valueFrom: Int
		val valueTo: Int
		val currentValue: Int

		constructor(
			superState: Parcelable,
			valueFrom: Int,
			valueTo: Int,
			currentValue: Int,
		) : super(superState) {
			this.valueFrom = valueFrom
			this.valueTo = valueTo
			this.currentValue = currentValue
		}

		constructor(source: Parcel, classLoader: ClassLoader?) : super(source, classLoader) {
			valueFrom = source.readInt()
			valueTo = source.readInt()
			currentValue = source.readInt()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(valueFrom)
			out.writeInt(valueTo)
			out.writeInt(currentValue)
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
}
