package org.koitharu.kotatsu.utils.ext

import android.view.View
import org.koitharu.kotatsu.utils.anim.Duration
import org.koitharu.kotatsu.utils.anim.Motion

fun View.showAnimated(
	effect: Motion,
	duration: Duration = Duration.MEDIUM,
	onDone: (() -> Unit)? = null
) {
	if (this.visibility == View.VISIBLE) {
		onDone?.invoke()
		return
	}
	this.clearAnimation()
	effect.hideView(this)
	this.visibility = View.VISIBLE
	this.animate().also {
		it.duration = context.resources.getInteger(duration.resId).toLong()
		effect.show(this, it)
	}.withEndAction(onDone)
		.start()
}

fun View.hideAnimated(
	effect: Motion,
	duration: Duration = Duration.MEDIUM,
	onDone: (() -> Unit)? = null
) {
	if (this.visibility != View.VISIBLE) {
		onDone?.invoke()
		return
	}
	this.clearAnimation()
	this.animate().also {
		it.duration = context.resources.getInteger(duration.resId).toLong()
		effect.hide(this, it)
	}.withEndAction {
		this.visibility = View.GONE
		effect.reset(this)
		onDone?.invoke()
	}.start()
}

fun View.showOrHideAnimated(
	predicate: Boolean,
	effect: Motion,
	duration: Duration = Duration.MEDIUM,
	onDone: (() -> Unit)? = null
) {
	if (predicate) {
		showAnimated(effect, duration, onDone)
	} else {
		hideAnimated(effect, duration, onDone)
	}
}