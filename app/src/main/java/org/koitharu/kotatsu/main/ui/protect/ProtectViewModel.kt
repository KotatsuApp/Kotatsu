package org.koitharu.kotatsu.main.ui.protect

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.md5

class ProtectViewModel(
	private val settings: AppSettings,
	private val protectHelper: AppProtectHelper,
) : BaseViewModel() {

	private var job: Job? = null

	val onUnlockSuccess = SingleLiveEvent<Unit>()

	fun tryUnlock(password: String) {
		if (job?.isActive == true) {
			return
		}
		job = launchLoadingJob {
			val passwordHash = password.md5()
			val appPasswordHash = settings.appPassword
			if (passwordHash == appPasswordHash) {
				protectHelper.unlock()
				onUnlockSuccess.call(Unit)
			} else {
				delay(PASSWORD_COMPARE_DELAY)
				throw WrongPasswordException()
			}
		}
	}

	private companion object {

		const val PASSWORD_COMPARE_DELAY = 1_000L
	}
}