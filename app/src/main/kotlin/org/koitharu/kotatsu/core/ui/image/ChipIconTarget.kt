package org.koitharu.kotatsu.core.ui.image

import android.graphics.drawable.Drawable
import coil3.target.GenericViewTarget
import com.google.android.material.chip.Chip

class ChipIconTarget(override val view: Chip) : GenericViewTarget<Chip>() {

	override var drawable: Drawable?
		get() = view.chipIcon
		set(value) {
			view.chipIcon = value
		}
}
