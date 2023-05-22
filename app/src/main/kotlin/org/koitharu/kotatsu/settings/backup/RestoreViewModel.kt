package org.koitharu.kotatsu.settings.backup

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.SingleLiveEvent
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

	val progress = MutableLiveData(-1f)
	val onRestoreDone = SingleLiveEvent<CompositeResult>()

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
				result += repository.restoreHistory(backup.getEntry(BackupEntry.HISTORY))

				progress.value = 0.3f
				result += repository.restoreCategories(backup.getEntry(BackupEntry.CATEGORIES))

				progress.value = 0.6f
				result += repository.restoreFavourites(backup.getEntry(BackupEntry.FAVOURITES))

				progress.value = 1f
				onRestoreDone.call(result)
			} finally {
				backup.close()
				backup.file.delete()
			}
		}
	}
}
