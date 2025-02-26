package org.koitharu.kotatsu.core.ui.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.GravityInt
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.koitharu.kotatsu.core.util.ext.consumeRelative
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.start

class InsetsToMarginsListener(
	@GravityInt
	private val sides: Int,
) : OnApplyWindowInsetsListener {

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			if (sides and Gravity.START == Gravity.START) marginStart = barsInsets.start(v)
			if (sides and Gravity.TOP == Gravity.TOP) topMargin = barsInsets.top
			if (sides and Gravity.END == Gravity.END) marginEnd = barsInsets.end(v)
			if (sides and Gravity.BOTTOM == Gravity.BOTTOM) bottomMargin = barsInsets.bottom
		}
		return WindowInsetsCompat.Builder(insets)
			.setInsets(
				WindowInsetsCompat.Type.systemBars(),
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
