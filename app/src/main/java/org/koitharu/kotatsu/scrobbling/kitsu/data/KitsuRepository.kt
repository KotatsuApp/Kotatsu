package org.koitharu.kotatsu.scrobbling.kitsu.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerRepository
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerMangaInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser

private const val BASE_WEB_URL = "https://kitsu.io"

class KitsuRepository(
	@ApplicationContext context: Context,
	private val okHttp: OkHttpClient,
	private val storage: ScrobblerStorage,
	private val db: MangaDatabase,
) : ScrobblerRepository {

	private val clientId = context.getString(R.string.kitsu_clientId)
	private val clientSecret = context.getString(R.string.kitsu_clientSecret)

	override val oauthUrl: String
		get() = "${BASE_WEB_URL}/api/oauth2/token" +
			"?username=..." + // Get from AlertDialog...
			"&password=..." + // Get from AlertDialog...
			"&grant_type=password" +
			"&client_id=$clientId" +
			"&client_secret=$clientSecret"

	override val isAuthorized: Boolean
		get() = TODO("Not yet implemented")

	override val cachedUser: ScrobblerUser?
		get() = TODO("Not yet implemented")

	override suspend fun authorize(code: String?) {
		TODO("Not yet implemented")
	}

	override suspend fun loadUser(): ScrobblerUser {
		TODO("Not yet implemented")
	}

	override fun logout() {
		TODO("Not yet implemented")
	}

	override suspend fun unregister(mangaId: Long) {
		TODO("Not yet implemented")
	}

	override suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		TODO("Not yet implemented")
	}

	override suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		TODO("Not yet implemented")
	}

	override suspend fun createRate(mangaId: Long, scrobblerMangaId: Long) {
		TODO("Not yet implemented")
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, chapter: MangaChapter) {
		TODO("Not yet implemented")
	}

	override suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?) {
		TODO("Not yet implemented")
	}

}
