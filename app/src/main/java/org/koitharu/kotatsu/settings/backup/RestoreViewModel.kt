package org.koitharu.kotatsu.settings.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.backup.RestoreRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.File
import java.io.FileNotFoundException

class RestoreViewModel(
	uri: Uri?,
	private val repository: RestoreRepository,
	context: Context
) : BaseViewModel() {

	val progress = MutableLiveData<Progress?>(null)
	val onRestoreDone = SingleLiveEvent<CompositeResult>()

	init {
		launchLoadingJob {
			if (uri == null) {
				throw FileNotFoundException()
			}
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

				progress.value = Progress(0, 3)
				result += repository.upsertHistory(backup.getEntry(BackupEntry.HISTORY))

				progress.value = Progress(1, 3)
				result += repository.upsertCategories(backup.getEntry(BackupEntry.CATEGORIES))

				progress.value = Progress(2, 3)
				result += repository.upsertFavourites(backup.getEntry(BackupEntry.FAVOURITES))

				progress.value = Progress(3, 3)
				onRestoreDone.call(result)
			} finally {
				backup.close()
				backup.file.delete()
			}
		}
	}
}