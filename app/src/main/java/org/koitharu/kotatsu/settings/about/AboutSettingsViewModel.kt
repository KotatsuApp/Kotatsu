package org.koitharu.kotatsu.settings.about

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.utils.SingleLiveEvent

@HiltViewModel
class AboutSettingsViewModel @Inject constructor(
	private val appUpdateRepository: AppUpdateRepository,
) : BaseViewModel() {

	val isUpdateSupported = appUpdateRepository.isUpdateSupported()
	val onUpdateAvailable = SingleLiveEvent<AppVersion?>()

	fun checkForUpdates() {
		launchLoadingJob {
			val update = appUpdateRepository.fetchUpdate()
			onUpdateAvailable.call(update)
		}
	}
}
