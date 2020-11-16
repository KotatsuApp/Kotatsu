package org.koitharu.kotatsu.ui.settings.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.core.component.get
import org.koitharu.kotatsu.core.backup.BackupArchive
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.backup.RestoreRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.File
import java.io.FileNotFoundException

class RestorePresenter(
	private val uri: Uri?,
	private val repository: RestoreRepository
) : BasePresenter<RestoreView>() {

	override fun onFirstViewAttach() {
		super.onFirstViewAttach()
		launchLoadingJob {
			viewState.onProgressChanged(null)
			if (uri == null) {
				throw FileNotFoundException()
			}
			val contentResolver = get<Context>().contentResolver

			@Suppress("BlockingMethodInNonBlockingContext")
			val backup = withContext(Dispatchers.IO) {
				val tempFile = File.createTempFile("backup_", ".tmp")
				(contentResolver.openInputStream(uri)
					?: throw FileNotFoundException()).use { input ->
					tempFile.outputStream().use { output ->
						input.copyTo(output)
					}
				}
				BackupArchive(tempFile)
			}
			try {
				backup.unpack()
				val result = CompositeResult()

				viewState.onProgressChanged(Progress(0, 3))
				result += repository.upsertHistory(backup.getEntry(BackupEntry.HISTORY))

				viewState.onProgressChanged(Progress(1, 3))
				result += repository.upsertCategories(backup.getEntry(BackupEntry.CATEGORIES))

				viewState.onProgressChanged(Progress(2, 3))
				result += repository.upsertFavourites(backup.getEntry(BackupEntry.FAVOURITES))

				viewState.onProgressChanged(Progress(3, 3))
				viewState.onRestoreDone(result)
			} finally {
				withContext(NonCancellable) {
					backup.cleanup()
					backup.file.delete()
				}
			}
		}
	}
}