package org.koitharu.kotatsu.reader.ui

import android.content.res.Resources
import android.os.SystemClock
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.util.ext.resolveDp
import kotlin.math.roundToLong

private const val MAX_DELAY = 32L
private const val MAX_SWITCH_DELAY = 10_000L
private const val INTERACTION_SKIP_MS = 2_000L
private const val SPEED_FACTOR_DELTA = 0.02f

class ScrollTimer @AssistedInject constructor(
	@Assisted resources: Resources,
	@Assisted private val listener: ReaderControlDelegate.OnInteractionListener,
	@Assisted lifecycleOwner: LifecycleOwner,
	settings: AppSettings,
) {

	private val coroutineScope = lifecycleOwner.lifecycleScope
	private var job: Job? = null
	private var delayMs: Long = 10L
	var pageSwitchDelay: Long = 100L
		private set
	private var resumeAt = 0L
	private var isTouchDown = MutableStateFlow(false)
	private val isRunning = MutableStateFlow(false)
	private val scrollDelta = resources.resolveDp(1)

	val isActive: StateFlow<Boolean>
		get() = isRunning

	init {
		settings.observeAsFlow(AppSettings.KEY_READER_AUTOSCROLL_SPEED) {
			readerAutoscrollSpeed
		}.flowOn(Dispatchers.Default)
			.onEach {
				onSpeedChanged(it)
			}.launchIn(coroutineScope)
	}

	fun setActive(value: Boolean) {
		if (isRunning.value != value) {
			isRunning.value = value
			restartJob()
		}
	}

	fun onUserInteraction() {
		resumeAt = SystemClock.elapsedRealtime() + INTERACTION_SKIP_MS
	}

	fun onTouchEvent(event: MotionEvent) {
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				isTouchDown.value = true
			}

			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_CANCEL -> {
				isTouchDown.value = false
			}
		}
	}

	private fun onSpeedChanged(speed: Float) {
		if (speed <= 0f) {
			delayMs = 0L
			pageSwitchDelay = 0L
		} else {
			val speedFactor = 1f - speed
			delayMs = (MAX_DELAY * speedFactor).roundToLong()
			pageSwitchDelay = (MAX_SWITCH_DELAY * speedFactor).roundToLong()
		}
		if ((job == null) != (delayMs == 0L)) {
			restartJob()
		}
	}

	private fun restartJob() {
		job?.cancel()
		resumeAt = 0L
		if (!isRunning.value || delayMs == 0L) {
			job = null
			return
		}
		job = coroutineScope.launch {
			var accumulator = 0L
			var speedFactor = 1f
			while (isActive) {
				if (isPaused()) {
					speedFactor = (speedFactor - SPEED_FACTOR_DELTA).coerceAtLeast(0f)
				} else if (speedFactor < 1f) {
					speedFactor = (speedFactor + SPEED_FACTOR_DELTA).coerceAtMost(1f)
				}
				if (speedFactor == 1f) {
					delay(delayMs)
				} else if (speedFactor == 0f) {
					delayUntilResumed()
					continue
				} else {
					delay((delayMs * (1f + speedFactor * 2)).toLong())
				}
				if (!listener.isReaderResumed()) {
					continue
				}
				if (!listener.scrollBy(scrollDelta, false)) {
					accumulator += delayMs
				}
				if (accumulator >= pageSwitchDelay) {
					listener.switchPageBy(1)
					accumulator -= pageSwitchDelay
				}
			}
		}
	}

	private fun isPaused(): Boolean {
		return isTouchDown.value || resumeAt > SystemClock.elapsedRealtime()
	}

	private suspend fun delayUntilResumed() {
		while (isPaused()) {
			val delayTime = resumeAt - SystemClock.elapsedRealtime()
			if (delayTime > 0) {
				delay(delayTime)
			} else {
				yield()
			}
			isTouchDown.first { !it }
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(
			resources: Resources,
			lifecycleOwner: LifecycleOwner,
			listener: ReaderControlDelegate.OnInteractionListener,
		): ScrollTimer
	}
}
