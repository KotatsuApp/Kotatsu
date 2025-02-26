package org.koitharu.kotatsu.core.ui.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.GravityInt
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.koitharu.kotatsu.core.util.ext.consumeRelative
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.start

class InsetsToMarginsListener(
	@GravityInt
	private val sides: Int,
	private val baseMargins: Insets,
) : OnApplyWindowInsetsListener {

	private val insetType = WindowInsetsCompat.Type.systemBars()

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(insetType)
		v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			if (sides and Gravity.START == Gravity.START) {
				marginStart = barsInsets.start(v) + baseMargins.start(v)
			}
			if (sides and Gravity.TOP == Gravity.TOP) {
				topMargin = barsInsets.top + baseMargins.top
			}
			if (sides and Gravity.END == Gravity.END) {
				marginEnd = barsInsets.end(v) + baseMargins.end(v)
			}
			if (sides and Gravity.BOTTOM == Gravity.BOTTOM) {
				bottomMargin = barsInsets.bottom + baseMargins.bottom
			}
		}
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
