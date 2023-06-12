package org.koitharu.kotatsu.sync.ui.history

import android.app.Service
import android.content.Intent
import android.os.IBinder

class HistorySyncService : Service() {

	private lateinit var syncAdapter: HistorySyncAdapter

	override fun onCreate() {
		super.onCreate()
		syncAdapter = HistorySyncAdapter(applicationContext)
	}

	override fun onBind(intent: Intent?): IBinder {
		return syncAdapter.syncAdapterBinder
	}
}