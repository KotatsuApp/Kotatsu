package org.koitharu.kotatsu

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.databaseModule
import org.koitharu.kotatsu.core.github.githubModule
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.network.networkModule
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaDataRepository
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.MangaSearchRepository
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.base.uiModule
import org.koitharu.kotatsu.ui.utils.AppCrashHandler
import org.koitharu.kotatsu.ui.widget.WidgetUpdater

class KotatsuApp : Application() {

	override fun onCreate() {
		super.onCreate()
		if (BuildConfig.DEBUG) {
			StrictMode.setThreadPolicy(
				StrictMode.ThreadPolicy.Builder()
					.detectAll()
					.penaltyLog()
					.build()
			)
			StrictMode.setVmPolicy(
				StrictMode.VmPolicy.Builder()
					.detectAll()
					.setClassInstanceLimit(LocalMangaRepository::class.java, 1)
					.setClassInstanceLimit(PagesCache::class.java, 1)
					.setClassInstanceLimit(MangaLoaderContext::class.java, 1)
					.penaltyLog()
					.build()
			)
		}
		initKoin()
		Thread.setDefaultUncaughtExceptionHandler(AppCrashHandler(applicationContext))
		AppCompatDelegate.setDefaultNightMode(get<AppSettings>().theme)
		val widgetUpdater = WidgetUpdater(applicationContext)
		FavouritesRepository.subscribe(widgetUpdater)
		HistoryRepository.subscribe(widgetUpdater)
	}

	private fun initKoin() {
		startKoin {
			androidContext(this@KotatsuApp)
			modules(
				networkModule,
				databaseModule,
				githubModule,
				uiModule,
				module {
					single { FavouritesRepository(get()) }
					single { HistoryRepository(get()) }
					single { TrackingRepository(get()) }
					single { MangaDataRepository(get()) }
					single { MangaSearchRepository() }
					single { MangaLoaderContext() }
					single { AppSettings(get()) }
					single { PagesCache(get()) }
				}
			)
		}
	}
}