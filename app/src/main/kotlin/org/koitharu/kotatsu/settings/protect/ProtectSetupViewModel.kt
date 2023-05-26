package org.koitharu.kotatsu.settings.protect

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.SingleLiveEvent
import org.koitharu.kotatsu.core.util.asFlowLiveData
import org.koitharu.kotatsu.parsers.util.md5
import javax.inject.Inject

@HiltViewModel
class ProtectSetupViewModel @Inject constructor(
	private val settings: AppSettings,
) : BaseViewModel() {

	private val firstPassword = MutableStateFlow<String?>(null)

	val isSecondStep = firstPassword.map {
		it != null
	}.asFlowLiveData(viewModelScope.coroutineContext, false)
	val onPasswordSet = SingleLiveEvent<Unit>()
	val onPasswordMismatch = SingleLiveEvent<Unit>()
	val onClearText = SingleLiveEvent<Unit>()

	val isBiometricEnabled
		get() = settings.isBiometricProtectionEnabled

	fun onNextClick(password: String) {
		if (firstPassword.value == null) {
			firstPassword.value = password
			onClearText.call(Unit)
		} else {
			if (firstPassword.value == password) {
				settings.appPassword = password.md5()
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
