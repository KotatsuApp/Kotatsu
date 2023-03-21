package org.koitharu.kotatsu.reader.ui

import androidx.annotation.FloatRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val MIN_SPEED = 0.1
private const val MAX_DELAY = 80L
private const val MAX_SWITCH_DELAY = 20_000L

class ScrollTimer(
	private val lifecycleOwner: LifecycleOwner,
	private val listener: ReaderControlDelegate.OnInteractionListener,
) {

	private var job: Job? = null
	private var delayMs: Long = 10L
	private var pageSwitchDelay: Long = 100L

	@FloatRange(from = 0.0, to = 1.0)
	var speed: Float = 0f
		set(value) {
			if (field != value) {
				field = value
				onSpeedChanged()
			}
		}

	private fun onSpeedChanged() {
		if (speed < MIN_SPEED) {
			delayMs = 0L
			pageSwitchDelay = 0L
		} else {
			val speedFactor = 1 - speed + MIN_SPEED
			delayMs = (MAX_DELAY * speedFactor).roundToLong()
			pageSwitchDelay = (MAX_SWITCH_DELAY * speedFactor).roundToLong()
		}
		if ((job == null) != (delayMs == 0L)) {
			restartJob()
		}
	}

	private fun restartJob() {
		job?.cancel()
		if (delayMs == 0L) {
			job = null
			return
		}
		job = lifecycleOwner.lifecycle.coroutineScope.launch {
			lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				var accumulator = 0L
				while (isActive) {
					delay(delayMs)
					if (!listener.scrollBy(1)) {
						accumulator += delayMs
					}
					if (accumulator >= pageSwitchDelay) {
						listener.switchPageBy(1)
						accumulator -= pageSwitchDelay
					}
				}
			}
		}
	}
}
