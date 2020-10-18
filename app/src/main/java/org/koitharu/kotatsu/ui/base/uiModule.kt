package org.koitharu.kotatsu.ui.base

import coil.ComponentRegistry
import coil.ImageLoader
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.core.local.CbzFetcher

val uiModule
	get() = module {
		single {
			ImageLoader.Builder(androidContext())
				.okHttpClient(get<OkHttpClient>())
				.componentRegistry(
					ComponentRegistry.Builder()
						.add(CbzFetcher())
						.build()
				).build()
		}
	}