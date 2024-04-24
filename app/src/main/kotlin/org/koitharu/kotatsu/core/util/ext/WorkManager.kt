package org.koitharu.kotatsu.core.util.ext

import android.annotation.SuppressLint
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest
import androidx.work.await
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@SuppressLint("RestrictedApi")
suspend fun WorkManager.deleteWork(id: UUID) = suspendCoroutine { cont ->
	workManagerImpl.workTaskExecutor.executeOnTaskThread {
		try {
			workManagerImpl.workDatabase.workSpecDao().delete(id.toString())
			cont.resume(Unit)
		} catch (e: Exception) {
			cont.resumeWithException(e)
		}
	}
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.deleteWorks(ids: Collection<UUID>) = suspendCoroutine { cont ->
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

@SuppressLint("RestrictedApi")
suspend fun WorkManager.awaitWorkInfosByTag(tag: String): List<WorkInfo> {
	return getWorkInfosByTag(tag).await()
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.awaitFinishedWorkInfosByTag(tag: String): List<WorkInfo> {
	val query = WorkQuery.Builder.fromTags(listOf(tag))
		.addStates(listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED))
		.build()
	return getWorkInfos(query).await()
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.awaitWorkInfoById(id: UUID): WorkInfo? {
	return getWorkInfoById(id).await()
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.awaitUniqueWorkInfoByName(name: String): List<WorkInfo> {
	return getWorkInfosForUniqueWork(name).await().orEmpty()
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.awaitUpdateWork(request: WorkRequest): WorkManager.UpdateResult {
	return updateWork(request).await()
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.getWorkSpec(id: UUID): WorkSpec? = suspendCoroutine { cont ->
	workManagerImpl.workTaskExecutor.executeOnTaskThread {
		try {
			val spec = workManagerImpl.workDatabase.workSpecDao().getWorkSpec(id.toString())
			cont.resume(spec)
		} catch (e: Exception) {
			cont.resumeWithException(e)
		}
	}
}

@SuppressLint("RestrictedApi")
suspend fun WorkManager.getWorkInputData(id: UUID): Data? = getWorkSpec(id)?.input

val Data.isEmpty: Boolean
	get() = this == Data.EMPTY

private val WorkManager.workManagerImpl
	@SuppressLint("RestrictedApi") inline get() = this as WorkManagerImpl
