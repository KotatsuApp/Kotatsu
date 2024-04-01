package org.koitharu.kotatsu.settings.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.exceptions.BadBackupFormatException
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.parsers.util.SuspendLazy
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

	private val backupInput = SuspendLazy {
		val uri = savedStateHandle.get<String>(RestoreDialogFragment.ARG_FILE)
			?.toUriOrNull() ?: throw FileNotFoundException()
		val contentResolver = context.contentResolver
		runInterruptible(Dispatchers.IO) {
			val tempFile = File.createTempFile("backup_", ".tmp")
			(contentResolver.openInputStream(uri) ?: throw FileNotFoundException()).use { input ->
				tempFile.outputStream().use { output ->
					input.copyTo(output)
				}
			}
			BackupZipInput.from(tempFile)
		}
	}

	val progress = MutableStateFlow(-1f)
	val onRestoreDone = MutableEventFlow<CompositeResult>()

	val availableEntries = MutableStateFlow<List<BackupEntryModel>>(emptyList())
	val backupDate = MutableStateFlow<Date?>(null)

	init {
		launchLoadingJob(Dispatchers.Default) {
			val backup = backupInput.get()
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

	override fun onCleared() {
		super.onCleared()
		backupInput.peek()?.cleanupAsync()
	}

	fun onItemClick(item: BackupEntryModel) {
		val map = availableEntries.value.associateByTo(EnumMap(BackupEntry.Name::class.java)) { it.name }
		map[item.name] = item.copy(isChecked = !item.isChecked)
		map.validate()
		availableEntries.value = map.values.sortedBy { it.name.ordinal }
	}

	fun restore() {
		launchLoadingJob {
			val backup = backupInput.get()
			val checkedItems = availableEntries.value.mapNotNullTo(EnumSet.noneOf(BackupEntry.Name::class.java)) {
				if (it.isChecked) it.name else null
			}
			val result = CompositeResult()
			val step = 1f / 6f

			progress.value = 0f
			if (BackupEntry.Name.HISTORY in checkedItems) {
				backup.getEntry(BackupEntry.Name.HISTORY)?.let {
					result += repository.restoreHistory(it)
				}
			}

			progress.value += step
			if (BackupEntry.Name.CATEGORIES in checkedItems) {
				backup.getEntry(BackupEntry.Name.CATEGORIES)?.let {
					result += repository.restoreCategories(it)
				}
			}

			progress.value += step
			if (BackupEntry.Name.FAVOURITES in checkedItems) {
				backup.getEntry(BackupEntry.Name.FAVOURITES)?.let {
					result += repository.restoreFavourites(it)
				}
			}

			progress.value += step
			if (BackupEntry.Name.BOOKMARKS in checkedItems) {
				backup.getEntry(BackupEntry.Name.BOOKMARKS)?.let {
					result += repository.restoreBookmarks(it)
				}
			}

			progress.value += step
			if (BackupEntry.Name.SOURCES in checkedItems) {
				backup.getEntry(BackupEntry.Name.SOURCES)?.let {
					result += repository.restoreSources(it)
				}
			}

			progress.value += step
			if (BackupEntry.Name.SETTINGS in checkedItems) {
				backup.getEntry(BackupEntry.Name.SETTINGS)?.let {
					result += repository.restoreSettings(it)
				}
			}

			progress.value = 1f
			onRestoreDone.call(result)
		}
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
