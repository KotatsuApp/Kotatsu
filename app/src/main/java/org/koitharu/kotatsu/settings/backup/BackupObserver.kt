package org.koitharu.kotatsu.settings.backup

import android.app.backup.BackupManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.InvalidationTracker
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY

@RequiresApi(Build.VERSION_CODES.M)
class BackupObserver(
	context: Context,
) : InvalidationTracker.Observer(arrayOf(TABLE_HISTORY, TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)) {

	private val backupManager = BackupManager(context)

	override fun onInvalidated(tables: MutableSet<String>) {
		backupManager.dataChanged()
	}
}