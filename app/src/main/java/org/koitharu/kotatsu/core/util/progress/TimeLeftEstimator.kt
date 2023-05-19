package org.koitharu.kotatsu.core.util.progress

import android.os.SystemClock
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MIN_ESTIMATE_TICKS = 4
private const val NO_TIME = -1L

class TimeLeftEstimator {

	private var times = ArrayList<Int>()
	private var lastTick: Tick? = null
	private val tooLargeTime = TimeUnit.DAYS.toMillis(1)

	fun tick(value: Int, total: Int) {
		if (total < 0) {
			emptyTick()
			return
		}
		if (lastTick?.value == value) {
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
		val eta = (ticksLeft * timePerTick).roundToLong()
		return if (eta < tooLargeTime) eta else NO_TIME
	}

	fun getEta(): Long {
		val etl = getEstimatedTimeLeft()
		return if (etl == NO_TIME) NO_TIME else System.currentTimeMillis() + etl
	}

	private class Tick(
		@JvmField val value: Int,
		@JvmField val total: Int,
		@JvmField val time: Long,
	)
}
