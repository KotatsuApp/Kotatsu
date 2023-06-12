package org.koitharu.kotatsu.core.util

import android.os.SystemClock

class Throttler(
	private val timeoutMs: Long,
) {

	private var lastTick = 0L

	fun throttle(): Boolean {
		val now = SystemClock.elapsedRealtime()
		return if (lastTick + timeoutMs <= now) {
			lastTick = now
			true
		} else {
			false
		}
	}

	fun reset() {
		lastTick = 0L
	}
}
