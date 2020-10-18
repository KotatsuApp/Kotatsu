package org.koitharu.kotatsu.ui.utils.protect

import kotlinx.coroutines.delay
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.utils.ext.md5

class ProtectPresenter : BasePresenter<ProtectView>() {

	private val settings by inject<AppSettings>()

	fun tryUnlock(password: String) {
		launchLoadingJob {
			val passwordHash = password.md5()
			val appPasswordHash = settings.appPassword
			if (passwordHash == appPasswordHash) {
				viewState.onUnlockSuccess()
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