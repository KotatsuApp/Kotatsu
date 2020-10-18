package org.koitharu.kotatsu.core.github

import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.parseJson

class GithubRepository(private val okHttp: OkHttpClient) {

	suspend fun getLatestVersion(): AppVersion {
		val request = Request.Builder()
			.get()
			.url("https://api.github.com/repos/nv95/Kotatsu/releases/latest")
		val json = okHttp.newCall(request.build()).await().parseJson()
		val asset = json.getJSONArray("assets").getJSONObject(0)
		return AppVersion(
			id = json.getLong("id"),
			url = json.getString("html_url"),
			name = json.getString("name").removePrefix("v"),
			apkSize = asset.getLong("size"),
			apkUrl = asset.getString("browser_download_url"),
			description = json.getString("body")
		)
	}
}