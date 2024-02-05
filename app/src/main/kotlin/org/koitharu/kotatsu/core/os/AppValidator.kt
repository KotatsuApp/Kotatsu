package org.koitharu.kotatsu.core.os

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValidator @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	@Suppress("NewApi")
	val isOriginalApp by lazy {
		val certificates = mapOf(CERT_SHA256.hexToByteArray() to PackageManager.CERT_INPUT_SHA256)
		PackageInfoCompat.hasSignatures(context.packageManager, context.packageName, certificates, false)
	}

	private companion object {
		private const val CERT_SHA256 = "67e15100bb809301783edcb6348fa3bbf83034d91e62868a91053dbd70db3f18"
	}
}
