package org.koitharu.kotatsu.shikimori

import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.shikimori.data.ShikimoriAuthenticator
import org.koitharu.kotatsu.shikimori.data.ShikimoriInterceptor
import org.koitharu.kotatsu.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.shikimori.data.ShikimoriStorage
import org.koitharu.kotatsu.shikimori.ui.ShikimoriSettingsViewModel
import org.koitharu.kotatsu.shikimori.ui.selector.ShikimoriSelectorViewModel

val shikimoriModule
	get() = module {
		single { ShikimoriStorage(androidContext()) }
		factory {
			val okHttp = OkHttpClient.Builder().apply {
				authenticator(ShikimoriAuthenticator(get(), ::get))
				addInterceptor(ShikimoriInterceptor(get()))
			}.build()
			ShikimoriRepository(okHttp, get())
		}
		viewModel { params ->
			ShikimoriSettingsViewModel(get(), params.getOrNull())
		}
		viewModel { params -> ShikimoriSelectorViewModel(params[0], get()) }
	}