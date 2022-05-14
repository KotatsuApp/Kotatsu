package org.koitharu.kotatsu.sync.ui

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SyncAuthenticatorService : Service() {

	private lateinit var authenticator: SyncAccountAuthenticator

	override fun onCreate() {
		super.onCreate()
		authenticator = SyncAccountAuthenticator(this)
	}

	override fun onBind(intent: Intent?): IBinder? {
		return authenticator.iBinder
	}
}