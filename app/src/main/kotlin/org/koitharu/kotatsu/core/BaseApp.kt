package org.koitharu.kotatsu.core

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.conscrypt.Conscrypt
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.os.AppValidator
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.WorkServiceStopHelper
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.settings.work.WorkScheduleManager
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
open class BaseApp : Application(), Configuration.Provider {

	@Inject
	lateinit var databaseObserversProvider: Provider<Set<@JvmSuppressWildcards InvalidationTracker.Observer>>

	@Inject
	lateinit var activityLifecycleCallbacks: Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>

	@Inject
	lateinit var database: Provider<MangaDatabase>

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var appValidator: AppValidator

	@Inject
	lateinit var workScheduleManager: WorkScheduleManager

	@Inject
	lateinit var workManagerProvider: Provider<WorkManager>

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		AppCompatDelegate.setDefaultNightMode(settings.theme)
		AppCompatDelegate.setApplicationLocales(settings.appLocales)
		// TLS 1.3 support for Android < 10
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			Security.insertProviderAt(Conscrypt.newProvider(), 1)
		}
		setupActivityLifecycleCallbacks()
		processLifecycleScope.launch {
			val isOriginalApp = withContext(Dispatchers.Default) {
				appValidator.isOriginalApp
			}
			ACRA.errorReporter.putCustomData("isOriginalApp", isOriginalApp.toString())
		}
		processLifecycleScope.launch(Dispatchers.Default) {
			setupDatabaseObservers()
		}
		workScheduleManager.init()
		WorkServiceStopHelper(workManagerProvider).setup()
	}

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		initAcra {
			buildConfigClass = BuildConfig::class.java
			reportFormat = StringFormat.JSON
			httpSender {
				uri = getString(R.string.url_error_report)
				basicAuthLogin = getString(R.string.acra_login)
				basicAuthPassword = getString(R.string.acra_password)
				httpMethod = HttpSender.Method.POST
			}
			reportContent = listOf(
				ReportField.PACKAGE_NAME,
				ReportField.INSTALLATION_ID,
				ReportField.APP_VERSION_CODE,
				ReportField.APP_VERSION_NAME,
				ReportField.ANDROID_VERSION,
				ReportField.PHONE_MODEL,
				ReportField.STACK_TRACE,
				ReportField.CRASH_CONFIGURATION,
				ReportField.CUSTOM_DATA,
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

	@WorkerThread
	private fun setupDatabaseObservers() {
		val tracker = database.get().invalidationTracker
		databaseObserversProvider.get().forEach {
			tracker.addObserver(it)
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		activityLifecycleCallbacks.forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}
}
