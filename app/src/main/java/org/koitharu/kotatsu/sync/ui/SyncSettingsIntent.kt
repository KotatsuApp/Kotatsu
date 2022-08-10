package org.koitharu.kotatsu.sync.ui

import android.accounts.Account
import android.content.Intent
import android.os.Bundle

private const val ACCOUNT_KEY = "account"
private const val ACTION_ACCOUNT_SYNC_SETTINGS = "android.settings.ACCOUNT_SYNC_SETTINGS"
private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

@Suppress("FunctionName")
fun SyncSettingsIntent(account: Account): Intent {
	val args = Bundle(1)
	args.putParcelable(ACCOUNT_KEY, account)
	val intent = Intent(ACTION_ACCOUNT_SYNC_SETTINGS)
	intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
	return intent
}
