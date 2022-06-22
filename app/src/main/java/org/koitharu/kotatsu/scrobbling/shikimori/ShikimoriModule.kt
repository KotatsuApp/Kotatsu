package org.koitharu.kotatsu.scrobbling.shikimori

import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriAuthenticator
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriInterceptor
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.scrobbling.shikimori.data.ShikimoriStorage
import org.koitharu.kotatsu.scrobbling.shikimori.domain.ShikimoriScrobbler
import org.koitharu.kotatsu.scrobbling.shikimori.ui.ShikimoriSettingsViewModel
import org.koitharu.kotatsu.scrobbling.ui.selector.ScrobblingSelectorViewModel

val shikimoriModule
	get() = module {
		single { ShikimoriStorage(androidContext()) }
		factory {
			val okHttp = OkHttpClient.Builder().apply {
				authenticator(ShikimoriAuthenticator(get(), ::get))
				addInterceptor(ShikimoriInterceptor(get()))
			}.build()
			ShikimoriRepository(okHttp, get(), get())
		}
		factory { ShikimoriScrobbler(get(), get()) } bind Scrobbler::class
		viewModel { params ->
			ShikimoriSettingsViewModel(get(), params.getOrNull())
		}
		viewModel { params -> ScrobblingSelectorViewModel(params[0], get()) }
	}