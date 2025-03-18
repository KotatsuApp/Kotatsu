package org.koitharu.kotatsu.settings.userdata

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.local.data.LocalStorageManager
import javax.inject.Inject

@HiltViewModel
class UserDataSettingsViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
) : BaseViewModel() {

	val storageUsage = MutableStateFlow(-1L)

	val periodicalBackupFrequency = settings.observeAsFlow(
		key = AppSettings.KEY_BACKUP_PERIODICAL_ENABLED,
		valueProducer = { isPeriodicalBackupEnabled },
	).flatMapLatest { isEnabled ->
		if (isEnabled) {
			settings.observeAsFlow(
				key = AppSettings.KEY_BACKUP_PERIODICAL_FREQUENCY,
				valueProducer = { periodicalBackupFrequency },
			)
		} else {
			flowOf(0)
		}
	}

	private var storageUsageJob: Job? = null

	init {
		loadStorageUsage()
	}

	private fun loadStorageUsage(): Job {
		val prevJob = storageUsageJob
		return launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val totalBytes = storageManager.computeCacheSize() + storageManager.computeStorageSize()
			storageUsage.value = totalBytes
		}.also {
			storageUsageJob = it
		}
	}
}
