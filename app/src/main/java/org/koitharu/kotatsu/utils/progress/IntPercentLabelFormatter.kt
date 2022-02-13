package org.koitharu.kotatsu.utils.progress

import com.google.android.material.slider.LabelFormatter

class IntPercentLabelFormatter : LabelFormatter {
	override fun getFormattedValue(value: Float) = "%d%%".format(value.toInt())
}