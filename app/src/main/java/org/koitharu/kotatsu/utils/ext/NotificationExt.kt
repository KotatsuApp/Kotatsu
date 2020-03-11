package org.koitharu.kotatsu.utils.ext

import androidx.core.app.NotificationCompat

fun NotificationCompat.Builder.clearActions(): NotificationCompat.Builder {
	mActions.clear()
	return this
}