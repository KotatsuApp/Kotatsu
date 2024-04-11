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
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.domain.DeleteReadChaptersUseCase
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalStorageCleanupWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val settings: AppSettings,
	private val localMangaRepository: LocalMangaRepository,
	private val dataRepository: MangaDataRepository,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		if (settings.isAutoLocalChaptersCleanupEnabled) {
			deleteReadChaptersUseCase.invoke()
		}
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
			val request = OneTimeWorkRequestBuilder<LocalStorageCleanupWorker>()
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request).await()
		}
	}
}
