package org.koitharu.kotatsu.settings.backup

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import java.io.File
import java.io.FileNotFoundException
import java.util.Date
import java.util.EnumMap
import java.util.EnumSet
import javax.inject.Inject

@HiltViewModel
class RestoreViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val uri = savedStateHandle.get<String>(AppRouter.KEY_FILE)?.toUriOrNull()
	private val contentResolver = context.contentResolver

	val availableEntries = MutableStateFlow<List<BackupEntryModel>>(emptyList())
	val backupDate = MutableStateFlow<Date?>(null)

	init {
		launchLoadingJob(Dispatchers.Default) {
			loadBackupInfo()
		}
	}

	private suspend fun loadBackupInfo() {
		runInterruptible(Dispatchers.IO) {
			val tempFile = File.createTempFile("backup_", ".tmp")
			(uri?.let { contentResolver.openInputStream(it) } ?: throw FileNotFoundException()).use { input ->
				tempFile.outputStream().use { output ->
					input.copyTo(output)
				}
			}
			BackupZipInput.from(tempFile)
		}.use { backup ->
			val entries = backup.entries()
			availableEntries.value = BackupEntry.Name.entries.mapNotNull { entry ->
				if (entry == BackupEntry.Name.INDEX || entry !in entries) {
					return@mapNotNull null
				}
				BackupEntryModel(
					name = entry,
					isChecked = true,
					isEnabled = true,
				)
			}
			backupDate.value = repository.getBackupDate(backup.getEntry(BackupEntry.Name.INDEX))
		}
	}

	fun onItemClick(item: BackupEntryModel) {
		val map = availableEntries.value.associateByTo(EnumMap(BackupEntry.Name::class.java)) { it.name }
		map[item.name] = item.copy(isChecked = !item.isChecked)
		map.validate()
		availableEntries.value = map.values.sortedBy { it.name.ordinal }
	}

	fun getCheckedEntries(): Set<BackupEntry.Name> = availableEntries.value
		.mapNotNullTo(EnumSet.noneOf(BackupEntry.Name::class.java)) {
			if (it.isChecked) it.name else null
		}

	/**
	 * Check for inconsistent user selection
	 * Favorites cannot be restored without categories
	 */
	private fun MutableMap<BackupEntry.Name, BackupEntryModel>.validate() {
		val favorites = this[BackupEntry.Name.FAVOURITES] ?: return
		val categories = this[BackupEntry.Name.CATEGORIES]
		if (categories?.isChecked == true) {
			if (!favorites.isEnabled) {
				this[BackupEntry.Name.FAVOURITES] = favorites.copy(isEnabled = true)
			}
		} else {
			if (favorites.isEnabled) {
				this[BackupEntry.Name.FAVOURITES] = favorites.copy(isEnabled = false, isChecked = false)
			}
		}
	}
}
