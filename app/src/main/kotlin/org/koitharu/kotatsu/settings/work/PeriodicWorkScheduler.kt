package org.koitharu.kotatsu.settings.work

import android.content.Context

interface PeriodicWorkScheduler {

	suspend fun schedule(context: Context)

	suspend fun unschedule(context: Context)

	suspend fun isScheduled(context: Context): Boolean
}
