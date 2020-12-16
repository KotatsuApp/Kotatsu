package org.koitharu.kotatsu.utils

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.ext.measureWidth
import kotlin.math.abs
import kotlin.math.roundToInt

object UiUtils : KoinComponent {

	fun resolveGridSpanCount(context: Context, width: Int = 0): Int {
		val scaleFactor = get<AppSettings>().gridSize / 100f
		val cellWidth = context.resources.getDimension(R.dimen.preferred_grid_width) * scaleFactor
		val screenWidth = (if (width <= 0) {
			context.resources.displayMetrics.widthPixels
		} else width).toDouble()
		val estimatedCount = (screenWidth / cellWidth).roundToInt()
		return estimatedCount.coerceAtLeast(2)
	}

	fun isTablet(context: Context) = context.resources.getBoolean(R.bool.is_tablet)

	@Deprecated("Use MangaListSpanResolver")
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

		fun update(rv: RecyclerView) {
			val width = rv.measureWidth()
			if (width > 0) {
				(rv.layoutManager as? GridLayoutManager)?.spanCount =
					resolveGridSpanCount(rv.context, width)
			}
		}
	}
}