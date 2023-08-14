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
				backup.put(repository.createIndex())

				progress.value = 0f
				backup.put(repository.dumpHistory())

				progress.value = 0.2f
				backup.put(repository.dumpCategories())

				progress.value = 0.4f
				backup.put(repository.dumpFavourites())

				progress.value = 0.6f
				backup.put(repository.dumpBookmarks())

				progress.value = 0.8f
				backup.put(repository.dumpSettings())

				backup.finish()
				progress.value = 1f
				backup.close()
				backup.file
			}
			onBackupDone.call(file)
		}
	}
}
