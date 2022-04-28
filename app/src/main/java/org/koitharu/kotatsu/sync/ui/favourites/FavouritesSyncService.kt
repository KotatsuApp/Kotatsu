package org.koitharu.kotatsu.sync.ui.favourites

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FavouritesSyncService : Service() {

	private lateinit var syncAdapter: FavouritesSyncAdapter

	override fun onCreate() {
		super.onCreate()
		syncAdapter = FavouritesSyncAdapter(applicationContext)
	}

	override fun onBind(intent: Intent?): IBinder {
		return syncAdapter.syncAdapterBinder
	}
}