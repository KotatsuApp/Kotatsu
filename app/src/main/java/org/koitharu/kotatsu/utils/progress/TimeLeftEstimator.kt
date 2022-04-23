package org.koitharu.kotatsu.utils.progress

import android.os.SystemClock
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MIN_ESTIMATE_TICKS = 4
private const val NO_TIME = -1L

class TimeLeftEstimator {

	private var times = ArrayList<Int>()
	private var lastTick: Tick? = null

	fun tick(value: Int, total: Int) {
		if (total < 0) {
			emptyTick()
			return
		}
		val tick = Tick(value, total, SystemClock.elapsedRealtime())
		lastTick?.let {
			val ticksCount = value - it.value
			times.add(((tick.time - it.time) / ticksCount.toDouble()).roundToInt())
		}
		lastTick = tick
	}

	fun emptyTick() {
		lastTick = null
	}

	fun getEstimatedTimeLeft(): Long {
		val progress = lastTick ?: return NO_TIME
		if (times.size < MIN_ESTIMATE_TICKS) {
			return NO_TIME
		}
		val timePerTick = times.average()
		val ticksLeft = progress.total - progress.value
		return (ticksLeft * timePerTick).roundToLong()
	}

	private class Tick(
		val value: Int,
		val total: Int,
		val time: Long,
	)
}