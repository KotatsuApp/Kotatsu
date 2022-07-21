package org.koitharu.kotatsu.settings.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.settings.tools.model.StorageUsage

class ToolsViewModel(
	private val storageManager: LocalStorageManager,
	private val appUpdateRepository: AppUpdateRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val appUpdate = appUpdateRepository.observeAvailableUpdate()
		.asLiveData(viewModelScope.coroutineContext)

	val storageUsage: LiveData<StorageUsage> = liveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
	) {
		emit(collectStorageUsage())
	}

	val isIncognitoModeEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
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
