package org.koitharu.kotatsu.reader.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import kotlin.math.roundToLong

private const val MAX_DELAY = 20L
private const val MAX_SWITCH_DELAY = 10_000L
private const val INTERACTION_SKIP_MS = 1_000L

class ScrollTimer @AssistedInject constructor(
	@Assisted private val listener: ReaderControlDelegate.OnInteractionListener,
	@Assisted lifecycleOwner: LifecycleOwner,
	settings: AppSettings,
) {

	private val coroutineScope = lifecycleOwner.lifecycleScope
	private var job: Job? = null
	private var delayMs: Long = 10L
	private var pageSwitchDelay: Long = 100L
	private var skip = 0L

	var isEnabled: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				restartJob()
			}
		}

	init {
		settings.observeAsFlow(AppSettings.KEY_READER_AUTOSCROLL_SPEED) {
			readerAutoscrollSpeed
		}.flowOn(Dispatchers.Default)
			.onEach {
				onSpeedChanged(it)
			}.launchIn(coroutineScope)
	}

	fun onUserInteraction() {
		skip = INTERACTION_SKIP_MS
	}

	private fun onSpeedChanged(speed: Float) {
		if (speed <= 0f) {
			delayMs = 0L
			pageSwitchDelay = 0L
		} else {
			val speedFactor = 1 - speed
			delayMs = (MAX_DELAY * speedFactor).roundToLong()
			pageSwitchDelay = (MAX_SWITCH_DELAY * speedFactor).roundToLong()
		}
		if ((job == null) != (delayMs == 0L)) {
			restartJob()
		}
	}

	private fun restartJob() {
		job?.cancel()
		skip = 0
		if (!isEnabled || delayMs == 0L) {
			job = null
			return
		}
		job = coroutineScope.launch {
			var accumulator = 0L
			while (isActive) {
				delay(delayMs)
				if (!listener.isReaderResumed()) {
					continue
				}
				skip -= delayMs
				if (skip > 0) {
					continue
				}
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

	@AssistedFactory
	interface Factory {

		fun create(
			lifecycleOwner: LifecycleOwner,
			listener: ReaderControlDelegate.OnInteractionListener,
		): ScrollTimer
	}
}
