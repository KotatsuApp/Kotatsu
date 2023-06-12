package org.koitharu.kotatsu.sync.ui

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils

class SyncAccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

	override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle? = null

	override fun addAccount(
		response: AccountAuthenticatorResponse?,
		accountType: String?,
		authTokenType: String?,
		requiredFeatures: Array<out String>?,
		options: Bundle?,
	): Bundle {
		val intent = Intent(context, SyncAuthActivity::class.java)
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
		val bundle = Bundle()
		if (options != null) {
			bundle.putAll(options)
		}
		bundle.putParcelable(AccountManager.KEY_INTENT, intent)
		return bundle
	}

	override fun confirmCredentials(
		response: AccountAuthenticatorResponse?,
		account: Account?,
		options: Bundle?,
	): Bundle? = null

	override fun getAuthToken(
		response: AccountAuthenticatorResponse?,
		account: Account,
		authTokenType: String?,
		options: Bundle?,
	): Bundle {
		val result = Bundle()
		val am = AccountManager.get(context.applicationContext)
		val authToken = am.peekAuthToken(account, authTokenType)
		if (!TextUtils.isEmpty(authToken)) {
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
			result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
		} else {
			val intent = Intent(context, SyncAuthActivity::class.java)
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
			val bundle = Bundle()
			if (options != null) {
				bundle.putAll(options)
			}
			bundle.putParcelable(AccountManager.KEY_INTENT, intent)
		}
		return result
	}

	override fun getAuthTokenLabel(authTokenType: String?): String? = null

	override fun updateCredentials(
		response: AccountAuthenticatorResponse?,
		account: Account?,
		authTokenType: String?,
		options: Bundle?,
	): Bundle? = null

	override fun hasFeatures(
		response: AccountAuthenticatorResponse?,
		account: Account?,
		features: Array<out String>?,
	): Bundle? = null
}
