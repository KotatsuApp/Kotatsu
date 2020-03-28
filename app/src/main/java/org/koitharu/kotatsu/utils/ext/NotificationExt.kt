package org.koitharu.kotatsu.utils.ext

import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat

@SuppressLint("RestrictedApi")
fun NotificationCompat.Builder.clearActions(): NotificationCompat.Builder {
	safe {
		mActions.clear()
	}
	return this
}