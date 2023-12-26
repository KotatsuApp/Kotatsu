package org.koitharu.kotatsu.settings.backup

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

data class BackupEntryModel(
	val name: BackupEntry.Name,
	val isChecked: Boolean,
	val isEnabled: Boolean,
) : ListModel {

	@get:StringRes
	val titleResId: Int
		get() = when (name) {
			BackupEntry.Name.INDEX -> 0 // should not appear here
			BackupEntry.Name.HISTORY -> R.string.history
			BackupEntry.Name.CATEGORIES -> R.string.favourites_categories
			BackupEntry.Name.FAVOURITES -> R.string.favourites
			BackupEntry.Name.SETTINGS -> R.string.settings
			BackupEntry.Name.BOOKMARKS -> R.string.bookmarks
			BackupEntry.Name.SOURCES -> R.string.remote_sources
		}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is BackupEntryModel && other.name == name
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		if (previousState !is BackupEntryModel) {
			return null
		}
		return if (previousState.isEnabled != isEnabled) {
			ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
		} else if (previousState.isChecked != isChecked) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
