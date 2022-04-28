package org.koitharu.kotatsu.sync.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class AccountInterceptor(
	context: Context,
	private val account: Account,
) : Interceptor {

	private val accountManager = AccountManager.get(context)

	override fun intercept(chain: Interceptor.Chain): Response {
		val password = accountManager.getPassword(account)
		val request = if (password != null) {
			val credential: String = Credentials.basic(account.name, password)
			chain.request().newBuilder()
				.header("Authorization", credential)
				.build()
		} else {
			chain.request()
		}
		return chain.proceed(request)
	}
}