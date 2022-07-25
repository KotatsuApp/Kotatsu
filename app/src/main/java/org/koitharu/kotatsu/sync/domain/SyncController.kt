package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.util.ArrayMap
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.utils.ext.processLifecycleScope

@Singleton
class SyncController @Inject constructor(
	@ApplicationContext context: Context,
) : InvalidationTracker.Observer(arrayOf(TABLE_HISTORY, TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)) {

	private val am = AccountManager.get(context)
	private val accountType = context.getString(R.string.account_type_sync)
	private val minSyncInterval = if (BuildConfig.DEBUG) {
		TimeUnit.SECONDS.toMillis(5)
	} else {
		TimeUnit.MINUTES.toMillis(4)
	}
	private val mutex = Mutex()
	private val jobs = ArrayMap<String, Job>(2)
	private val defaultGcPeriod: Long // gc period if sync disabled
		get() = TimeUnit.HOURS.toMillis(2)

	override fun onInvalidated(tables: MutableSet<String>) {
		requestSync(
			favourites = TABLE_FAVOURITES in tables || TABLE_FAVOURITE_CATEGORIES in tables,
			history = TABLE_HISTORY in tables,
		)
	}

	fun getLastSync(account: Account, authority: String): Long {
		val key = "last_sync_" + authority.substringAfterLast('.')
		val rawValue = am.getUserData(account, key) ?: return 0L
		return rawValue.toLongOrNull() ?: 0L
	}

	fun setLastSync(account: Account, authority: String, time: Long) {
		val key = "last_sync_" + authority.substringAfterLast('.')
		am.setUserData(account, key, time.toString())
	}

	suspend fun requestFullSync() = withContext(Dispatchers.Default) {
		requestSyncImpl(favourites = true, history = true, db = null)
	}

	suspend fun requestFullSyncAndGc(database: MangaDatabase) = withContext(Dispatchers.Default) {
		requestSyncImpl(favourites = true, history = true, db = database)
	}

	private fun requestSync(favourites: Boolean, history: Boolean) = processLifecycleScope.launch(Dispatchers.Default) {
		requestSyncImpl(favourites = favourites, history = history, db = null)
	}

	private suspend fun requestSyncImpl(favourites: Boolean, history: Boolean, db: MangaDatabase?) = mutex.withLock {
		if (!favourites && !history) {
			return
		}
		val account = peekAccount()
		if (account == null || !ContentResolver.getMasterSyncAutomatically()) {
			db?.gc(favourites, history)
			return
		}
		var gcHistory = false
		var gcFavourites = false
		if (favourites) {
			if (ContentResolver.getSyncAutomatically(account, AUTHORITY_FAVOURITES)) {
				scheduleSync(account, AUTHORITY_FAVOURITES)
			} else {
				gcFavourites = true
			}
		}
		if (history) {
			if (ContentResolver.getSyncAutomatically(account, AUTHORITY_HISTORY)) {
				scheduleSync(account, AUTHORITY_HISTORY)
			} else {
				gcHistory = true
			}
		}
		if (db != null && (gcHistory || gcFavourites)) {
			db.gc(gcFavourites, gcHistory)
		}
	}

	private fun scheduleSync(account: Account, authority: String) {
		if (ContentResolver.isSyncActive(account, authority) || ContentResolver.isSyncPending(account, authority)) {
			return
		}
		val job = jobs[authority]
		if (job?.isActive == true) {
			// already scheduled
			return
		}
		val lastSyncTime = getLastSync(account, authority)
		val timeLeft = System.currentTimeMillis() - lastSyncTime + minSyncInterval
		if (timeLeft <= 0) {
			jobs.remove(authority)
			ContentResolver.requestSync(account, authority, Bundle.EMPTY)
		} else {
			jobs[authority] = processLifecycleScope.launch(Dispatchers.Default) {
				try {
					delay(timeLeft)
				} finally {
					// run even if scope cancelled
					ContentResolver.requestSync(account, authority, Bundle.EMPTY)
				}
			}
		}
	}

	private fun peekAccount(): Account? {
		return am.getAccountsByType(accountType).firstOrNull()
	}

	private suspend fun MangaDatabase.gc(favourites: Boolean, history: Boolean) = withTransaction {
		val deletedAt = System.currentTimeMillis() - defaultGcPeriod
		if (history) {
			historyDao.gc(deletedAt)
		}
		if (favourites) {
			favouritesDao.gc(deletedAt)
			favouriteCategoriesDao.gc(deletedAt)
		}
	}
}
