package org.koitharu.kotatsu.settings.backup

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.progress.Progress
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val progress = MutableStateFlow(Progress.INDETERMINATE)
	val onBackupDone = MutableEventFlow<File>()

	init {
		launchLoadingJob {
			val file = BackupZipOutput.createTemp(context).use { backup ->
				progress.value = Progress(0, 7)
				backup.put(repository.createIndex())

				backup.put(repository.dumpHistory())
				progress.value = progress.value.inc()

				backup.put(repository.dumpCategories())
				progress.value = progress.value.inc()

				backup.put(repository.dumpFavourites())
				progress.value = progress.value.inc()

				backup.put(repository.dumpBookmarks())
				progress.value = progress.value.inc()

				backup.put(repository.dumpSources())
				progress.value = progress.value.inc()

				backup.put(repository.dumpSettings())
				progress.value = progress.value.inc()

				backup.put(repository.dumpReaderGridSettings())
				progress.value = progress.value.inc()

				backup.finish()
				backup.file
			}
			onBackupDone.call(file)
		}
	}
}
