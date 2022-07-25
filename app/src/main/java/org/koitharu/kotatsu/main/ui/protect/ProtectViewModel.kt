package org.koitharu.kotatsu.main.ui.protect

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.util.md5
import org.koitharu.kotatsu.utils.SingleLiveEvent

private const val PASSWORD_COMPARE_DELAY = 1_000L

@HiltViewModel
class ProtectViewModel @Inject constructor(
	private val settings: AppSettings,
	private val protectHelper: AppProtectHelper,
) : BaseViewModel() {

	private var job: Job? = null

	val onUnlockSuccess = SingleLiveEvent<Unit>()

	val isBiometricEnabled
		get() = settings.isBiometricProtectionEnabled

	fun tryUnlock(password: String) {
		if (job?.isActive == true) {
			return
		}
		job = launchLoadingJob {
			val passwordHash = password.md5()
			val appPasswordHash = settings.appPassword
			if (passwordHash == appPasswordHash) {
				unlock()
			} else {
				delay(PASSWORD_COMPARE_DELAY)
				throw WrongPasswordException()
			}
		}
	}

	fun unlock() {
		protectHelper.unlock()
		onUnlockSuccess.call(Unit)
	}
}
