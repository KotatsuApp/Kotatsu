package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.collection.MutableScatterMap
import com.google.android.material.slider.Slider
import kotlin.math.cbrt
import kotlin.math.pow

class CubicSlider @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : Slider(context, attrs) {

	private val changeListeners = MutableScatterMap<OnChangeListener, OnChangeListenerMapper>(1)

	override fun setValue(value: Float) {
		super.setValue(value.unmap())
	}

	override fun getValue(): Float {
		return super.getValue().map()
	}

	override fun getValueFrom(): Float {
		return super.getValueFrom().map()
	}

	override fun setValueFrom(valueFrom: Float) {
		super.setValueFrom(valueFrom.unmap())
	}

	override fun getValueTo(): Float {
		return super.getValueTo().map()
	}

	override fun setValueTo(valueTo: Float) {
		super.setValueTo(valueTo.unmap())
	}

	override fun addOnChangeListener(listener: OnChangeListener) {
		val mapper = OnChangeListenerMapper(listener)
		super.addOnChangeListener(mapper)
		changeListeners[listener] = mapper
	}

	override fun removeOnChangeListener(listener: OnChangeListener) {
		changeListeners.remove(listener)?.let {
			super.removeOnChangeListener(it)
		}
	}

	override fun clearOnChangeListeners() {
		super.clearOnChangeListeners()
		changeListeners.clear()
	}

	private fun Float.map(): Float {
		return this.pow(3)
	}

	private fun Float.unmap(): Float {
		return cbrt(this)
	}

	private inner class OnChangeListenerMapper(
		private val delegate: OnChangeListener,
	) : OnChangeListener {

		override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
			delegate.onValueChange(slider, value.map(), fromUser)
		}
	}
}
