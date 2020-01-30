package org.koitharu.kotatsu

import android.app.Application
import androidx.room.Room
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.domain.MangaLoaderContext
import java.util.concurrent.TimeUnit

class KotatsuApp : Application() {

	override fun onCreate() {
		super.onCreate()
		initKoin()
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
						MangaLoaderContext(applicationContext)
					}
				}, module {
					single {
						mangaDb().build()
					}
				}
			))
		}
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