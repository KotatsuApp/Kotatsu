package org.koitharu.kotatsu.core.ui

import coil.ComponentRegistry
import coil.ImageLoader
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.FaviconMapper
import org.koitharu.kotatsu.local.data.CbzFetcher

val uiModule
	get() = module {
		single {
			ImageLoader.Builder(androidContext())
				.okHttpClient(get<OkHttpClient>())
				.componentRegistry(
					ComponentRegistry.Builder()
						.add(CbzFetcher())
						.add(FaviconMapper())
						.build()
				).build()
		}
	}