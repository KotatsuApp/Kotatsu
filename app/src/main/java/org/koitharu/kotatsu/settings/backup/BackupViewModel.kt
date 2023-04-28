package org.koitharu.kotatsu.settings.backup

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val progress = MutableLiveData(-1f)
	val onBackupDone = SingleLiveEvent<File>()

	init {
		launchLoadingJob {
			val file = BackupZipOutput(context).use { backup ->
				backup.put(repository.createIndex())

				progress.value = 0f
				backup.put(repository.dumpHistory())

				progress.value = 0.3f
				backup.put(repository.dumpCategories())

				progress.value = 0.6f
				backup.put(repository.dumpFavourites())

				progress.value = 0.9f
				backup.finish()
				progress.value = 1f
				backup.close()
				backup.file
			}
			onBackupDone.call(file)
		}
	}
}
