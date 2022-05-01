package org.koitharu.kotatsu.sync.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.R

class AccountInterceptor(
	context: Context,
	private val account: Account,
) : Interceptor {

	private val accountManager = AccountManager.get(context)
	private val tokenType = context.getString(R.string.account_type_sync)

	override fun intercept(chain: Interceptor.Chain): Response {
		val token = accountManager.peekAuthToken(account, tokenType)
		val request = if (token != null) {
			chain.request().newBuilder()
				.header("Authorization", "Bearer $token")
				.build()
		} else {
			chain.request()
		}
		return chain.proceed(request)
	}
}