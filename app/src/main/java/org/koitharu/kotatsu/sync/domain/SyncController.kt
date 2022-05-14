package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import androidx.room.InvalidationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.utils.ext.processLifecycleScope

class SyncController(
	context: Context,
) : InvalidationTracker.Observer(arrayOf(TABLE_HISTORY, TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)) {

	private val am = AccountManager.get(context)
	private val accountType = context.getString(R.string.account_type_sync)

	override fun onInvalidated(tables: MutableSet<String>) {
		requestSync(
			favourites = TABLE_FAVOURITES in tables || TABLE_FAVOURITE_CATEGORIES in tables,
			history = TABLE_HISTORY in tables,
		)
	}

	suspend fun requestFullSync() = withContext(Dispatchers.Default) {
		requestSyncImpl(favourites = true, history = true)
	}

	private fun requestSync(favourites: Boolean, history: Boolean) = processLifecycleScope.launch(Dispatchers.Default) {
		requestSyncImpl(favourites, history)
	}

	@Synchronized
	private fun requestSyncImpl(favourites: Boolean, history: Boolean) {
		if (!favourites && !history) {
			return
		}
		val account = peekAccount() ?: return
		if (!ContentResolver.getMasterSyncAutomatically()) {
			return
		}
		// TODO limit frequency
		if (favourites) {
			requestSyncForAuthority(account, AUTHORITY_FAVOURITES)
		}
		if (history) {
			requestSyncForAuthority(account, AUTHORITY_HISTORY)
		}
	}

	private fun requestSyncForAuthority(account: Account, authority: String) {
		if (
			ContentResolver.getSyncAutomatically(account, AUTHORITY_FAVOURITES) &&
			!ContentResolver.isSyncActive(account, authority) &&
			!ContentResolver.isSyncPending(account, authority)
		) {
			ContentResolver.requestSync(account, authority, Bundle.EMPTY)
		}
	}

	private fun peekAccount(): Account? {
		return am.getAccountsByType(accountType).firstOrNull()
	}
}