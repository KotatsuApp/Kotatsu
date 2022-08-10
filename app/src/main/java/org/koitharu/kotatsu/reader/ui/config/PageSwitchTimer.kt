package org.koitharu.kotatsu.reader.ui.config

import android.content.res.Resources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.LabelFormatter
import kotlin.math.roundToLong
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.ui.ReaderControlDelegate

class PageSwitchTimer(
	private val listener: ReaderControlDelegate.OnInteractionListener,
	private val lifecycleOwner: LifecycleOwner,
) {

	var delaySec: Float = 0f
		set(value) {
			field = value
			delayMs = mapDelay(value)
			restartJob()
		}
	private var delayMs = 0L

	fun onUserInteraction() {
		restartJob()
	}

	private var job: Job? = null

	private fun restartJob() {
		job?.cancel()
		if (delayMs == 0L) {
			job = null
			return
		}
		job = lifecycleOwner.lifecycle.coroutineScope.launch {
			// FIXME: pause when bs is opened
			lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				while (isActive) {
					delay(delayMs)
					listener.switchPageBy(1)
				}
			}
		}
	}

	class DelayLabelFormatter(resources: Resources) : LabelFormatter {

		private val textOff = resources.getString(R.string.off_short)
		private val textSec = resources.getString(R.string.seconds_pattern)

		override fun getFormattedValue(value: Float): String {
			val ms = mapDelay(value)
			return if (ms == 0L) textOff else textSec.format((ms / 1000.0).format(1))
		}
	}

	companion object {

		private const val DELAY_MIN = 2000L

		fun mapDelay(value: Float): Long {
			val delay = (value * 1000L).roundToLong()
			return if (delay < DELAY_MIN) 0L else delay
		}
	}
}
