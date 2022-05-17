package org.koitharu.kotatsu.search.ui.multi.adapter

import android.content.res.Resources
import kotlin.math.roundToInt
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings

class ItemSizeResolver(resources: Resources, settings: AppSettings) {

	private val scaleFactor = settings.gridSize / 100f
	private val gridWidth = resources.getDimension(R.dimen.preferred_grid_width)

	val cellWidth: Int
		get() = (gridWidth * scaleFactor).roundToInt()
}