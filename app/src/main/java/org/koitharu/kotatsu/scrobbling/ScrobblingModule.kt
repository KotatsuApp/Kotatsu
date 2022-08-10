package org.koitharu.kotatsu.scrobbling

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriAuthenticator
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriInterceptor
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriStorage
import org.koitharu.kotatsu.scrobbling.shikimori.domain.ShikimoriScrobbler

@Module
@InstallIn(SingletonComponent::class)
object ScrobblingModule {

	@Provides
	@Singleton
	fun provideShikimoriRepository(
		storage: ShikimoriStorage,
		database: MangaDatabase,
		authenticator: ShikimoriAuthenticator,
	): ShikimoriRepository {
		val okHttp = OkHttpClient.Builder().apply {
			authenticator(authenticator)
			addInterceptor(ShikimoriInterceptor(storage))
		}.build()
		return ShikimoriRepository(okHttp, storage, database)
	}

	@Provides
	@ElementsIntoSet
	fun provideScrobblers(
		shikimoriScrobbler: ShikimoriScrobbler,
	): Set<@JvmSuppressWildcards Scrobbler> = setOf(shikimoriScrobbler)
}
