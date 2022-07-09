package org.koitharu.kotatsu.core.ui

import android.app.ActivityManager
import android.content.Context
import android.text.Html
import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.core.parser.FaviconMapper
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.CbzFetcher
import org.koitharu.kotatsu.utils.image.CoilImageGetter

val uiModule
	get() = module {
		single {
			val httpClientFactory = {
				get<OkHttpClient>().newBuilder()
					.cache(null)
					.build()
			}
			val diskCacheFactory = {
				val context = androidContext()
				val rootDir = context.externalCacheDir ?: context.cacheDir
				DiskCache.Builder()
					.directory(rootDir.resolve(CacheDir.THUMBS.dir))
					.build()
			}
			val activityManager = androidContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
			ImageLoader.Builder(androidContext())
				.okHttpClient(httpClientFactory)
				.interceptorDispatcher(Dispatchers.Default)
				.fetcherDispatcher(Dispatchers.IO)
				.decoderDispatcher(Dispatchers.Default)
				.transformationDispatcher(Dispatchers.Default)
				.diskCache(diskCacheFactory)
				.allowRgb565(activityManager.isLowRamDevice)
				.components(
					ComponentRegistry.Builder()
						.add(CbzFetcher.Factory())
						.add(FaviconMapper())
						.build()
				).build()
		}
		factory<Html.ImageGetter> { CoilImageGetter(androidContext(), get()) }
	}