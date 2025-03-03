package org.koitharu.kotatsu.settings.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import okio.FileNotFoundException
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.progress.Progress
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
	private val repository: BackupRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val progress = MutableStateFlow(Progress.INDETERMINATE)
	val onBackupDone = MutableEventFlow<File>()
	val onBackupSaved = MutableEventFlow<Unit>()

	private val contentResolver: ContentResolver = context.contentResolver
	private var backupFile: File? = null

	init {
		launchLoadingJob(Dispatchers.Default) {
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
			backupFile = file
			onBackupDone.call(file)
		}
	}

	fun saveBackup(output: Uri) {
		launchLoadingJob(Dispatchers.Default) {
			val file = backupFile ?: throw FileNotFoundException()
			contentResolver.openFileDescriptor(output, "w")?.use { fd ->
				FileOutputStream(fd.fileDescriptor).use {
					it.write(file.readBytes())
				}
			}
			onBackupSaved.call(Unit)
		}
	}
}
