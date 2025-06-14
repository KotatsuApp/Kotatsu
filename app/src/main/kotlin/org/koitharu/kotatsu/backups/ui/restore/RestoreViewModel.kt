package org.koitharu.kotatsu.backups.ui.restore

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koitharu.kotatsu.backups.data.model.BackupIndex
import org.koitharu.kotatsu.backups.domain.BackupSection
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Date
import java.util.EnumMap
import java.util.EnumSet
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class RestoreViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val uri = savedStateHandle.get<String>(AppRouter.KEY_FILE)?.toUriOrNull()
	private val contentResolver = context.contentResolver

	val availableEntries = MutableStateFlow<List<BackupSectionModel>>(emptyList())
	val backupDate = MutableStateFlow<Date?>(null)

	init {
		launchLoadingJob(Dispatchers.Default) {
			loadBackupInfo()
		}
	}

	private suspend fun loadBackupInfo() {
		val sections = runInterruptible(Dispatchers.IO) {
			if (uri == null) throw FileNotFoundException()
			ZipInputStream(contentResolver.openInputStream(uri)).use { stream ->
				val result = EnumSet.noneOf(BackupSection::class.java)
				var entry = stream.nextEntry
				while (entry != null) {
					val s = BackupSection.of(entry)
					if (s != null) {
						result.add(s)
						if (s == BackupSection.INDEX) {
							backupDate.value = stream.readDate()
						}
					}
					stream.closeEntry()
					entry = stream.nextEntry
				}
				result
			}
		}
		availableEntries.value = BackupSection.entries.mapNotNull { entry ->
			if (entry == BackupSection.INDEX || entry !in sections) {
				return@mapNotNull null
			}
			BackupSectionModel(
				section = entry,
				isChecked = true,
				isEnabled = true,
			)
		}
	}

	fun onItemClick(item: BackupSectionModel) {
		val map = availableEntries.value.associateByTo(EnumMap(BackupSection::class.java)) { it.section }
		map[item.section] = item.copy(isChecked = !item.isChecked)
		map.validate()
		availableEntries.value = map.values.sortedBy { it.section.ordinal }
	}

	fun getCheckedSections(): Set<BackupSection> = availableEntries.value
		.mapNotNullTo(EnumSet.noneOf(BackupSection::class.java)) {
			if (it.isChecked) it.section else null
		}

	/**
	 * Check for inconsistent user selection
	 * Favorites cannot be restored without categories
	 */
	private fun MutableMap<BackupSection, BackupSectionModel>.validate() {
		val favorites = this[BackupSection.FAVOURITES] ?: return
		val categories = this[BackupSection.CATEGORIES]
		if (categories?.isChecked == true) {
			if (!favorites.isEnabled) {
				this[BackupSection.FAVOURITES] = favorites.copy(isEnabled = true)
			}
		} else {
			if (favorites.isEnabled) {
				this[BackupSection.FAVOURITES] = favorites.copy(isEnabled = false, isChecked = false)
			}
		}
	}

	private fun InputStream.readDate(): Date? = runCatching {
		val index = Json.decodeFromStream<List<BackupIndex>>(this)
		Date(index.single().createdAt)
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrNull()
}
