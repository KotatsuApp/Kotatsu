package org.koitharu.kotatsu.reader.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.findById
import org.koitharu.kotatsu.reader.data.TranslationPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationFallbackManager @Inject constructor(
	private val appSettings: AppSettings,
	private val translationPreferencesRepository: TranslationPreferencesRepository,
	private val database: MangaDatabase,
) {

	data class FallbackResult(
		val branch: String?,
		val wasFallback: Boolean,
		val fallbackReason: String? = null,
		val chapterId: Long? = null,
		val hasChapterGap: Boolean = false,
		val previousChapterNumber: Float? = null,
		val nextChapterNumber: Float? = null,
	)

	suspend fun findBestAvailableBranch(
		manga: Manga,
		currentChapterId: Long,
		direction: Int, // -1 for previous, +1 for next
		history: MangaHistory? = null,
	): FallbackResult = try {
		val chapters = manga.chapters
		if (chapters.isNullOrEmpty()) {
			FallbackResult(null, false, null, null, false, null, null)
		} else if (!appSettings.isTranslationFallbackEnabled) {
			// Fall back to original logic when feature is disabled
			val preferredBranch = manga.getPreferredBranch(history)
			FallbackResult(preferredBranch, false, null, null, false, null, null)
		} else {
			performFallbackLogic(manga, chapters, currentChapterId, direction, history)
		}
	} catch (e: Exception) {
		// If fallback logic fails, return a safe fallback result
		FallbackResult(
			branch = null,
			wasFallback = false,
			fallbackReason = "Translation fallback error",
			chapterId = null,
			hasChapterGap = false,
			previousChapterNumber = null,
			nextChapterNumber = null
		)
	}

	private suspend fun performFallbackLogic(
		manga: Manga,
		chapters: List<MangaChapter>,
		currentChapterId: Long,
		direction: Int,
		history: MangaHistory?
	): FallbackResult {

		val currentChapter = chapters.findById(currentChapterId)
		val currentBranch = currentChapter?.branch

		// Initialize default preferences if needed
		translationPreferencesRepository.initializeDefaultPreferences(manga)

		// Get user preferences for this manga - always check higher priority branches first
		val preferences = translationPreferencesRepository.getPreferences(manga.id)
		val enabledPreferences = preferences.filter { it.isEnabled }.sortedBy { it.priority }
		
		// Find the immediate next chapter (including decimals like 40.5, 41.7)

		// Check if decimal chapters should be skipped for this manga
		val mangaPrefs = database.getPreferencesDao().find(manga.id)
		val skipDecimalChapters = mangaPrefs?.skipDecimalChapters ?: false
		
		// First pass: Find the chronologically immediate next/previous chapter across all branches
		var bestNextChapter: MangaChapter? = null
		var bestNextBranch: String? = null
		
		for (preference in enabledPreferences) {
			val branchName = preference.branch
			val branchChapters = chapters.filter { it.branch == branchName }
			if (branchChapters.isNotEmpty() && currentChapter != null) {
				// Find the immediate next/previous chapter in this branch
				val candidateChapter = if (direction > 0) {
					// Find smallest chapter number > current
					val candidates = branchChapters.filter { it.number > currentChapter.number }
					// For fallback: skip decimals if setting enabled AND this is not the current branch
					if (skipDecimalChapters && branchName != currentBranch) {
						// Only consider whole number chapters for fallback to other branches
						candidates.filter { it.number % 1.0 == 0.0 }.minByOrNull { it.number }
					} else {
						// Include all chapters (including decimals) for current branch or when setting disabled
						candidates.minByOrNull { it.number }
					}
				} else {
					// Find largest chapter number < current  
					val candidates = branchChapters.filter { it.number < currentChapter.number }
					// For fallback: skip decimals if setting enabled AND this is not the current branch
					if (skipDecimalChapters && branchName != currentBranch) {
						// Only consider whole number chapters for fallback to other branches
						candidates.filter { it.number % 1.0 == 0.0 }.maxByOrNull { it.number }
					} else {
						// Include all chapters (including decimals) for current branch or when setting disabled
						candidates.maxByOrNull { it.number }
					}
				}
				
				if (candidateChapter != null) {
					// Check if this is the best (chronologically closest) candidate so far
					if (bestNextChapter == null || 
						(direction > 0 && candidateChapter.number < bestNextChapter.number) ||
						(direction < 0 && candidateChapter.number > bestNextChapter.number)) {
						bestNextChapter = candidateChapter
						bestNextBranch = branchName
					}
				}
			}
		}

		// If we found the chronologically next chapter, return it
		if (bestNextChapter != null && bestNextBranch != null) {
			val wasFallback = bestNextBranch != currentBranch
			
			// Check for chapter number gap - improved logic for decimal chapters
			val hasGap = currentChapter != null && hasSignificantGap(currentChapter.number, bestNextChapter.number)
			
			var reason: String? = null
			if (wasFallback) {
				reason = "Switched to preferred translation: $bestNextBranch"
			}
			if (hasGap && currentChapter != null) {
				val gapMessage = createGapMessage(currentChapter.number, bestNextChapter.number)
				reason = if (reason != null) "$reason • $gapMessage" else gapMessage
			}
			
			return FallbackResult(
				branch = bestNextBranch, 
				wasFallback = wasFallback, 
				fallbackReason = reason, 
				chapterId = bestNextChapter.id,
				hasChapterGap = hasGap,
				previousChapterNumber = currentChapter?.number,
				nextChapterNumber = bestNextChapter.number
			)
		}

		// Second pass: If no exact match, find any suitable continuation by priority
		for (preference in enabledPreferences) {
			val branchName = preference.branch
			val branchChapters = chapters.filter { it.branch == branchName }
			if (branchChapters.isNotEmpty()) {
				// Look for a logical continuation in this branch
				val continuationChapter = findBestContinuationChapter(chapters, currentChapterId, branchName, direction)
				if (continuationChapter != null) {
					val wasFallback = branchName != currentBranch
					
					// Check for chapter number gap
					val hasGap = currentChapter != null && hasSignificantGap(currentChapter.number, continuationChapter.number)
					
					var reason: String? = null
					if (wasFallback) {
						reason = "Switched to preferred translation: $branchName"
					}
					if (hasGap && currentChapter != null) {
						val gapMessage = createGapMessage(currentChapter.number, continuationChapter.number)
						reason = if (reason != null) "$reason • $gapMessage" else gapMessage
					}
					
					return FallbackResult(
						branch = branchName, 
						wasFallback = wasFallback, 
						fallbackReason = reason, 
						chapterId = continuationChapter.id,
						hasChapterGap = hasGap,
						previousChapterNumber = currentChapter?.number,
						nextChapterNumber = continuationChapter.number
					)
				}
			}
		}

		// If no preferences set, fall back to intelligent defaults
		val branchGroups = chapters.groupBy { it.branch }
		val fallbackBranch = branchGroups
			.filter { (key, _) -> key != currentBranch }
			.maxByOrNull { (_, value) -> value.size }?.key

		return if (fallbackBranch != null) {
			FallbackResult(
				branch = fallbackBranch,
				wasFallback = true,
				fallbackReason = "Switched to most complete translation: $fallbackBranch",
				chapterId = null,
				hasChapterGap = false,
				previousChapterNumber = currentChapter?.number,
				nextChapterNumber = null
			)
		} else {
			// Last resort: stay in current branch even if no adjacent chapters
			FallbackResult(
				branch = currentBranch,
				wasFallback = false,
				fallbackReason = null,
				chapterId = null,
				hasChapterGap = false,
				previousChapterNumber = currentChapter?.number,
				nextChapterNumber = null
			)
		}
	}

	private fun findBestContinuationChapter(
		chapters: List<MangaChapter>,
		currentChapterId: Long,
		targetBranch: String,
		direction: Int
	): MangaChapter? {
		val currentChapter = chapters.find { it.id == currentChapterId }
		val branchChapters = chapters.filter { it.branch == targetBranch }.sortedBy { it.number }
		
		if (currentChapter == null || branchChapters.isEmpty()) {
			return null
		}
		
		// For forward direction, try to find the immediate next chapter
		return if (direction > 0) {
			// Find the chapter with the smallest number > current chapter number (immediate next)
			branchChapters.filter { it.number > currentChapter.number }.minByOrNull { it.number }
				?: branchChapters.firstOrNull() // Fallback to first chapter in branch
		} else {
			// For backward direction, find the chapter with the largest number < current chapter number (immediate previous)
			branchChapters.filter { it.number < currentChapter.number }.maxByOrNull { it.number }
				?: branchChapters.lastOrNull() // Fallback to last chapter in branch
		}
	}

	private fun hasAdjacentChapterInBranch(
		chapters: List<MangaChapter>,
		currentChapterId: Long,
		direction: Int,
		branch: String?,
	): Boolean {
		// Use the same logic as findBestContinuationChapter to check if next/previous chapter exists in branch
		val continuationChapter = findBestContinuationChapter(chapters, currentChapterId, branch ?: return false, direction)
		return continuationChapter != null
	}

	fun getAllAvailableBranches(manga: Manga): List<String> {
		return manga.chapters
			?.mapNotNull { it.branch }
			?.distinct()
			?.sorted()
			?: emptyList()
	}

	fun getChapterCountByBranch(manga: Manga): Map<String?, Int> {
		return manga.chapters
			?.groupBy { it.branch }
			?.mapValues { (_, chapters) -> chapters.size }
			?: emptyMap()
	}

	suspend fun recordTranslationUsage(mangaId: Long, branch: String) {
		translationPreferencesRepository.updateLastUsed(mangaId, branch)
	}

	suspend fun getAvailableTranslationsWithPreferences(manga: Manga): List<MangaTranslationPreference> {
		val chapters = manga.chapters
		if (chapters.isNullOrEmpty()) {
			return emptyList()
		}

		// Initialize preferences if needed
		translationPreferencesRepository.initializeDefaultPreferences(manga)

		// Get existing preferences
		val preferences = translationPreferencesRepository.getPreferences(manga.id)

		// Count chapters per branch
		val chapterCounts = chapters.groupBy { it.branch }.mapValues { it.value.size }

		// Merge with preferences data
		return preferences.map { pref ->
			pref.copy(chapterCount = chapterCounts[pref.branch] ?: 0)
		}.sortedBy { it.priority }
	}

	/**
	 * Determines if there's a significant gap between two chapter numbers.
	 * This handles edge cases like:
	 * - 40.0 -> 41.0 (no gap)
	 * - 40.0 -> 42.0 (gap of 1 chapter)
	 * - 40.5 -> 41.0 (no gap, normal progression)
	 * - 40.0 -> 40.5 (no gap, normal progression)
	 * - 40.0 -> 45.0 (gap of 4 chapters)
	 */
	private fun hasSignificantGap(fromNumber: Float, toNumber: Float): Boolean {
		val difference = abs(toNumber - fromNumber)
		
		// If the difference is less than 1.0, there's no gap (handles decimals like 40.5 -> 41.0)
		if (difference < 1.0f) return false
		
		// If the difference is exactly 1.0 (or very close due to floating point), it's adjacent
		if (abs(difference - 1.0f) < 0.1f) return false
		
		// For larger differences, consider it a gap if we're skipping whole numbers
		// e.g., 40.0 -> 42.0 is a gap (missing 41), but 40.9 -> 42.0 might not be
		val wholeDifference = floor(difference)
		return wholeDifference >= 1.0
	}

	/**
	 * Creates a user-friendly gap message for missing chapters
	 */
	private fun createGapMessage(fromNumber: Float, toNumber: Float): String {
		val startMissing = kotlin.math.ceil(fromNumber.toDouble()).toInt()
		val endMissing = kotlin.math.floor(toNumber.toDouble()).toInt() - 1
		
		return if (startMissing == endMissing) {
			"Skipped chapter $startMissing"
		} else {
			"Skipped chapters ($startMissing to $endMissing)"
		}
	}
}