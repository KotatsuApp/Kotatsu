package org.koitharu.kotatsu.core.os

import android.content.Intent
import android.os.Build
import android.provider.Settings

@Suppress("FunctionName")
fun NetworkManageIntent(): Intent {
	val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		Settings.Panel.ACTION_INTERNET_CONNECTIVITY
	} else {
		Settings.ACTION_WIRELESS_SETTINGS
	}
	return Intent(action)
}
