package org.koitharu.kotatsu.core.util.ext

import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar

fun Toolbar.setNavigationIconSafe(@DrawableRes iconRes: Int, retry: Boolean = true) {
	try {
		setNavigationIcon(iconRes)
	} catch (e: IllegalStateException) {
		if (retry) {
			post { setNavigationIconSafe(iconRes, retry = false) }
		}
	}
}
