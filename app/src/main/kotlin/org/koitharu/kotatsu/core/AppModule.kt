package org.koitharu.kotatsu.core

import android.app.Application
import android.content.Context
import android.provider.SearchRecentSuggestions
import android.text.Html
import androidx.collection.arraySetOf
import androidx.room.InvalidationTracker
import androidx.work.WorkManager
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.util.DebugLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.browser.cloudflare.CaptchaNotifier
import org.koitharu.kotatsu.core.cache.ContentCache
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.cache.StubContentCache
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.network.ImageProxyInterceptor
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.parser.MangaLoaderContextImpl
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.favicon.FaviconFetcher
import org.koitharu.kotatsu.core.ui.image.CoilImageGetter
import org.koitharu.kotatsu.core.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.util.AcraScreenLogger
import org.koitharu.kotatsu.core.util.IncognitoModeIndicator
import org.koitharu.kotatsu.core.util.ext.connectivityManager
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.CbzFetcher
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.main.domain.CoverRestoreInterceptor
import org.koitharu.kotatsu.main.ui.protect.AppProtectHelper
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.reader.ui.thumbnails.MangaPageFetcher
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import org.koitharu.kotatsu.settings.backup.BackupObserver
import org.koitharu.kotatsu.sync.domain.SyncController
import org.koitharu.kotatsu.widget.WidgetUpdater
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {

	@Binds
	fun bindMangaLoaderContext(mangaLoaderContextImpl: MangaLoaderContextImpl): MangaLoaderContext

	@Binds
	fun bindImageGetter(coilImageGetter: CoilImageGetter): Html.ImageGetter

	companion object {

		@Provides
		@Singleton
		fun provideNetworkState(
			@ApplicationContext context: Context
		) = NetworkState(context.connectivityManager)

		@Provides
		@Singleton
		fun provideMangaDatabase(
			@ApplicationContext context: Context,
		): MangaDatabase {
			return MangaDatabase(context)
		}

		@Provides
		@Singleton
		fun provideCoil(
			@ApplicationContext context: Context,
			@MangaHttpClient okHttpClient: OkHttpClient,
			mangaRepositoryFactory: MangaRepository.Factory,
			imageProxyInterceptor: ImageProxyInterceptor,
			pageFetcherFactory: MangaPageFetcher.Factory,
			coverRestoreInterceptor: CoverRestoreInterceptor,
		): ImageLoader {
			val diskCacheFactory = {
				val rootDir = context.externalCacheDir ?: context.cacheDir
				DiskCache.Builder()
					.directory(rootDir.resolve(CacheDir.THUMBS.dir))
					.build()
			}
			return ImageLoader.Builder(context)
				.okHttpClient(okHttpClient.newBuilder().cache(null).build())
				.interceptorDispatcher(Dispatchers.Default)
				.fetcherDispatcher(Dispatchers.IO)
				.decoderDispatcher(Dispatchers.Default)
				.transformationDispatcher(Dispatchers.Default)
				.diskCache(diskCacheFactory)
				.logger(if (BuildConfig.DEBUG) DebugLogger() else null)
				.allowRgb565(context.isLowRamDevice())
				.eventListener(CaptchaNotifier(context))
				.components(
					ComponentRegistry.Builder()
						.add(SvgDecoder.Factory())
						.add(CbzFetcher.Factory())
						.add(FaviconFetcher.Factory(context, okHttpClient, mangaRepositoryFactory))
						.add(pageFetcherFactory)
						.add(imageProxyInterceptor)
						.add(coverRestoreInterceptor)
						.build(),
				).build()
		}

		@Provides
		fun provideSearchSuggestions(
			@ApplicationContext context: Context,
		): SearchRecentSuggestions {
			return MangaSuggestionsProvider.createSuggestions(context)
		}

		@Provides
		@ElementsIntoSet
		fun provideDatabaseObservers(
			widgetUpdater: WidgetUpdater,
			appShortcutManager: AppShortcutManager,
			backupObserver: BackupObserver,
			syncController: SyncController,
		): Set<@JvmSuppressWildcards InvalidationTracker.Observer> = arraySetOf(
			widgetUpdater,
			appShortcutManager,
			backupObserver,
			syncController,
		)

		@Provides
		@ElementsIntoSet
		fun provideActivityLifecycleCallbacks(
			appProtectHelper: AppProtectHelper,
			activityRecreationHandle: ActivityRecreationHandle,
			incognitoModeIndicator: IncognitoModeIndicator,
			acraScreenLogger: AcraScreenLogger,
		): Set<@JvmSuppressWildcards Application.ActivityLifecycleCallbacks> = arraySetOf(
			appProtectHelper,
			activityRecreationHandle,
			incognitoModeIndicator,
			acraScreenLogger,
		)

		@Provides
		@Singleton
		fun provideContentCache(
			application: Application,
		): ContentCache {
			return if (application.isLowRamDevice()) {
				StubContentCache()
			} else {
				MemoryContentCache(application)
			}
		}

		@Provides
		@Singleton
		@LocalStorageChanges
		fun provideMutableLocalStorageChangesFlow(): MutableSharedFlow<LocalManga?> = MutableSharedFlow()

		@Provides
		@LocalStorageChanges
		fun provideLocalStorageChangesFlow(
			@LocalStorageChanges flow: MutableSharedFlow<LocalManga?>,
		): SharedFlow<LocalManga?> = flow.asSharedFlow()

		@Provides
		fun provideWorkManager(
			@ApplicationContext context: Context,
		): WorkManager = WorkManager.getInstance(context)
	}
}
