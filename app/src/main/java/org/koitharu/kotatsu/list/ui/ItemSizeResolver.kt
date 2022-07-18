package org.koitharu.kotatsu.list.ui

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import kotlin.math.roundToInt

class ItemSizeResolver(resources: Resources, settings: AppSettings) {

	private val scaleFactor = settings.gridSize / 100f
	private val gridWidth = resources.getDimension(R.dimen.preferred_grid_width)

	val cellWidth: Int
		get() = (gridWidth * scaleFactor).roundToInt()
}