package org.koitharu.kotatsu.utils

import android.annotation.SuppressLint
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@SuppressLint("RestrictedApi")
class WorkManagerHelper(
	workManager: WorkManager,
) {

	private val workManagerImpl = workManager as WorkManagerImpl

	suspend fun deleteWork(id: UUID) = suspendCoroutine { cont ->
		workManagerImpl.workTaskExecutor.executeOnTaskThread {
			try {
				workManagerImpl.workDatabase.workSpecDao().delete(id.toString())
				cont.resume(Unit)
			} catch (e: Exception) {
				cont.resumeWithException(e)
			}
		}
	}
}
