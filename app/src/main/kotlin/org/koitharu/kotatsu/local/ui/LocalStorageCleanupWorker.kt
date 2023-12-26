package org.koitharu.kotatsu.local.ui

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalStorageCleanupWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val localMangaRepository: LocalMangaRepository,
	private val dataRepository: MangaDataRepository,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		return if (localMangaRepository.cleanup()) {
			dataRepository.cleanupLocalManga()
			Result.success()
		} else {
			Result.retry()
		}
	}

	companion object {

		private const val TAG = "cleanup"

		suspend fun enqueue(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiresBatteryNotLow(true)
				.build()
			val request = OneTimeWorkRequestBuilder<ImportWorker>()
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request).await()
		}
	}
}
