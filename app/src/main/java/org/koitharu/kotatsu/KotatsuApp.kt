package org.koitharu.kotatsu

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.util.CoilUtils
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.DatabasePrePopulateCallback
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.migrations.*
import org.koitharu.kotatsu.core.local.CbzFetcher
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.local.cookies.PersistentCookieJar
import org.koitharu.kotatsu.core.local.cookies.cache.SetCookieCache
import org.koitharu.kotatsu.core.local.cookies.persistence.SharedPrefsCookiePersistor
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.core.parser.UserAgentInterceptor
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.utils.AppCrashHandler
import org.koitharu.kotatsu.ui.widget.WidgetUpdater
import org.koitharu.kotatsu.utils.CacheUtils
import java.util.concurrent.TimeUnit

class KotatsuApp : Application() {

	private val chuckerCollector by lazy(LazyThreadSafetyMode.NONE) {
		ChuckerCollector(applicationContext)
	}

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
		initCoil(get())
		Thread.setDefaultUncaughtExceptionHandler(AppCrashHandler(applicationContext))
		if (BuildConfig.DEBUG) {
			initErrorHandler()
		}
		AppCompatDelegate.setDefaultNightMode(AppSettings(this).theme)
		val widgetUpdater = WidgetUpdater(applicationContext)
		FavouritesRepository.subscribe(widgetUpdater)
		HistoryRepository.subscribe(widgetUpdater)
	}

	private fun initKoin() {
		startKoin {
			androidLogger(Level.ERROR)
			androidContext(applicationContext)
			modules(
				module {
					single<CookieJar> {
						PersistentCookieJar(
							SetCookieCache(),
							SharedPrefsCookiePersistor(applicationContext)
						)
					}
					factory {
						okHttp(get())
							.cache(CacheUtils.createHttpCache(applicationContext))
							.build()
					}
					single {
						mangaDb().build()
					}
					single {
						MangaLoaderContext()
					}
					single {
						AppSettings(applicationContext)
					}
					single {
						PagesCache(applicationContext)
					}
				}
			)
		}
	}

	private fun initCoil(cookieJar: CookieJar) {
		Coil.setImageLoader(
			ImageLoader.Builder(applicationContext)
				.okHttpClient(
					okHttp(cookieJar)
						.cache(CoilUtils.createDefaultCache(applicationContext))
						.build()
				).componentRegistry(
					ComponentRegistry.Builder()
						.add(CbzFetcher())
						.build()
				)
				.build()
		)
	}

	private fun initErrorHandler() {
		val exceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler { t, e ->
			chuckerCollector.onError("CRASH", e)
			exceptionHandler?.uncaughtException(t, e)
		}
	}

	private fun okHttp(cookieJar: CookieJar) = OkHttpClient.Builder().apply {
		connectTimeout(20, TimeUnit.SECONDS)
		readTimeout(60, TimeUnit.SECONDS)
		writeTimeout(20, TimeUnit.SECONDS)
		cookieJar(cookieJar)
		addInterceptor(UserAgentInterceptor)
		if (BuildConfig.DEBUG) {
			addInterceptor(ChuckerInterceptor(applicationContext, collector = chuckerCollector))
		}
	}

	private fun mangaDb() = Room.databaseBuilder(
		applicationContext,
		MangaDatabase::class.java,
		"kotatsu-db"
	).addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6)
		.addCallback(DatabasePrePopulateCallback(resources))
}