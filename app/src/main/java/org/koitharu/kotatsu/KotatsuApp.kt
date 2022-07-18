package org.koitharu.kotatsu

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.strictmode.FragmentStrictMode
import androidx.room.InvalidationTracker
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koitharu.kotatsu.bookmarks.bookmarksModule
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.databaseModule
import org.koitharu.kotatsu.core.github.githubModule
import org.koitharu.kotatsu.core.network.networkModule
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.uiModule
import org.koitharu.kotatsu.details.detailsModule
import org.koitharu.kotatsu.favourites.favouritesModule
import org.koitharu.kotatsu.history.historyModule
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.local.localModule
import org.koitharu.kotatsu.main.mainModule
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.reader.readerModule
import org.koitharu.kotatsu.remotelist.remoteListModule
import org.koitharu.kotatsu.scrobbling.shikimori.shikimoriModule
import org.koitharu.kotatsu.search.searchModule
import org.koitharu.kotatsu.settings.settingsModule
import org.koitharu.kotatsu.suggestions.suggestionsModule
import org.koitharu.kotatsu.tracker.trackerModule
import org.koitharu.kotatsu.widget.appWidgetModule

class KotatsuApp : Application() {

	override fun onCreate() {
		super.onCreate()
		if (BuildConfig.DEBUG) {
			enableStrictMode()
		}
		initKoin()
		AppCompatDelegate.setDefaultNightMode(get<AppSettings>().theme)
		setupActivityLifecycleCallbacks()
		setupDatabaseObservers()
	}

	private fun initKoin() {
		startKoin {
			androidContext(this@KotatsuApp)
			modules(
				networkModule,
				databaseModule,
				githubModule,
				uiModule,
				mainModule,
				searchModule,
				localModule,
				favouritesModule,
				historyModule,
				remoteListModule,
				detailsModule,
				trackerModule,
				settingsModule,
				readerModule,
				appWidgetModule,
				suggestionsModule,
				shikimoriModule,
				bookmarksModule,
			)
		}
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		initAcra {
			buildConfigClass = BuildConfig::class.java
			reportFormat = StringFormat.KEY_VALUE_LIST
			reportContent = listOf(
				ReportField.PACKAGE_NAME,
				ReportField.APP_VERSION_CODE,
				ReportField.APP_VERSION_NAME,
				ReportField.ANDROID_VERSION,
				ReportField.PHONE_MODEL,
				ReportField.CRASH_CONFIGURATION,
				ReportField.STACK_TRACE,
				ReportField.CUSTOM_DATA,
				ReportField.SHARED_PREFERENCES,
			)
			dialog {
				text = getString(R.string.crash_text)
				title = getString(R.string.error_occurred)
				positiveButtonText = getString(R.string.send)
				resIcon = R.drawable.ic_alert_outline
				resTheme = android.R.style.Theme_Material_Light_Dialog_Alert
			}
			mailSender {
				mailTo = getString(R.string.email_error_report)
				reportAsFile = true
				reportFileName = "stacktrace.txt"
			}
		}
	}

	private fun setupDatabaseObservers() {
		val observers = getKoin().getAll<InvalidationTracker.Observer>()
		val database = get<MangaDatabase>()
		val tracker = database.invalidationTracker
		observers.forEach {
			tracker.addObserver(it)
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		val callbacks = getKoin().getAll<ActivityLifecycleCallbacks>()
		callbacks.forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}

	private fun enableStrictMode() {
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
		FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder()
			.penaltyDeath()
			.detectFragmentReuse()
			.detectWrongFragmentContainer()
			.detectRetainInstanceUsage()
			.detectSetUserVisibleHint()
			.detectFragmentTagUsage()
			.build()
	}
}