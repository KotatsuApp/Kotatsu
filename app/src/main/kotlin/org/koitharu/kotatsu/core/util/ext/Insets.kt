package org.koitharu.kotatsu.core.util.ext

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

fun Insets.end(view: View): Int {
	return if (view.isRtl) left else right
}

fun Insets.start(view: View): Int {
	return if (view.isRtl) right else left
}

@Deprecated("")
val WindowInsetsCompat.systemBarsInsets: Insets
	get() = getInsets(WindowInsetsCompat.Type.systemBars())

@Deprecated("")
fun WindowInsetsCompat.consumeSystemBarsInsets(
	left: Boolean = false,
	top: Boolean = false,
	right: Boolean = false,
	bottom: Boolean = false,
): WindowInsetsCompat {
	val barsInsets = systemBarsInsets
	val insets = Insets.of(
		if (left) 0 else barsInsets.left,
		if (top) 0 else barsInsets.top,
		if (right) 0 else barsInsets.right,
		if (bottom) 0 else barsInsets.bottom,
	)
	return WindowInsetsCompat.Builder(this)
		.setInsets(WindowInsetsCompat.Type.systemBars(), insets)
		.build()
}

fun WindowInsetsCompat.consume(
	v: View,
	@InsetsType typeMask: Int,
	start: Boolean = false,
	top: Boolean = false,
	end: Boolean = false,
	bottom: Boolean = false,
): WindowInsetsCompat {
	val insets = getInsets(typeMask)
	val newInsets = Insets.of(
		/* left = */ if (if (v.isRtl) end else start) 0 else insets.left,
		/* top = */ if (top) 0 else insets.top,
		/* right = */ if (if (v.isRtl) start else end) 0 else insets.right,
		/* bottom = */ if (bottom) 0 else insets.bottom,
	)
	return WindowInsetsCompat.Builder(this)
		.setInsets(typeMask, newInsets)
		.build()
}

fun WindowInsetsCompat.consumeAll(
	@InsetsType typeMask: Int,
): WindowInsetsCompat = WindowInsetsCompat.Builder(this)
	.setInsets(typeMask, Insets.NONE)
	.build()

@Deprecated("")
fun WindowInsetsCompat.consumeSystemBarsInsets(
	view: View,
	start: Boolean = false,
	top: Boolean = false,
	end: Boolean = false,
	bottom: Boolean = false,
): WindowInsetsCompat = consume(view, WindowInsetsCompat.Type.systemBars(), start, top, end, bottom)

@Deprecated("")
fun WindowInsetsCompat.consumeAllSystemBarsInsets() = consumeAll(WindowInsetsCompat.Type.systemBars())

@Deprecated("")
fun Insets.consume(
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
