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
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

@HiltViewModel
class RestoreViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val progress = MutableStateFlow(-1f)
	val onRestoreDone = MutableEventFlow<CompositeResult>()

	init {
		launchLoadingJob {
			val uri = savedStateHandle.get<String>(RestoreDialogFragment.ARG_FILE)
				?.toUriOrNull() ?: throw FileNotFoundException()
			val contentResolver = context.contentResolver

			val backup = runInterruptible(Dispatchers.IO) {
				val tempFile = File.createTempFile("backup_", ".tmp")
				(contentResolver.openInputStream(uri) ?: throw FileNotFoundException()).use { input ->
					tempFile.outputStream().use { output ->
						input.copyTo(output)
					}
				}
				BackupZipInput(tempFile)
			}
			try {
				val result = CompositeResult()

				progress.value = 0f
				backup.getEntry(BackupEntry.HISTORY)?.let {
					result += repository.restoreHistory(it)
				}

				progress.value = 0.2f
				backup.getEntry(BackupEntry.CATEGORIES)?.let {
					result += repository.restoreCategories(it)
				}

				progress.value = 0.4f
				backup.getEntry(BackupEntry.FAVOURITES)?.let {
					result += repository.restoreFavourites(it)
				}

				progress.value = 0.6f
				backup.getEntry(BackupEntry.BOOKMARKS)?.let {
					result += repository.restoreBookmarks(it)
				}

				progress.value = 0.8f
				backup.getEntry(BackupEntry.SETTINGS)?.let {
					result += repository.restoreSettings(it)
				}

				progress.value = 1f
				onRestoreDone.call(result)
			} finally {
				backup.close()
				backup.file.delete()
			}
		}
	}
}
