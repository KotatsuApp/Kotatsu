package org.koitharu.kotatsu

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoaderBuilder
import coil.util.CoilUtils
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.DatabasePrePopulateCallback
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.migrations.*
import org.koitharu.kotatsu.core.local.CbzFetcher
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.local.cookies.PersistentCookieJar
import org.koitharu.kotatsu.core.local.cookies.cache.SetCookieCache
import org.koitharu.kotatsu.core.local.cookies.persistence.SharedPrefsCookiePersistor
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

	private val cookieJar by lazy {
		PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(applicationContext))
	}

	private val chuckerCollector by lazy(LazyThreadSafetyMode.NONE) {
		ChuckerCollector(applicationContext)
	}

	override fun onCreate() {
		super.onCreate()
		initKoin()
		initCoil()
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
			androidLogger()
			androidContext(applicationContext)
			modules(
				module {
					factory {
						okHttp()
							.cache(CacheUtils.createHttpCache(applicationContext))
							.build()
					}
					single {
						mangaDb().build()
					}
					single {
						MangaLoaderContext()
					}
					factory {
						AppSettings(applicationContext)
					}
					single {
						PagesCache(applicationContext)
					}
				}
			)
		}
	}

	private fun initCoil() {
		Coil.setImageLoader(
			ImageLoaderBuilder(applicationContext)
				.okHttpClient(
					okHttp()
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

	private fun okHttp() = OkHttpClient.Builder().apply {
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