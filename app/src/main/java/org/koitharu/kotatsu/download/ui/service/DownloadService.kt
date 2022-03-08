package org.koitharu.kotatsu.download.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.core.context.GlobalContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseService
import org.koitharu.kotatsu.base.ui.dialog.CheckBoxAlertDialog
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.utils.ext.connectivityManager
import org.koitharu.kotatsu.utils.ext.toArraySet
import org.koitharu.kotatsu.utils.progress.ProgressJob
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class DownloadService : BaseService() {

	private lateinit var notificationManager: NotificationManagerCompat
	private lateinit var wakeLock: PowerManager.WakeLock
	private lateinit var downloadManager: DownloadManager

	private val jobs = LinkedHashMap<Int, ProgressJob<DownloadManager.State>>()
	private val jobCount = MutableStateFlow(0)
	private val mutex = Mutex()
	private val controlReceiver = ControlReceiver()
	private var binder: DownloadBinder? = null

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(this)
		wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kotatsu:downloading")
		downloadManager = DownloadManager(this, get(), get(), get(), get())
		DownloadNotification.createChannel(this)
		registerReceiver(controlReceiver, IntentFilter(ACTION_DOWNLOAD_CANCEL))
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		val manga = intent?.getParcelableExtra<Manga>(EXTRA_MANGA)
		val chapters = intent?.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toArraySet()
		return if (manga != null) {
			jobs[startId] = downloadManga(startId, manga, chapters)
			jobCount.value = jobs.size
			Toast.makeText(this, R.string.manga_downloading_, Toast.LENGTH_SHORT).show()
			START_REDELIVER_INTENT
		} else {
			stopSelf(startId)
			START_NOT_STICKY
		}
	}

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		return binder ?: DownloadBinder(this).also { binder = it }
	}

	override fun onUnbind(intent: Intent?): Boolean {
		binder = null
		return super.onUnbind(intent)
	}

	override fun onDestroy() {
		unregisterReceiver(controlReceiver)
		binder = null
		super.onDestroy()
	}

	private fun downloadManga(
		startId: Int,
		manga: Manga,
		chaptersIds: Set<Long>?,
	): ProgressJob<DownloadManager.State> {
		val initialState = DownloadManager.State.Queued(startId, manga, null)
		val stateFlow = MutableStateFlow<DownloadManager.State>(initialState)
		val job = lifecycleScope.launch {
			mutex.withLock {
				wakeLock.acquire(TimeUnit.HOURS.toMillis(1))
				val notification = DownloadNotification(this@DownloadService, startId)
				startForeground(startId, notification.create(initialState))
				try {
					withContext(Dispatchers.Default) {
						downloadManager.downloadManga(manga, chaptersIds, startId)
							.collect { state ->
								stateFlow.value = state
								notificationManager.notify(startId, notification.create(state))
							}
					}
					if (stateFlow.value is DownloadManager.State.Done) {
						sendBroadcast(
							Intent(ACTION_DOWNLOAD_COMPLETE)
								.putExtra(EXTRA_MANGA, manga)
						)
					}
				} finally {
					ServiceCompat.stopForeground(
						this@DownloadService,
						if (isActive) {
							ServiceCompat.STOP_FOREGROUND_DETACH
						} else {
							ServiceCompat.STOP_FOREGROUND_REMOVE
						}
					)
					if (wakeLock.isHeld) {
						wakeLock.release()
					}
					stopSelf(startId)
				}
			}
		}
		return ProgressJob(job, stateFlow)
	}

	inner class ControlReceiver : BroadcastReceiver() {

		override fun onReceive(context: Context, intent: Intent?) {
			when (intent?.action) {
				ACTION_DOWNLOAD_CANCEL -> {
					val cancelId = intent.getIntExtra(EXTRA_CANCEL_ID, 0)
					jobs.remove(cancelId)?.cancel()
					jobCount.value = jobs.size
				}
			}
		}
	}

	class DownloadBinder(private val service: DownloadService) : Binder() {

		val downloads: Flow<Collection<ProgressJob<DownloadManager.State>>>
			get() = service.jobCount.mapLatest { service.jobs.values }
	}

	companion object {

		const val ACTION_DOWNLOAD_COMPLETE =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_COMPLETE"

		private const val ACTION_DOWNLOAD_CANCEL =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_CANCEL"

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"
		private const val EXTRA_CANCEL_ID = "cancel_id"

		fun start(context: Context, manga: Manga, chaptersIds: Collection<Long>? = null) {
			if (chaptersIds?.isEmpty() == true) {
				return
			}
			confirmDataTransfer(context) {
				val intent = Intent(context, DownloadService::class.java)
				intent.putExtra(EXTRA_MANGA, manga)
				if (chaptersIds != null) {
					intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
				}
				ContextCompat.startForegroundService(context, intent)
			}
		}

		fun getCancelIntent(startId: Int) = Intent(ACTION_DOWNLOAD_CANCEL)
			.putExtra(ACTION_DOWNLOAD_CANCEL, startId)

		private fun confirmDataTransfer(context: Context, callback: () -> Unit) {
			val settings = GlobalContext.get().get<AppSettings>()
			if (context.connectivityManager.isActiveNetworkMetered && settings.isTrafficWarningEnabled) {
				CheckBoxAlertDialog.Builder(context)
					.setTitle(R.string.warning)
					.setMessage(R.string.network_consumption_warning)
					.setCheckBoxText(R.string.dont_ask_again)
					.setCheckBoxChecked(false)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string._continue) { _, doNotAsk ->
						settings.isTrafficWarningEnabled = !doNotAsk
						callback()
					}.create()
					.show()
			} else {
				callback()
			}
		}
	}
}