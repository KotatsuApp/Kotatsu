package org.koitharu.kotatsu.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class TestResponse(
	val code: Int,
	val type: String?,
	val subtype: String?,
) {

	companion object : KoinComponent {

		private val okHttp by inject<OkHttpClient>()

		fun testRequest(url: String): TestResponse {
			val request = Request.Builder()
				.url(url)
				.head()
				.build()
			val response = okHttp.newCall(request).execute()
			val type = response.body?.contentType()
			return TestResponse(
				code = response.code,
				type = type?.type,
				subtype = type?.subtype,
			)
		}
	}
}
