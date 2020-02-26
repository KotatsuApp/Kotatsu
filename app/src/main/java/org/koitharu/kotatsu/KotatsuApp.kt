package org.koitharu.kotatsu

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import coil.Coil
import coil.ImageLoader
import coil.util.CoilUtils
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.http.persistentcookiejar.PersistentCookieJar
import org.koitharu.kotatsu.core.http.persistentcookiejar.cache.SetCookieCache
import org.koitharu.kotatsu.core.http.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import org.koitharu.kotatsu.core.local.CbzFetcher
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.utils.CacheUtils
import java.util.concurrent.TimeUnit

class KotatsuApp : Application() {

	private val cookieJar by lazy {
		PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(applicationContext))
	}

	override fun onCreate() {
		super.onCreate()
		initKoin()
		initCoil()
		AppCompatDelegate.setDefaultNightMode(AppSettings(this).theme)
	}

	private fun initKoin() {
		startKoin {
			androidLogger()
			androidContext(applicationContext)
			modules(listOf(
				module {
					factory {
						okHttp()
							.cache(CacheUtils.createHttpCache(applicationContext))
							.build()
					}
				}, module {
					single {
						MangaLoaderContext()
					}
				}, module {
					single {
						mangaDb().build()
					}
				}, module {
					factory {
						AppSettings(applicationContext)
					}
				}, module {
					single {
						PagesCache(applicationContext)
					}
				}
			))
		}
	}

	private fun initCoil() {
		Coil.setDefaultImageLoader(ImageLoader(applicationContext) {
			okHttpClient {
				okHttp()
					.cache(CoilUtils.createDefaultCache(applicationContext))
					.build()
			}
			componentRegistry {
				add(CbzFetcher())
			}
		})
	}

	private fun okHttp() = OkHttpClient.Builder()
		.connectTimeout(20, TimeUnit.SECONDS)
		.readTimeout(60, TimeUnit.SECONDS)
		.writeTimeout(20, TimeUnit.SECONDS)
		.cookieJar(cookieJar)

	private fun mangaDb() = Room.databaseBuilder(
		applicationContext,
		MangaDatabase::class.java,
		"kotatsu-db"
	)
}