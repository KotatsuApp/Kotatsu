package org.koitharu.kotatsu

import android.app.Application
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
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaLoaderContext
import java.util.concurrent.TimeUnit

class KotatsuApp : Application() {

	override fun onCreate() {
		super.onCreate()
		initKoin()
		initCoil()
	}

	private fun initKoin() {
		startKoin {
			androidLogger()
			androidContext(applicationContext)
			modules(listOf(
				module {
					factory {
						okHttp().build()
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
		})
	}

	private fun okHttp() = OkHttpClient.Builder()
		.connectTimeout(20, TimeUnit.SECONDS)
		.readTimeout(60, TimeUnit.SECONDS)
		.writeTimeout(20, TimeUnit.SECONDS)

	private fun mangaDb() = Room.databaseBuilder(
		applicationContext,
		MangaDatabase::class.java,
		"kotatsu-db"
	)
}