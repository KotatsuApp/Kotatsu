package org.koitharu.kotatsu

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.strictmode.FragmentStrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.utils.ext.processLifecycleScope

@HiltAndroidApp
class KotatsuApp : Application(), Configuration.Provider {

	@Inject
	lateinit var databaseObservers: Set<@JvmSuppressWildcards InvalidationTracker.Observer>

	@Inject
	lateinit var activityLifecycleCallbacks: Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>

	@Inject
	lateinit var database: MangaDatabase

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override fun onCreate() {
		super.onCreate()
		if (BuildConfig.DEBUG) {
			enableStrictMode()
		}
		AppCompatDelegate.setDefaultNightMode(settings.theme)
		setupActivityLifecycleCallbacks()
		processLifecycleScope.launch(Dispatchers.Default) {
			setupDatabaseObservers()
		}
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		initAcra {
			buildConfigClass = BuildConfig::class.java
			reportFormat = StringFormat.JSON
			excludeMatchingSharedPreferencesKeys = listOf(
				"sources_\\w+",
			)
			httpSender {
				uri = getString(R.string.url_error_report)
				basicAuthLogin = getString(R.string.acra_login)
				basicAuthPassword = getString(R.string.acra_password)
				httpMethod = HttpSender.Method.POST
			}
			reportContent = listOf(
				ReportField.PACKAGE_NAME,
				ReportField.APP_VERSION_CODE,
				ReportField.APP_VERSION_NAME,
				ReportField.ANDROID_VERSION,
				ReportField.PHONE_MODEL,
				ReportField.STACK_TRACE,
				ReportField.CRASH_CONFIGURATION,
				ReportField.SHARED_PREFERENCES,
			)
			dialog {
				text = getString(R.string.crash_text)
				title = getString(R.string.error_occurred)
				positiveButtonText = getString(R.string.send)
				resIcon = R.drawable.ic_alert_outline
				resTheme = android.R.style.Theme_Material_Light_Dialog_Alert
			}
		}
	}

	override fun getWorkManagerConfiguration(): Configuration {
		return Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
	}

	@WorkerThread
	private fun setupDatabaseObservers() {
		val tracker = database.invalidationTracker
		databaseObservers.forEach {
			tracker.addObserver(it)
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		activityLifecycleCallbacks.forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}

	private fun enableStrictMode() {
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.penaltyLog()
				.build(),
		)
		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder()
				.detectAll()
				.setClassInstanceLimit(LocalMangaRepository::class.java, 1)
				.setClassInstanceLimit(PagesCache::class.java, 1)
				.setClassInstanceLimit(MangaLoaderContext::class.java, 1)
				.penaltyLog()
				.build(),
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
