package org.koitharu.kotatsu.core.ui.util

import android.view.Gravity
import android.view.View
import androidx.annotation.GravityInt
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import org.koitharu.kotatsu.core.util.ext.consumeRelative
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.start

class InsetsToPaddingListener(
	@GravityInt
	private val sides: Int,
	private val basePaddings: Insets,
) : OnApplyWindowInsetsListener {

	private val insetType = WindowInsetsCompat.Type.systemBars()

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(insetType)
		v.setPaddingRelative(
			/* start = */
			if (sides and Gravity.START == Gravity.START) {
				barsInsets.start(v) + basePaddings.start(v)
			} else {
				v.paddingStart
			},
			/* top = */
			if (sides and Gravity.TOP == Gravity.TOP) {
				barsInsets.top + basePaddings.top
			} else {
				v.paddingTop
			},
			/* end = */
			if (sides and Gravity.END == Gravity.END) {
				barsInsets.end(v) + basePaddings.end(v)
			} else {
				v.paddingEnd
			},
			/* bottom = */
			if (sides and Gravity.BOTTOM == Gravity.BOTTOM) {
				barsInsets.bottom + basePaddings.bottom
			} else {
				v.paddingBottom
			},
		)
		return WindowInsetsCompat.Builder(insets)
			.setInsets(
				insetType,
				barsInsets.consumeRelative(
					v,
					start = sides and Gravity.START == Gravity.START,
					top = sides and Gravity.TOP == Gravity.TOP,
					end = sides and Gravity.END == Gravity.END,
					bottom = sides and Gravity.BOTTOM == Gravity.BOTTOM,
				),
			).build()
	}
}
