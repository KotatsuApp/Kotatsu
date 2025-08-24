package org.koitharu.kotatsu.reader.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.TranslationPreferencesEntity
import org.koitharu.kotatsu.core.util.ext.mapToSet
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationPreferencesRepository @Inject constructor(
	private val database: MangaDatabase,
) {
	private val dao = database.getTranslationPreferencesDao()

	suspend fun getPreferences(mangaId: Long): List<MangaTranslationPreference> {
		return dao.getPreferences(mangaId).map { entity ->
			MangaTranslationPreference(
				branch = entity.branch,
				priority = entity.priority,
				isEnabled = entity.isEnabled,
				lastUsed = entity.lastUsed,
			)
		}
	}

	fun observePreferences(mangaId: Long): Flow<List<MangaTranslationPreference>> {
		return dao.observePreferences(mangaId).map { entities ->
			entities.map { entity ->
				MangaTranslationPreference(
					branch = entity.branch,
					priority = entity.priority,
					isEnabled = entity.isEnabled,
					lastUsed = entity.lastUsed,
				)
			}
		}
	}

	suspend fun setPreferences(mangaId: Long, preferences: List<MangaTranslationPreference>) {
		val entities = preferences.map { pref ->
			TranslationPreferencesEntity(
				mangaId = mangaId,
				branch = pref.branch,
				priority = pref.priority,
				isEnabled = pref.isEnabled,
				lastUsed = pref.lastUsed,
			)
		}
		dao.insertOrReplace(entities)
	}

	suspend fun updateLastUsed(mangaId: Long, branch: String, timestamp: Long = System.currentTimeMillis()) {
		dao.updateLastUsed(mangaId, branch, timestamp)
	}

	suspend fun updatePriority(mangaId: Long, branch: String, priority: Int) {
		dao.updatePriority(mangaId, branch, priority)
	}

	suspend fun initializeDefaultPreferences(manga: Manga) {
		val mangaId = manga.id
		val existingPrefs = dao.getPreferences(mangaId)
		val existingBranches = existingPrefs.map { it.branch }.toSet()
		
		val chapters = manga.chapters
		if (!chapters.isNullOrEmpty()) {
			// Get all available branches from chapters
			val allBranches = chapters
				.mapNotNull { it.branch }
				.distinct() // Preserve original order, remove duplicates
			
			// Find missing branches that don't have preferences yet
			val missingBranches = allBranches.filter { it !in existingBranches }
			
			if (missingBranches.isNotEmpty()) {
				// Create preferences for missing branches, starting priority after existing ones
				val maxExistingPriority = existingPrefs.maxOfOrNull { it.priority } ?: -1
				val newPreferences = missingBranches.mapIndexed { index, branch ->
					TranslationPreferencesEntity(
						mangaId = mangaId,
						branch = branch,
						priority = maxExistingPriority + 1 + index,
						isEnabled = true,
						lastUsed = null,
					)
				}
				dao.insertOrReplace(newPreferences)
			}
		}
	}

	suspend fun getOrderedBranches(mangaId: Long): List<String> {
		return dao.getPreferences(mangaId).map { it.branch }
	}

	suspend fun hasCustomPreferences(mangaId: Long): Boolean {
		return dao.getPreferencesCount(mangaId) > 0
	}

	suspend fun deletePreferences(mangaId: Long) {
		dao.deleteByMangaId(mangaId)
	}
}