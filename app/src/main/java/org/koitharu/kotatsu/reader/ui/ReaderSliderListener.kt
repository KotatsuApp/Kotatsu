package org.koitharu.kotatsu.reader.ui

import com.google.android.material.slider.Slider
import org.koitharu.kotatsu.reader.ui.thumbnails.OnPageSelectListener

class ReaderSliderListener(
	private val pageSelectListener: OnPageSelectListener,
	private val viewModel: ReaderViewModel,
) : Slider.OnChangeListener, Slider.OnSliderTouchListener {

	private var isChanged = false

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			isChanged = true
		}
	}

	override fun onStartTrackingTouch(slider: Slider) {
		isChanged = false
	}

	override fun onStopTrackingTouch(slider: Slider) {
		if (isChanged) {
			switchPageToIndex(slider.value.toInt())
		}
	}

	fun attachToSlider(slider: Slider) {
		slider.addOnChangeListener(this)
		slider.addOnSliderTouchListener(this)
	}

	private fun switchPageToIndex(index: Int) {
		val pages = viewModel.getCurrentChapterPages()
		val page = pages?.getOrNull(index) ?: return
		pageSelectListener.onPageSelected(page)
	}
}
