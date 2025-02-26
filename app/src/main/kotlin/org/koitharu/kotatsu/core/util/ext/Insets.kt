package org.koitharu.kotatsu.core.util.ext

import android.view.View
import androidx.core.graphics.Insets

fun Insets.end(view: View): Int {
	return if (view.isRtl) left else right
}

fun Insets.start(view: View): Int {
	return if (view.isRtl) right else left
}

fun Insets.consume(
	left: Boolean = false,
	top: Boolean = false,
	right: Boolean = false,
	bottom: Boolean = false,
): Insets = Insets.of(
	/* left = */ if (left) 0 else this.left,
	/* top = */ if (top) 0 else this.top,
	/* right = */ if (right) 0 else this.right,
	/* bottom = */ if (bottom) 0 else this.bottom,
)


fun Insets.consumeRelative(
	view: View,
	start: Boolean = false,
	top: Boolean = false,
	end: Boolean = false,
	bottom: Boolean = false,
): Insets = Insets.of(
	/* left = */ if (if (view.isRtl) end else start) 0 else this.left,
	/* top = */ if (top) 0 else this.top,
	/* right = */ if (if (view.isRtl) start else end) 0 else this.right,
	/* bottom = */ if (bottom) 0 else this.bottom,
)
