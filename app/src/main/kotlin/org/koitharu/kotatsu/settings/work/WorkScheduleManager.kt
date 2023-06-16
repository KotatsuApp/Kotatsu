package org.koitharu.kotatsu.settings.work

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.suggestions.ui.SuggestionsWorker
import org.koitharu.kotatsu.tracker.work.TrackWorker
import javax.inject.Inject

class WorkScheduleManager @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) : SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_TRACKER_ENABLED -> updateWorker(TrackWorker, settings.isTrackerEnabled)
			AppSettings.KEY_SUGGESTIONS -> updateWorker(SuggestionsWorker, settings.isSuggestionsEnabled)
		}
	}

	fun init() {
		settings.subscribe(this)
		processLifecycleScope.launch(Dispatchers.Default) {
			updateWorkerImpl(TrackWorker, settings.isTrackerEnabled)
			updateWorkerImpl(SuggestionsWorker, settings.isSuggestionsEnabled)
		}
	}

	private fun updateWorker(scheduler: PeriodicWorkScheduler, isEnabled: Boolean) {
		processLifecycleScope.launch(Dispatchers.Default) {
			updateWorkerImpl(scheduler, isEnabled)
		}
	}

	private suspend fun updateWorkerImpl(scheduler: PeriodicWorkScheduler, isEnabled: Boolean) {
		if (scheduler.isScheduled(context) != isEnabled) {
			if (isEnabled) {
				scheduler.schedule(context)
			} else {
				scheduler.unschedule(context)
			}
		}
	}
}
