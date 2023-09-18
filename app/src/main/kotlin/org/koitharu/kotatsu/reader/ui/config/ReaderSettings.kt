package org.koitharu.kotatsu.reader.ui.config

import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter

class ReaderSettings(
	private val parentScope: CoroutineScope,
	private val settings: AppSettings,
	private val colorFilterFlow: StateFlow<ReaderColorFilter?>,
) : MediatorLiveData<ReaderSettings>() {

	private val internalObserver = InternalObserver()
	private var collectJob: Job? = null

	val zoomMode: ZoomMode
		get() = settings.zoomMode

	val colorFilter: ReaderColorFilter?
		get() = colorFilterFlow.value?.takeUnless { it.isEmpty }

	val isPagesNumbersEnabled: Boolean
		get() = settings.isPagesNumbersEnabled

	val isZoomControlsEnabled: Boolean
		get() = settings.isReaderZoomButtonsEnabled

	fun applyBackground(view: View) {
		val bg = settings.readerBackground
		view.background = bg.resolve(view.context)
	}

	override fun onInactive() {
		super.onInactive()
		settings.unsubscribe(internalObserver)
		collectJob?.cancel()
		collectJob = null
	}

	override fun onActive() {
		super.onActive()
		settings.subscribe(internalObserver)
		collectJob?.cancel()
		collectJob = parentScope.launch {
			colorFilterFlow.collect(internalObserver)
		}
	}

	override fun getValue() = this

	private fun notifyChanged() {
		value = value
	}

	private inner class InternalObserver :
		FlowCollector<ReaderColorFilter?>,
		SharedPreferences.OnSharedPreferenceChangeListener {

		override suspend fun emit(value: ReaderColorFilter?) {
			withContext(Dispatchers.Main.immediate) {
				notifyChanged()
			}
		}

		override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
			if (
				key == AppSettings.KEY_ZOOM_MODE ||
				key == AppSettings.KEY_PAGES_NUMBERS ||
				key == AppSettings.KEY_WEBTOON_ZOOM ||
				key == AppSettings.KEY_READER_ZOOM_BUTTONS ||
				key == AppSettings.KEY_READER_BACKGROUND
			) {
				notifyChanged()
			}
		}
	}
}
