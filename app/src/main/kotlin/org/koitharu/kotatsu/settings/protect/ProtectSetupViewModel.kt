package org.koitharu.kotatsu.settings.protect

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.parsers.util.isNumeric
import org.koitharu.kotatsu.parsers.util.md5
import javax.inject.Inject

@HiltViewModel
class ProtectSetupViewModel @Inject constructor(
	private val settings: AppSettings,
) : BaseViewModel() {

	private val firstPassword = MutableStateFlow<String?>(null)

	val isSecondStep = firstPassword.map {
		it != null
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)
	val onPasswordSet = MutableEventFlow<Unit>()
	val onPasswordMismatch = MutableEventFlow<Unit>()
	val onClearText = MutableEventFlow<Unit>()

	val isBiometricEnabled
		get() = settings.isBiometricProtectionEnabled

	fun onNextClick(password: String) {
		if (firstPassword.value == null) {
			firstPassword.value = password
			onClearText.call(Unit)
		} else {
			if (firstPassword.value == password) {
				settings.appPassword = password.md5()
				settings.isAppPasswordNumeric = password.isNumeric()
				onPasswordSet.call(Unit)
			} else {
				onPasswordMismatch.call(Unit)
			}
		}
	}

	fun setBiometricEnabled(isEnabled: Boolean) {
		settings.isBiometricProtectionEnabled = isEnabled
	}
}
