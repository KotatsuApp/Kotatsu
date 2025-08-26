package org.koitharu.kotatsu.reader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.toMangaChapters
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.reader.data.TranslationPreferencesRepository
import org.koitharu.kotatsu.reader.domain.AutoTranslationConfigManager
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference
import org.koitharu.kotatsu.reader.domain.TranslationFallbackManager
import org.koitharu.kotatsu.core.util.LanguageDetectionUtils
import org.koitharu.kotatsu.parsers.model.Manga
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class TranslationSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val translationPreferencesRepository: TranslationPreferencesRepository,
	private val translationFallbackManager: TranslationFallbackManager,
	private val appSettings: AppSettings,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val database: MangaDatabase,
	private val autoTranslationConfigManager: AutoTranslationConfigManager,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	val manga = intent.manga ?: throw IllegalArgumentException("Manga is required")
	
	companion object {
		private const val TAG = "TranslationSettingsVM"
	}

	private val _preferences = MutableStateFlow<List<MangaTranslationPreference>>(emptyList())
	val preferences = _preferences.asStateFlow()

	private val _skipDecimalChapters = MutableStateFlow(false)
	val skipDecimalChapters = _skipDecimalChapters.asStateFlow()

	val isGlobalFallbackEnabled = appSettings.isTranslationFallbackEnabled

	init {
		Log.d(TAG, "DEBUG: TranslationSettingsViewModel init() - Starting initialization")
		Log.d(TAG, "DEBUG: Initial manga state - id=${manga.id}, title='${manga.title}', source=${manga.source}")
		Log.d(TAG, "DEBUG: Initial manga chapters count: ${manga.chapters?.size ?: 0}")
		Log.d(TAG, "DEBUG: Initial manga chapters branches: ${manga.chapters?.mapNotNull { it.branch }?.distinct() ?: emptyList()}")
		loadPreferences()
		loadSkipDecimalChapters()
		Log.d(TAG, "DEBUG: TranslationSettingsViewModel init() - Initialization completed")
	}

	private fun loadPreferences() {
		launchJob {
			Log.d(TAG, "DEBUG: loadPreferences() started")
			Log.d(TAG, "DEBUG: manga.id = ${manga.id}")
			Log.d(TAG, "DEBUG: manga.title = ${manga.title}")
			Log.d(TAG, "DEBUG: manga.source = ${manga.source}")
			
			// Check if manga already has chapters
			val hasChapters = manga.chapters?.isNotEmpty() == true
			Log.d(TAG, "DEBUG: manga.chapters size = ${manga.chapters?.size ?: 0}")
			Log.d(TAG, "DEBUG: hasChapters = $hasChapters")
			
			if (hasChapters) {
				// Fast path: manga already has chapters, use them directly
				Log.d(TAG, "DEBUG: Using fast path - manga already has chapters")
				val branches = manga.chapters?.mapNotNull { it.branch }?.distinct() ?: emptyList()
				Log.d(TAG, "DEBUG: Available branches from existing chapters: $branches")
				
				// Generate preferences with built-in default language logic (avoiding potential database issues)
				Log.d(TAG, "DEBUG: Generating preferences with built-in default language logic")
				val prefs = generatePreferencesFromChapters(manga)
				Log.d(TAG, "DEBUG: Generated ${prefs.size} preferences with default language logic applied")
				prefs.forEach { pref ->
					Log.d(TAG, "DEBUG: Fast Path Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
				}
				_preferences.value = prefs
			} else {
				// Manga has no chapters (common when coming from details via ParcelableManga)
				Log.d(TAG, "DEBUG: Manga has no chapters, trying fallback strategies")
				
				// Strategy 1: Try loading from database first
				Log.d(TAG, "DEBUG: Strategy 1 - Attempting to load cached chapters from database")
				loadMangaWithCachedChapters()?.let { mangaWithCachedChapters ->
					Log.d(TAG, "DEBUG: Strategy 1 SUCCESS - Found cached chapters: ${mangaWithCachedChapters.chapters?.size ?: 0}")
					val branches = mangaWithCachedChapters.chapters?.mapNotNull { it.branch }?.distinct() ?: emptyList()
					Log.d(TAG, "DEBUG: Cached branches: $branches")
					
					// Generate preferences with built-in default language logic
					Log.d(TAG, "DEBUG: Generating preferences manually from cached chapters with default language logic")
					val finalPrefs = generatePreferencesFromChapters(mangaWithCachedChapters)
					Log.d(TAG, "DEBUG: Final ${finalPrefs.size} preferences with default language logic applied:")
					finalPrefs.forEach { pref ->
						Log.d(TAG, "DEBUG: Final Cached Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
					}
					_preferences.value = finalPrefs
					return@launchJob
				}
				Log.d(TAG, "DEBUG: Strategy 1 FAILED - No cached chapters found")
				
				// Strategy 2: Use DetailsLoadUseCase with force=true (same as ReaderViewModel)
				Log.d(TAG, "DEBUG: Strategy 2 - Using DetailsLoadUseCase with force=true like ReaderViewModel")
				try {
					// Create manga-only intent to force proper database loading (like ReaderViewModel does)
					val mangaOnlyIntent = MangaIntent.of(manga)
					Log.d(TAG, "DEBUG: Created manga-only intent for proper DetailsLoadUseCase flow")
					
					// Use firstOrNull to get the loaded result and break out of collect
					val loadedDetails = detailsLoadUseCase(mangaOnlyIntent, force = true)
						.first { it.isLoaded }
					
					Log.d(TAG, "DEBUG: DetailsLoadUseCase completed - isLoaded=${loadedDetails.isLoaded}, chapters=${loadedDetails.allChapters.size}")
					
					if (loadedDetails.allChapters.isNotEmpty()) {
						val branches = loadedDetails.allChapters.mapNotNull { it.branch }.distinct()
						Log.d(TAG, "DEBUG: Strategy 2 SUCCESS - DetailsLoadUseCase branches: $branches")
						
						// Generate preferences with built-in default language logic
						val fullManga = loadedDetails.toManga()
						Log.d(TAG, "DEBUG: Generating preferences manually with built-in default language logic")
						val finalPrefs = generatePreferencesFromChapters(fullManga)
						Log.d(TAG, "DEBUG: Final ${finalPrefs.size} preferences with default language logic applied:")
						finalPrefs.forEach { pref ->
							Log.d(TAG, "DEBUG: Final Network Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
						}
						_preferences.value = finalPrefs
						return@launchJob
					} else {
						Log.d(TAG, "DEBUG: Strategy 2 FAILED - DetailsLoadUseCase returned no chapters")
					}
				} catch (e: Exception) {
					// DetailsLoadUseCase failed, fall through to empty state
					Log.e(TAG, "DEBUG: Strategy 2 EXCEPTION - DetailsLoadUseCase failed: ${e.message}", e)
				}
				
				// Strategy 3: If all else fails, show empty list
				Log.d(TAG, "DEBUG: Strategy 3 - All strategies failed, showing empty list")
				_preferences.value = emptyList()
			}
			Log.d(TAG, "DEBUG: loadPreferences() completed")
		}
	}

	fun updatePreferencesOrder(newOrder: List<MangaTranslationPreference>) {
		viewModelScope.launch {
			// Update priorities based on new order
			val updatedPreferences = newOrder.mapIndexed { index, preference ->
				preference.copy(priority = index)
			}
			_preferences.value = updatedPreferences
			
			// Save to database
			translationPreferencesRepository.setPreferences(manga.id, updatedPreferences)
		}
	}

	fun togglePreferenceEnabled(preference: MangaTranslationPreference, enabled: Boolean) {
		viewModelScope.launch {
			val updatedPreferences = _preferences.value.map { pref ->
				if (pref.branch == preference.branch) {
					pref.copy(isEnabled = enabled)
				} else {
					pref
				}
			}
			_preferences.value = updatedPreferences
			
			// Save to database
			translationPreferencesRepository.setPreferences(manga.id, updatedPreferences)
		}
	}

	fun resetToDefaults() {
		launchJob {
			// Delete all preferences for this manga and reload
			translationPreferencesRepository.deletePreferences(manga.id)
			loadPreferences()
		}
	}

	/**
	 * Apply default language preferences based on global settings
	 */
	fun applyDefaultLanguages() {
		launchJob {
			val defaultLanguages = appSettings.defaultTranslationLanguages
			if (defaultLanguages.isEmpty()) return@launchJob
			
			val currentPreferences = _preferences.value
			val updatedPreferences = currentPreferences.map { preference ->
				val branchLanguage = LanguageDetectionUtils.detectLanguageFromBranch(preference.branch)
				val shouldBeEnabled = branchLanguage != null && branchLanguage in defaultLanguages
				
				preference.copy(isEnabled = shouldBeEnabled)
			}.sortedWith { a, b ->
				// Sort enabled preferences first, then by priority
				when {
					a.isEnabled && !b.isEnabled -> -1
					!a.isEnabled && b.isEnabled -> 1
					else -> a.priority.compareTo(b.priority)
				}
			}.mapIndexed { index, preference ->
				preference.copy(priority = index)
			}
			
			_preferences.value = updatedPreferences
			translationPreferencesRepository.setPreferences(manga.id, updatedPreferences)
		}
	}

	/**
	 * Check if preferences should be auto-configured
	 * Now applies every time settings are opened if global language settings are available
	 */
	fun shouldAutoApplyDefaults(): Boolean {
		val defaultLanguages = appSettings.defaultTranslationLanguages
		
		// Auto-apply if user has set default languages in global settings
		// This will happen every time they open translation settings
		return defaultLanguages.isNotEmpty()
	}

	/**
	 * Check if current preferences match the global default languages
	 */
	private fun preferencesMatchDefaults(): Boolean {
		val preferences = _preferences.value
		val defaultLanguages = appSettings.defaultTranslationLanguages
		
		if (defaultLanguages.isEmpty()) return false
		
		// Check if enabled preferences match default languages
		val enabledLanguages = preferences
			.filter { it.isEnabled }
			.mapNotNull { LanguageDetectionUtils.detectLanguageFromBranch(it.branch) }
			.toSet()
			
		return enabledLanguages == defaultLanguages
	}

	private fun loadSkipDecimalChapters() {
		launchJob {
			val prefs = runCatching { database.getPreferencesDao().find(manga.id) }.getOrNull()
			_skipDecimalChapters.value = prefs?.skipDecimalChapters ?: false
		}
	}

	fun setSkipDecimalChapters(skip: Boolean) {
		viewModelScope.launch {
			val existingPrefs = runCatching { 
				database.getPreferencesDao().find(manga.id) 
			}.getOrNull() ?: newMangaPrefsEntity(manga.id)
			
			runCatching {
				database.getPreferencesDao().upsert(existingPrefs.copy(skipDecimalChapters = skip))
			}
			_skipDecimalChapters.value = skip
		}
	}

	/**
	 * Generate translation preferences directly from chapters without database dependencies
	 * This avoids foreign key constraint issues when manga isn't properly stored yet
	 */
	private suspend fun generatePreferencesFromChapters(manga: Manga): List<MangaTranslationPreference> {
		val chapters = manga.chapters
		if (chapters.isNullOrEmpty()) {
			Log.d(TAG, "DEBUG: generatePreferencesFromChapters - No chapters available")
			return emptyList()
		}

		// Group chapters by branch and count them
		val branchCounts = chapters.groupBy { it.branch }.mapValues { it.value.size }
		Log.d(TAG, "DEBUG: generatePreferencesFromChapters - Branch counts: $branchCounts")

		// Try to get existing preferences from database (might fail due to foreign key issues)
		val existingPrefs = try {
			translationPreferencesRepository.getPreferences(manga.id)
		} catch (e: Exception) {
			Log.d(TAG, "DEBUG: generatePreferencesFromChapters - Failed to load existing prefs, using defaults: ${e.message}")
			emptyList()
		}

		// Get default languages and apply the same logic as AutoTranslationConfigManager
		val defaultLanguages = appSettings.defaultTranslationLanguages
		Log.d(TAG, "DEBUG: generatePreferencesFromChapters - Default languages: $defaultLanguages")
		
		// Create preferences for each branch with proper priority assignment
		val sortedBranches = branchCounts.keys.sortedWith(compareBy<String?> { it == null }.thenBy { it }) // null last, then alphabetical
		val preferences = sortedBranches.mapIndexed { index, branch ->
			// Check if we have existing preference for this branch
			val existingPref = existingPrefs.find { it.branch == branch }
			
			// Detect language for this branch using the same logic as AutoTranslationConfigManager
			val branchLanguage = LanguageDetectionUtils.detectLanguageFromBranch(branch ?: "")
			val shouldBeEnabled = if (defaultLanguages.isNotEmpty()) {
				// Apply default language logic: enable only if language matches default languages
				branchLanguage != null && branchLanguage in defaultLanguages
			} else {
				// No default languages set, use existing preference or default to enabled
				existingPref?.isEnabled ?: true
			}
			
			Log.d(TAG, "DEBUG: Branch '$branch' -> language='$branchLanguage', shouldBeEnabled=$shouldBeEnabled")
			
			MangaTranslationPreference(
				branch = branch ?: "",
				priority = existingPref?.priority ?: index, // Use index for initial ordering
				isEnabled = shouldBeEnabled,
				lastUsed = existingPref?.lastUsed,
				chapterCount = branchCounts[branch] ?: 0
			)
		}
		
		// Apply the same sorting logic as AutoTranslationConfigManager:
		// Sort enabled preferences first, then by priority, then update priorities
		val sortedPrefs = preferences.sortedWith { a, b ->
			when {
				a.isEnabled && !b.isEnabled -> -1
				!a.isEnabled && b.isEnabled -> 1
				else -> a.priority.compareTo(b.priority)
			}
		}.mapIndexed { index, preference ->
			preference.copy(priority = index)
		}

		Log.d(TAG, "DEBUG: generatePreferencesFromChapters - Created ${sortedPrefs.size} preferences with proper sorting")
		return sortedPrefs
	}

	/**
	 * Attempts to load manga with chapters from cached database chapters
	 * Returns null if no cached chapters are available
	 */
	private suspend fun loadMangaWithCachedChapters(): Manga? {
		return try {
			Log.d(TAG, "DEBUG: loadMangaWithCachedChapters() - Querying database for manga.id=${manga.id}")
			val cachedChapters = database.getChaptersDao().findAll(manga.id)
			Log.d(TAG, "DEBUG: loadMangaWithCachedChapters() - Found ${cachedChapters.size} cached chapters in database")
			
			if (cachedChapters.isNotEmpty()) {
				val mangaChapters = cachedChapters.toMangaChapters()
				val branches = mangaChapters.mapNotNull { it.branch }.distinct()
				Log.d(TAG, "DEBUG: loadMangaWithCachedChapters() - Converting to ${mangaChapters.size} MangaChapters with branches: $branches")
				manga.copy(chapters = mangaChapters)
			} else {
				Log.d(TAG, "DEBUG: loadMangaWithCachedChapters() - No cached chapters found, returning null")
				null
			}
		} catch (e: Exception) {
			Log.e(TAG, "DEBUG: loadMangaWithCachedChapters() - Exception occurred: ${e.message}", e)
			null
		}
	}

	private fun newMangaPrefsEntity(mangaId: Long) = MangaPrefsEntity(
		mangaId = mangaId,
		mode = 0,
		cfBrightness = 0f,
		cfContrast = 0f,
		cfInvert = false,
		cfGrayscale = false,
		cfBookEffect = false,
		titleOverride = null,
		coverUrlOverride = null,
		contentRatingOverride = null,
		skipDecimalChapters = false
	)
}