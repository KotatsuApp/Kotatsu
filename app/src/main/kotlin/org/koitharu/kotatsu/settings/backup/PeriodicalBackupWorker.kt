package org.koitharu.kotatsu.settings.backup

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okio.buffer
import okio.sink
import okio.source
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.awaitUniqueWorkInfoByName
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.settings.work.PeriodicWorkScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class PeriodicalBackupWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val repository: BackupRepository,
	private val settings: AppSettings,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		val file = BackupZipOutput(applicationContext).use { backup ->
			backup.put(repository.createIndex())
			backup.put(repository.dumpHistory())
			backup.put(repository.dumpCategories())
			backup.put(repository.dumpFavourites())
			backup.put(repository.dumpBookmarks())
			backup.put(repository.dumpSettings())
			backup.finish()
			backup.file
		}
		return settings.periodicalBackupOutput?.let {
			applicationContext.contentResolver.openOutputStream(it)?.use { output ->
				file.source().use { input ->
					output.sink().buffer().writeAllCancellable(input)
				}
				Result.success()
			} ?: Result.failure()
		} ?: Result.success()
	}

	@Reusable
	class Scheduler @Inject constructor(
		private val workManager: WorkManager,
		private val settings: AppSettings,
	) : PeriodicWorkScheduler {

		override suspend fun schedule() {
			val constraints = Constraints.Builder()
				.setRequiresStorageNotLow(true)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				constraints.setRequiresDeviceIdle(true)
			}
			val request = PeriodicWorkRequestBuilder<PeriodicalBackupWorker>(
				settings.periodicalBackupFrequency,
				TimeUnit.HOURS,
			).setConstraints(constraints.build())
				.addTag(TAG)
				.build()
			workManager
				.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
				.await()
		}

		override suspend fun unschedule() {
			workManager
				.cancelUniqueWork(TAG)
				.await()
		}

		override suspend fun isScheduled(): Boolean {
			return workManager
				.awaitUniqueWorkInfoByName(TAG)
				.any { !it.state.isFinished }
		}
	}

	private companion object {

		const val TAG = "backups"
	}
}
