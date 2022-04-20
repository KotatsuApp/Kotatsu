package org.koitharu.kotatsu.download.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.android.ext.android.get
import org.koin.core.context.GlobalContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseService
import org.koitharu.kotatsu.base.ui.dialog.CheckBoxAlertDialog
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.download.domain.WakeLockNode
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.connectivityManager
import org.koitharu.kotatsu.utils.ext.throttle
import org.koitharu.kotatsu.utils.progress.ProgressJob
import org.koitharu.kotatsu.utils.progress.TimeLeftEstimator
import java.util.concurrent.TimeUnit

class DownloadService : BaseService() {

	private lateinit var downloadManager: DownloadManager
	private lateinit var notificationSwitcher: ForegroundNotificationSwitcher

	private val jobs = LinkedHashMap<Int, ProgressJob<DownloadState>>()
	private val jobCount = MutableStateFlow(0)
	private val controlReceiver = ControlReceiver()
	private var binder: DownloadBinder? = null

	override fun onCreate() {
		super.onCreate()
		notificationSwitcher = ForegroundNotificationSwitcher(this)
		val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kotatsu:downloading")
		downloadManager = get<DownloadManager.Factory>().create(
			coroutineScope = lifecycleScope + WakeLockNode(wakeLock, TimeUnit.HOURS.toMillis(1)),
		)
		DownloadNotification.createChannel(this)
		registerReceiver(controlReceiver, IntentFilter(ACTION_DOWNLOAD_CANCEL))
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		val manga = intent?.getParcelableExtra<ParcelableManga>(EXTRA_MANGA)?.manga
		val chapters = intent?.getLongArrayExtra(EXTRA_CHAPTERS_IDS)
		return if (manga != null) {
			jobs[startId] = downloadManga(startId, manga, chapters)
			jobCount.value = jobs.size
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
		chaptersIds: LongArray?,
	): ProgressJob<DownloadState> {
		val job = downloadManager.downloadManga(manga, chaptersIds, startId)
		listenJob(job)
		return job
	}

	private fun listenJob(job: ProgressJob<DownloadState>) {
		lifecycleScope.launch {
			val startId = job.progressValue.startId
			val timeLeftEstimator = TimeLeftEstimator()
			val notification = DownloadNotification(this@DownloadService, startId)
			notificationSwitcher.notify(startId, notification.create(job.progressValue, -1L))
			job.progressAsFlow()
				.onEach { state ->
					if (state is DownloadState.Progress) {
						timeLeftEstimator.tick(value = state.progress, total = state.max)
					} else {
						timeLeftEstimator.emptyTick()
					}
				}
				.throttle { state -> if (state is DownloadState.Progress) 400L else 0L }
				.whileActive()
				.collect { state ->
					val timeLeft = timeLeftEstimator.getEstimatedTimeLeft()
					notificationSwitcher.notify(startId, notification.create(state, timeLeft))
				}
			job.join()
			(job.progressValue as? DownloadState.Done)?.let {
				sendBroadcast(
					Intent(ACTION_DOWNLOAD_COMPLETE)
						.putExtra(EXTRA_MANGA, ParcelableManga(it.localManga, withChapters = false))
				)
			}
			notificationSwitcher.detach(
				startId,
				if (job.isCancelled) {
					null
				} else {
					notification.create(job.progressValue, -1L)
				}
			)
			stopSelf(startId)
		}
	}

	private fun Flow<DownloadState>.whileActive(): Flow<DownloadState> = transformWhile { state ->
		emit(state)
		!state.isTerminal
	}

	private val DownloadState.isTerminal: Boolean
		get() = this is DownloadState.Done || this is DownloadState.Error || this is DownloadState.Cancelled

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

		val downloads: Flow<Collection<ProgressJob<DownloadState>>>
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
				intent.putExtra(EXTRA_MANGA, ParcelableManga(manga, withChapters = false))
				if (chaptersIds != null) {
					intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
				}
				ContextCompat.startForegroundService(context, intent)
				Toast.makeText(context, R.string.manga_downloading_, Toast.LENGTH_SHORT).show()
			}
		}

		fun start(context: Context, manga: Collection<Manga>) {
			if (manga.isEmpty()) {
				return
			}
			confirmDataTransfer(context) {
				for (item in manga) {
					val intent = Intent(context, DownloadService::class.java)
					intent.putExtra(EXTRA_MANGA, ParcelableManga(item, withChapters = false))
					ContextCompat.startForegroundService(context, intent)
				}
			}
		}

		fun confirmAndStart(context: Context, items: Set<Manga>) {
			MaterialAlertDialogBuilder(context)
				.setTitle(R.string.save_manga)
				.setMessage(R.string.batch_manga_save_confirm)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.save) { _, _ ->
					start(context, items)
				}.show()
		}

		fun getCancelIntent(startId: Int) = Intent(ACTION_DOWNLOAD_CANCEL)
			.putExtra(EXTRA_CANCEL_ID, startId)

		fun getDownloadedManga(intent: Intent?): Manga? {
			if (intent?.action == ACTION_DOWNLOAD_COMPLETE) {
				return intent.getParcelableExtra<ParcelableManga>(EXTRA_MANGA)?.manga
			}
			return null
		}

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