package org.koitharu.kotatsu.core.ui

import android.text.Html
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.parser.favicon.FaviconFetcher
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.CbzFetcher
import org.koitharu.kotatsu.utils.ext.isLowRamDevice
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
			ImageLoader.Builder(androidContext())
				.okHttpClient(httpClientFactory)
				.interceptorDispatcher(Dispatchers.Default)
				.fetcherDispatcher(Dispatchers.IO)
				.decoderDispatcher(Dispatchers.Default)
				.transformationDispatcher(Dispatchers.Default)
				.diskCache(diskCacheFactory)
				.logger(if (BuildConfig.DEBUG) DebugLogger() else null)
				.allowRgb565(isLowRamDevice(androidContext()))
				.components(
					ComponentRegistry.Builder()
						.add(SvgDecoder.Factory())
						.add(CbzFetcher.Factory())
						.add(FaviconFetcher.Factory(androidContext(), get()))
						.build()
				).build()
		}
		factory<Html.ImageGetter> { CoilImageGetter(androidContext(), get()) }
	}