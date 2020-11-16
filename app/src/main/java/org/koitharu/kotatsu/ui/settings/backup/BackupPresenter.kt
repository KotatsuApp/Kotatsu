package org.koitharu.kotatsu.ui.settings.backup

import org.koin.core.component.get
import org.koitharu.kotatsu.core.backup.BackupArchive
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.utils.progress.Progress

class BackupPresenter(
	private val repository: BackupRepository
) : BasePresenter<BackupView>() {

	override fun onFirstViewAttach() {
		super.onFirstViewAttach()
		launchLoadingJob {
			viewState.onProgressChanged(null)
			val backup = BackupArchive.createNew(get())
			backup.put(repository.createIndex())

			viewState.onProgressChanged(Progress(0, 3))
			backup.put(repository.dumpHistory())

			viewState.onProgressChanged(Progress(1, 3))
			backup.put(repository.dumpCategories())

			viewState.onProgressChanged(Progress(2, 3))
			backup.put(repository.dumpFavourites())

			viewState.onProgressChanged(Progress(3, 3))
			backup.flush()
			viewState.onProgressChanged(null)
			backup.cleanup()
			viewState.onBackupDone(backup.file)
		}
	}
}