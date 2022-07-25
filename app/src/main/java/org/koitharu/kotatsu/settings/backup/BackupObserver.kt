package org.koitharu.kotatsu.settings.backup

import android.app.backup.BackupManager
import android.content.Context
import androidx.room.InvalidationTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY

@Singleton
class BackupObserver @Inject constructor(
	@ApplicationContext context: Context,
) : InvalidationTracker.Observer(arrayOf(TABLE_HISTORY, TABLE_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)) {

	private val backupManager = BackupManager(context)

	override fun onInvalidated(tables: MutableSet<String>) {
		backupManager.dataChanged()
	}
}
