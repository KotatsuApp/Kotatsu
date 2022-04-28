package org.koitharu.kotatsu.sync.ui

import android.content.Context
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.sync.domain.SyncAuthResult
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.toRequestBody
import java.util.*

class SyncAuthViewModel(
	context: Context,
	private val okHttpClient: OkHttpClient,
) : BaseViewModel() {

	private val baseUrl = context.getString(R.string.url_sync_server)
	val onTokenObtained = SingleLiveEvent<SyncAuthResult>()

	fun obtainToken(email: String, password: String) {
		launchLoadingJob(Dispatchers.Default) {
			authenticate(email, password)
			val token = UUID.randomUUID().toString()
			val result = SyncAuthResult(email, password, token)
			onTokenObtained.postCall(result)
		}
	}

	private suspend fun authenticate(email: String, password: String) {
		val body = JSONObject(
			mapOf("email" to email, "password" to password)
		).toRequestBody()
		val request = Request.Builder()
			.url("$baseUrl/register")
			.post(body)
			.build()
		val response = okHttpClient.newCall(request).await().parseJson()
	}
}