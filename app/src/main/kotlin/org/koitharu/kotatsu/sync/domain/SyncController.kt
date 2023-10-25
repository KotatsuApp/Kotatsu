package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
import android.content.Context
import android.os.Bundle
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class SyncController @Inject constructor(
	@ApplicationContext context: Context,
	private val dbProvider: Provider<MangaDatabase>,
) : InvalidationTracker.Observer(arrayOf(TABLE_HISTORY, TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)) {

	private val authorityHistory = context.getString(R.string.sync_authority_history)
	private val authorityFavourites = context.getString(R.string.sync_authority_favourites)
	private val am = AccountManager.get(context)
	private val accountType = context.getString(R.string.account_type_sync)
	private val mutex = Mutex()
	private val defaultGcPeriod = TimeUnit.DAYS.toMillis(2) // gc period if sync disabled

	override fun onInvalidated(tables: Set<String>) {
		val favourites = (TABLE_FAVOURITES in tables || TABLE_FAVOURITE_CATEGORIES in tables)
			&& !isSyncActiveOrPending(authorityFavourites)
		val history = TABLE_HISTORY in tables && !isSyncActiveOrPending(authorityHistory)
		if (favourites || history) {
			requestSync(favourites, history)
		}
	}

	fun isEnabled(account: Account): Boolean {
		return ContentResolver.getMasterSyncAutomatically() && (ContentResolver.getSyncAutomatically(
			account,
			authorityFavourites,
		) || ContentResolver.getSyncAutomatically(
			account,
			authorityHistory,
		))
	}

	fun getLastSync(account: Account, authority: String): Long {
		val key = "last_sync_" + authority.substringAfterLast('.')
		val rawValue = am.getUserData(account, key) ?: return 0L
		return rawValue.toLongOrNull() ?: 0L
	}

	fun observeSyncStatus(): Flow<Boolean> = callbackFlow {
		val handle = ContentResolver.addStatusChangeListener(SYNC_OBSERVER_TYPE_ACTIVE) { which ->
			trySendBlocking(which and SYNC_OBSERVER_TYPE_ACTIVE != 0)
		}
		awaitClose { ContentResolver.removeStatusChangeListener(handle) }
	}

	suspend fun requestFullSync() = withContext(Dispatchers.Default) {
		requestSyncImpl(favourites = true, history = true)
	}

	private fun requestSync(favourites: Boolean, history: Boolean) = processLifecycleScope.launch(Dispatchers.Default) {
		requestSyncImpl(favourites = favourites, history = history)
	}

	private suspend fun requestSyncImpl(favourites: Boolean, history: Boolean) = mutex.withLock {
		if (!favourites && !history) {
			return
		}
		val db = dbProvider.get()
		val account = peekAccount()
		if (account == null || !ContentResolver.getMasterSyncAutomatically()) {
			db.gc(favourites, history)
			return
		}
		var gcHistory = false
		var gcFavourites = false
		if (favourites) {
			if (ContentResolver.getSyncAutomatically(account, authorityFavourites)) {
				ContentResolver.requestSync(account, authorityFavourites, Bundle.EMPTY)
			} else {
				gcFavourites = true
			}
		}
		if (history) {
			if (ContentResolver.getSyncAutomatically(account, authorityHistory)) {
				ContentResolver.requestSync(account, authorityHistory, Bundle.EMPTY)
			} else {
				gcHistory = true
			}
		}
		if (gcHistory || gcFavourites) {
			db.gc(gcFavourites, gcHistory)
		}
	}

	private fun peekAccount(): Account? {
		return am.getAccountsByType(accountType).firstOrNull()
	}

	private suspend fun MangaDatabase.gc(favourites: Boolean, history: Boolean) = withTransaction {
		val deletedAt = System.currentTimeMillis() - defaultGcPeriod
		if (history) {
			getHistoryDao().gc(deletedAt)
		}
		if (favourites) {
			getFavouritesDao().gc(deletedAt)
			getFavouriteCategoriesDao().gc(deletedAt)
		}
	}

	private fun isSyncActiveOrPending(authority: String): Boolean {
		val account = peekAccount() ?: return false
		return ContentResolver.isSyncActive(account, authority) || ContentResolver.isSyncPending(account, authority)
	}

	companion object {

		@JvmStatic
		fun setLastSync(context: Context, account: Account, authority: String, time: Long) {
			val key = "last_sync_" + authority.substringAfterLast('.')
			val am = AccountManager.get(context)
			am.setUserData(account, key, time.toString())
		}
	}
}
