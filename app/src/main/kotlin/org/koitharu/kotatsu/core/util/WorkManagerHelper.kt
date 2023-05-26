package org.koitharu.kotatsu.core.util

import android.annotation.SuppressLint
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest
import androidx.work.await
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

	suspend fun deleteWorks(ids: Collection<UUID>) = suspendCoroutine { cont ->
		workManagerImpl.workTaskExecutor.executeOnTaskThread {
			try {
				val db = workManagerImpl.workDatabase
				db.runInTransaction {
					for (id in ids) {
						db.workSpecDao().delete(id.toString())
					}
				}
				cont.resume(Unit)
			} catch (e: Exception) {
				cont.resumeWithException(e)
			}
		}
	}

	suspend fun getWorkInfosByTag(tag: String): List<WorkInfo> {
		return workManagerImpl.getWorkInfosByTag(tag).await()
	}

	suspend fun getFinishedWorkInfosByTag(tag: String): List<WorkInfo> {
		val query = WorkQuery.Builder.fromTags(listOf(tag))
			.addStates(listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED))
			.build()
		return workManagerImpl.getWorkInfos(query).await()
	}

	suspend fun getWorkInfoById(id: UUID): WorkInfo? {
		return workManagerImpl.getWorkInfoById(id).await()
	}

	suspend fun updateWork(request: WorkRequest): WorkManager.UpdateResult {
		return workManagerImpl.updateWork(request).await()
	}
}
