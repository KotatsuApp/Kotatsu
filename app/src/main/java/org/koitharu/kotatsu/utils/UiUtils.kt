package org.koitharu.kotatsu.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import kotlin.math.abs
import kotlin.math.roundToInt

object UiUtils {

	@JvmStatic
	@ColorInt
	fun invertColor(@ColorInt color: Int): Int {
		val red = Color.red(color)
		val green = Color.green(color)
		val blue = Color.blue(color)
		val alpha = Color.alpha(color)
		return Color.argb(alpha, 255 - red, 255 - green, 255 - blue)
	}

	@JvmStatic
	fun resolveGridSpanCount(context: Context, width: Int = 0): Int {
		val cellWidth = context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
		val screenWidth = (if (width <= 0) {
			context.resources.displayMetrics.widthPixels
		} else width).toDouble()
		val estimatedCount = (screenWidth / cellWidth).roundToInt()
		return estimatedCount.coerceAtLeast(2)
	}

	@JvmStatic
	fun isTablet(context: Context) = context.resources.getBoolean(R.bool.is_tablet)

	object SpanCountResolver : View.OnLayoutChangeListener {
		override fun onLayoutChange(
			v: View?, left: Int, top: Int, right: Int, bottom: Int,
			oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
		) {
			val rv = v as? RecyclerView ?: return
			val width = abs(right - left)
			if (width == 0) {
				return
			}
			(rv.layoutManager as? GridLayoutManager)?.spanCount =
				resolveGridSpanCount(rv.context, width)
		}

	}
}