package org.koitharu.kotatsu.download.ui.worker

import android.os.SystemClock

class Throttler(
	private val timeoutMs: Long,
) {

	private var lastTick = 0L

	fun throttle(): Boolean {
		val prevValue = lastTick
		lastTick = SystemClock.elapsedRealtime()
		return lastTick > prevValue + timeoutMs
	}

	fun reset() {
		lastTick = 0L
	}
}
