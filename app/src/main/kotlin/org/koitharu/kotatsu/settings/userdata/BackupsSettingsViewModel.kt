package org.koitharu.kotatsu.settings.userdata

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class BackupsSettingsViewModel @Inject constructor(
    private val settings: AppSettings,
) : BaseViewModel() {

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
}
