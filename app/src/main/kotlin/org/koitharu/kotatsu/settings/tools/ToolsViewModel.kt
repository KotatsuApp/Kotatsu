package org.koitharu.kotatsu.settings.tools

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.settings.tools.model.StorageUsage
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
	appUpdateRepository: AppUpdateRepository,
) : BaseViewModel() {

	val appUpdate = appUpdateRepository.observeAvailableUpdate()

	val storageUsage: StateFlow<StorageUsage?> = flow {
		emit(collectStorageUsage())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val isIncognitoModeEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	fun toggleIncognitoMode(isEnabled: Boolean) {
		settings.isIncognitoModeEnabled = isEnabled
	}

	private suspend fun collectStorageUsage(): StorageUsage {
		val pagesCacheSize = storageManager.computeCacheSize(CacheDir.PAGES)
		val otherCacheSize = storageManager.computeCacheSize() - pagesCacheSize
		val storageSize = storageManager.computeStorageSize()
		val availableSpace = storageManager.computeAvailableSize()
		val totalBytes = pagesCacheSize + otherCacheSize + storageSize + availableSpace
		return StorageUsage(
			savedManga = StorageUsage.Item(
				bytes = storageSize,
				percent = (storageSize.toDouble() / totalBytes).toFloat(),
			),
			pagesCache = StorageUsage.Item(
				bytes = pagesCacheSize,
				percent = (pagesCacheSize.toDouble() / totalBytes).toFloat(),
			),
			otherCache = StorageUsage.Item(
				bytes = otherCacheSize,
				percent = (otherCacheSize.toDouble() / totalBytes).toFloat(),
			),
			available = StorageUsage.Item(
				bytes = availableSpace,
				percent = (availableSpace.toDouble() / totalBytes).toFloat(),
			),
		)
	}
}
