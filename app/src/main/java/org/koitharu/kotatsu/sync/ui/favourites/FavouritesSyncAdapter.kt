package org.koitharu.kotatsu.sync.ui.favourites

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import org.koitharu.kotatsu.sync.domain.SyncHelper
import org.koitharu.kotatsu.utils.ext.onError

class FavouritesSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true) {

	override fun onPerformSync(
		account: Account,
		extras: Bundle,
		authority: String,
		provider: ContentProviderClient,
		syncResult: SyncResult,
	) {
		val syncHelper = SyncHelper(context, account, provider)
		runCatching {
			syncHelper.syncFavourites(syncResult)
		}.onFailure(syncResult::onError)
	}
}