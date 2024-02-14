package org.koitharu.kotatsu.sync.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.WorkerThread
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import javax.inject.Inject

class SyncSettings(
	context: Context,
	private val account: Account?,
) {

	@Inject
	constructor(@ApplicationContext context: Context) : this(
		context,
		AccountManager.get(context)
			.getAccountsByType(context.getString(R.string.account_type_sync))
			.firstOrNull(),
	)

	private val accountManager = AccountManager.get(context)
	private val defaultHost = context.getString(R.string.sync_host_default)

	@get:WorkerThread
	@set:WorkerThread
	var host: String
		get() = account?.let {
			accountManager.getUserData(it, KEY_HOST)
		}.ifNullOrEmpty { defaultHost }
		set(value) {
			account?.let {
				accountManager.setUserData(it, KEY_HOST, value)
			}
		}

	companion object {

		const val KEY_HOST = "host"
	}
}
