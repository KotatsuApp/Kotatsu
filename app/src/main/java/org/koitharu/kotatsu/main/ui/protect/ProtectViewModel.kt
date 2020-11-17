package org.koitharu.kotatsu.main.ui.protect

import kotlinx.coroutines.delay
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.md5

class ProtectViewModel(
	private val settings: AppSettings
) : BaseViewModel() {

	val onUnlockSuccess = SingleLiveEvent<Unit>()

	fun tryUnlock(password: String) {
		launchLoadingJob {
			val passwordHash = password.md5()
			val appPasswordHash = settings.appPassword
			if (passwordHash == appPasswordHash) {
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