package org.koitharu.kotatsu.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.get
import org.koin.core.context.GlobalContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseService
import org.koitharu.kotatsu.base.ui.dialog.CheckBoxAlertDialog
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.utils.LiveStateFlow
import org.koitharu.kotatsu.utils.ext.toArraySet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class DownloadService : BaseService() {

	private lateinit var notificationManager: NotificationManagerCompat
	private lateinit var wakeLock: PowerManager.WakeLock
	private lateinit var downloadManager: DownloadManager
	private lateinit var dispatcher: ExecutorCoroutineDispatcher

	private val jobs = HashMap<Int, LiveStateFlow<DownloadManager.State>>()
	private val mutex = Mutex()

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(this)
		wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kotatsu:downloading")
		downloadManager = DownloadManager(this, get(), get(), get(), get(), get())
		dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
		DownloadNotification.createChannel(this)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		when (intent?.action) {
			ACTION_DOWNLOAD_START -> {
				val manga = intent.getParcelableExtra<Manga>(EXTRA_MANGA)
				val chapters = intent.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toArraySet()
				if (manga != null) {
					jobs[startId] = downloadManga(startId, manga, chapters)
					Toast.makeText(this, R.string.manga_downloading_, Toast.LENGTH_SHORT).show()
					return START_REDELIVER_INTENT
				} else {
					stopSelf(startId)
				}
			}
			ACTION_DOWNLOAD_CANCEL -> {
				val cancelId = intent.getIntExtra(EXTRA_CANCEL_ID, 0)
				jobs.remove(cancelId)?.cancel()
				stopSelf(startId)
			}
			else -> stopSelf(startId)
		}
		return START_NOT_STICKY
	}

	override fun onDestroy() {
		super.onDestroy()
		dispatcher.close()
	}

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		return DownloadBinder()
	}

	private fun downloadManga(
		startId: Int,
		manga: Manga,
		chaptersIds: Set<Long>?,
	): LiveStateFlow<DownloadManager.State> {
		val initialState = DownloadManager.State.Queued(startId, manga, null)
		val stateFlow = MutableStateFlow<DownloadManager.State>(initialState)
		val job = lifecycleScope.launch {
			mutex.withLock {
				wakeLock.acquire(TimeUnit.HOURS.toMillis(1))
				val notification = DownloadNotification(this@DownloadService, startId)
				startForeground(startId, notification.create(initialState))
				try {
					withContext(dispatcher) {
						downloadManager.downloadManga(manga, chaptersIds, startId)
							.collect { state ->
								stateFlow.value = state
								notificationManager.notify(startId, notification.create(state))
							}
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
		return LiveStateFlow(stateFlow, job)
	}

	inner class DownloadBinder : Binder() {

		val downloads: Collection<LiveStateFlow<DownloadManager.State>>
			get() = jobs.values
	}

	companion object {

		private const val ACTION_DOWNLOAD_START =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_START"
		private const val ACTION_DOWNLOAD_CANCEL =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_CANCEL"

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"
		private const val EXTRA_CANCEL_ID = "cancel_id"

		fun start(context: Context, manga: Manga, chaptersIds: Collection<Long>? = null) {
			confirmDataTransfer(context) {
				val intent = Intent(context, DownloadService::class.java)
				intent.action = ACTION_DOWNLOAD_START
				intent.putExtra(EXTRA_MANGA, manga)
				if (chaptersIds != null) {
					intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
				}
				ContextCompat.startForegroundService(context, intent)
			}
		}

		fun getCancelIntent(context: Context, startId: Int) =
			Intent(context, DownloadService::class.java)
				.setAction(ACTION_DOWNLOAD_CANCEL)
				.putExtra(ACTION_DOWNLOAD_CANCEL, startId)

		private fun confirmDataTransfer(context: Context, callback: () -> Unit) {
			val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			val settings = GlobalContext.get().get<AppSettings>()
			if (cm.isActiveNetworkMetered && settings.isTrafficWarningEnabled) {
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