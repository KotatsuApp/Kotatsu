package org.koitharu.kotatsu.core.ui

import coil.ComponentRegistry
import coil.ImageLoader
import coil.util.CoilUtils
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.FaviconMapper
import org.koitharu.kotatsu.local.data.CbzFetcher

val uiModule
	get() = module {
		single {
			val httpClient = get<OkHttpClient>().newBuilder()
				.cache(CoilUtils.createDefaultCache(androidContext()))
				.build()
			ImageLoader.Builder(androidContext())
				.okHttpClient(httpClient)
				.componentRegistry(
					ComponentRegistry.Builder()
						.add(CbzFetcher())
						.add(FaviconMapper())
						.build()
				).build()
		}
	}