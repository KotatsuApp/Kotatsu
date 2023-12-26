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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val progress = MutableStateFlow(-1f)
	val onBackupDone = MutableEventFlow<File>()

	init {
		launchLoadingJob {
			val file = BackupZipOutput(context).use { backup ->
				val step = 1f / 6f
				backup.put(repository.createIndex())

				progress.value = 0f
				backup.put(repository.dumpHistory())

				progress.value += step
				backup.put(repository.dumpCategories())

				progress.value += step
				backup.put(repository.dumpFavourites())

				progress.value += step
				backup.put(repository.dumpBookmarks())

				progress.value += step
				backup.put(repository.dumpSources())

				progress.value += step
				backup.put(repository.dumpSettings())

				backup.finish()
				progress.value = 1f
				backup.file
			}
			onBackupDone.call(file)
		}
	}
}
