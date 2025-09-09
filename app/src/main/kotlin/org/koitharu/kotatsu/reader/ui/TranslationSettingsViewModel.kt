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
import org.koitharu.kotatsu.core.parser.MangaDataRepository
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
	private val mangaDataRepository: MangaDataRepository,
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
	
	// Track whether we're using manual preferences and store the full manga with chapters
	private var usingManualPreferences = false
	private var mangaWithChapters: Manga? = null

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
				usingManualPreferences = true
				mangaWithChapters = manga
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
					
					// First, try to load existing saved preferences from database
					Log.d(TAG, "DEBUG: Strategy 1 - Checking for existing saved preferences in database")
					val savedPrefs = try {
						translationPreferencesRepository.getPreferences(manga.id)
					} catch (e: Exception) {
						Log.d(TAG, "DEBUG: Strategy 1 - Failed to load saved preferences: ${e.message}")
						emptyList()
					}
					
					if (savedPrefs.isNotEmpty()) {
						Log.d(TAG, "DEBUG: Strategy 1 - Found ${savedPrefs.size} saved preferences in database:")
						savedPrefs.forEach { pref ->
							Log.d(TAG, "DEBUG: Saved Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}")
						}
						
						// Calculate chapter counts from cached chapters
						val branchCounts = mangaWithCachedChapters.chapters?.groupBy { it.branch }?.mapValues { it.value.size } ?: emptyMap()
						Log.d(TAG, "DEBUG: Strategy 1 - Calculated branch counts: $branchCounts")
						
						// Merge saved preferences with chapter counts
						val prefsWithCounts = savedPrefs.map { savedPref ->
							savedPref.copy(chapterCount = branchCounts[savedPref.branch] ?: 0)
						}
						
						Log.d(TAG, "DEBUG: Strategy 1 - Merged preferences with counts:")
						prefsWithCounts.forEach { pref ->
							Log.d(TAG, "DEBUG: Final Saved Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
						}
						
						// Use saved preferences with chapter counts (normal database mode)
						usingManualPreferences = false
						_preferences.value = prefsWithCounts
						return@launchJob
					}
					
					// No saved preferences found, generate new ones with default language logic
					Log.d(TAG, "DEBUG: Strategy 1 - No saved preferences found, generating with default language logic")
					val finalPrefs = generatePreferencesFromChapters(mangaWithCachedChapters)
					Log.d(TAG, "DEBUG: Final ${finalPrefs.size} preferences with default language logic applied:")
					finalPrefs.forEach { pref ->
						Log.d(TAG, "DEBUG: Final Cached Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
					}
					usingManualPreferences = true
					mangaWithChapters = mangaWithCachedChapters
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
						
						// First, try to load existing saved preferences from database
						Log.d(TAG, "DEBUG: Strategy 2 - Checking for existing saved preferences in database")
						val savedPrefs = try {
							translationPreferencesRepository.getPreferences(manga.id)
						} catch (e: Exception) {
							Log.d(TAG, "DEBUG: Strategy 2 - Failed to load saved preferences: ${e.message}")
							emptyList()
						}
						
						if (savedPrefs.isNotEmpty()) {
							Log.d(TAG, "DEBUG: Strategy 2 - Found ${savedPrefs.size} saved preferences in database:")
							savedPrefs.forEach { pref ->
								Log.d(TAG, "DEBUG: Saved Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}")
							}
							
							// Calculate chapter counts from loaded chapters
							val branchCounts = loadedDetails.allChapters.groupBy { it.branch }.mapValues { it.value.size }
							Log.d(TAG, "DEBUG: Strategy 2 - Calculated branch counts: $branchCounts")
							
							// Merge saved preferences with chapter counts
							val prefsWithCounts = savedPrefs.map { savedPref ->
								savedPref.copy(chapterCount = branchCounts[savedPref.branch] ?: 0)
							}
							
							Log.d(TAG, "DEBUG: Strategy 2 - Merged preferences with counts:")
							prefsWithCounts.forEach { pref ->
								Log.d(TAG, "DEBUG: Final Saved Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
							}
							
							// Use saved preferences with chapter counts (normal database mode)
							usingManualPreferences = false
							_preferences.value = prefsWithCounts
							return@launchJob
						}
						
						// No saved preferences found, generate new ones with default language logic
						val fullManga = loadedDetails.toManga()
						Log.d(TAG, "DEBUG: Strategy 2 - No saved preferences found, generating with default language logic")
						val finalPrefs = generatePreferencesFromChapters(fullManga)
						Log.d(TAG, "DEBUG: Final ${finalPrefs.size} preferences with default language logic applied:")
						finalPrefs.forEach { pref ->
							Log.d(TAG, "DEBUG: Final Network Pref - branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}, count=${pref.chapterCount}")
						}
						usingManualPreferences = true
						mangaWithChapters = fullManga
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
			
			try {
				// If using manual preferences, store manga in database first to satisfy foreign key
				if (usingManualPreferences) {
					ensureMangaStoredInDatabase()
				}
				// Now save preferences to database
				translationPreferencesRepository.setPreferences(manga.id, updatedPreferences)
				
				// Update the "last applied settings" so AutoTranslationConfigManager doesn't overwrite our changes
				val currentDefaults = appSettings.defaultTranslationLanguages  
				Log.d(TAG, "DEBUG: updatePreferencesOrder - Updating last applied settings to prevent auto-reset: $currentDefaults")
				autoTranslationConfigManager.markSettingsAsApplied(manga.id, currentDefaults)
			} catch (e: Exception) {
				Log.e(TAG, "Failed to save preference order to database", e)
				// Continue with UI update even if database save fails
			}
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
			
			try {
				// If using manual preferences, store manga in database first to satisfy foreign key
				if (usingManualPreferences) {
					Log.d(TAG, "DEBUG: togglePreferenceEnabled - Using manual preferences, storing manga first")
					ensureMangaStoredInDatabase()
				}
				// Now save preferences to database
				Log.d(TAG, "DEBUG: togglePreferenceEnabled - Saving ${updatedPreferences.size} preferences to database")
				updatedPreferences.forEachIndexed { index, pref ->
					Log.d(TAG, "DEBUG: togglePreferenceEnabled - Pref $index: branch='${pref.branch}', enabled=${pref.isEnabled}, priority=${pref.priority}")
				}
				translationPreferencesRepository.setPreferences(manga.id, updatedPreferences)
				Log.d(TAG, "DEBUG: togglePreferenceEnabled - Successfully saved preferences to database")
				
				// Update the "last applied settings" so AutoTranslationConfigManager doesn't overwrite our changes
				val currentDefaults = appSettings.defaultTranslationLanguages  
				Log.d(TAG, "DEBUG: togglePreferenceEnabled - Updating last applied settings to prevent auto-reset: $currentDefaults")
				autoTranslationConfigManager.markSettingsAsApplied(manga.id, currentDefaults)
			
			} catch (e: Exception) {
				Log.e(TAG, "Failed to save preference toggle to database", e)
				// Continue with UI update even if database save fails
			}
		}
	}

	fun resetToDefaults() {
		launchJob {
			// Only delete from database if we're not using manual preferences
			if (!usingManualPreferences) {
				try {
					translationPreferencesRepository.deletePreferences(manga.id)
				} catch (e: Exception) {
					Log.e(TAG, "Failed to delete preferences from database", e)
					// Continue with reload even if database delete fails
				}
			}
			// Reset the flags and reload
			usingManualPreferences = false
			mangaWithChapters = null
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
			.mapNotNullToSet { LanguageDetectionUtils.detectLanguageFromBranch(it.branch) }
			
		return enabledLanguages == defaultLanguages
	}

	private fun loadSkipDecimalChapters() {
		launchJob {
			val prefs = runCatchingCancellable { database.getPreferencesDao().find(manga.id) }.getOrNull()
			_skipDecimalChapters.value = prefs?.skipDecimalChapters ?: false
		}
	}

	fun setSkipDecimalChapters(skip: Boolean) {
		viewModelScope.launch {
			val existingPrefs = runCatchingCancellable { 
				database.getPreferencesDao().find(manga.id) 
			}.getOrNull() ?: newMangaPrefsEntity(manga.id)
			
			database.getPreferencesDao().upsert(existingPrefs.copy(skipDecimalChapters = skip))
			_skipDecimalChapters.value = skip
		}
	}

	/**
	 * Robust language detection system specifically designed for manga branch names
	 * Handles complex patterns, country variations, and scanlation group naming conventions
	 */
	private fun detectLanguageFromBranchFixed(branch: String): String? {
		if (branch.isBlank()) return null
		
		// Clean and normalize the branch name for better matching
		val cleanBranch = branch.trim()
		
		// Remove bracketed content (source/scanlation group names) before detection
		val withoutBrackets = cleanBranch.replace(Regex("""\([^)]*\)|\[[^\]]*\]|\{[^}]*\}"""), "").trim()
		
		val branchLower = withoutBrackets.lowercase()
		
		// Multi-pass detection system
		
		// Pass 1: Direct ISO language codes (highest priority)
		val isoMatch = findISOLanguageCode(branchLower)
		if (isoMatch != null) return isoMatch
		
		// Pass 2: Country/region name mapping (handles Indonesia, Brazil, etc.)
		val countryMatch = findLanguageByCountry(branchLower)
		if (countryMatch != null) return countryMatch
		
		// Pass 3: Language name variants (English, Inglês, Anglais, etc.)
		val languageNameMatch = findLanguageByName(branchLower)
		if (languageNameMatch != null) return languageNameMatch
		
		// Pass 4: Regional patterns (Español (Latinoamérica), Chinese (Simplified), etc.)
		val regionalMatch = findLanguageByRegion(branchLower)
		if (regionalMatch != null) return regionalMatch
		
		// Pass 5: Script/writing system detection (Simplified/Traditional Chinese, etc.)
		val scriptMatch = findLanguageByScript(branchLower)
		if (scriptMatch != null) return scriptMatch
		
		// Pass 6: Common scanlation group patterns and abbreviations
		val scanlationMatch = findLanguageByScanlationPatterns(branchLower)
		if (scanlationMatch != null) return scanlationMatch
		
		// Fallback: Use original LanguageDetectionUtils as last resort
		return LanguageDetectionUtils.detectLanguageFromBranch(branch)
	}
	
	private fun findISOLanguageCode(branchLower: String): String? {
		// Extract potential ISO codes from branch with stricter word boundaries
		val isoPattern = Regex("""\b([a-z]{2})(?:-[a-z]{2})?\b""")
		val matches = isoPattern.findAll(branchLower)
		
		val validLanguages = setOf(
			"en", "ja", "ko", "zh", "es", "fr", "de", "it", "pt", "ru", "ar", "th", "vi", "id", "tr", "hi", "bn", "fa", "he", "pl", "uk", "cs", "sk", "hu", "ro", "bg", "hr", "sr", "sl", "lv", "lt", "et", "fi", "sv", "no", "da", "nl", "ms", "tl", "mr", "gu", "te", "kn", "ta", "ml", "ur", "ne", "si", "my", "km", "lo", "ka", "am", "sw", "zu", "af", "eu", "ca", "gl", "cy", "ga", "mt", "is", "fo", "lb", "rm"
		)
		
		// Prioritize matches that are more likely to be language codes
		val potentialMatches = mutableListOf<String>()
		
		for (match in matches) {
			val code = match.groupValues[1]
			if (code in validLanguages) {
				// Skip matches that are likely part of words rather than language codes
				val startIndex = match.range.first
				val endIndex = match.range.last
				
				// Check context to avoid false positives
				val isPartOfWord = when {
					// Check if it's surrounded by non-space characters (part of a word)
					startIndex > 0 && branchLower[startIndex - 1].isLetter() -> true
					endIndex < branchLower.length - 1 && branchLower[endIndex + 1].isLetter() -> true
					// Common false positives: prepositions and common words
					code == "on" && branchLower.contains("on ") -> true
					code == "in" && branchLower.contains("in ") -> true
					code == "is" && branchLower.contains("is ") -> true
					code == "of" && branchLower.contains("of ") -> true
					else -> false
				}
				
				if (!isPartOfWord) {
					potentialMatches.add(code)
				}
			}
		}
		
		// Return the first valid match
		return potentialMatches.firstOrNull()
	}
	
	private fun findLanguageByCountry(branchLower: String): String? {
		val countryToLanguage = mapOf(
			// Major manga languages by country/region
			"japan" to "ja", "japanese" to "ja", "nihon" to "ja", "nippon" to "ja",
			"korea" to "ko", "korean" to "ko", "hanguk" to "ko", "corea" to "ko",
			"china" to "zh", "chinese" to "zh", "zhongguo" to "zh", "zhonghua" to "zh",
			"taiwan" to "zh", "hongkong" to "zh", "hong kong" to "zh", "macau" to "zh", "macao" to "zh",
			"usa" to "en", "america" to "en", "american" to "en", "united states" to "en",
			"uk" to "en", "britain" to "en", "british" to "en", "england" to "en", "english" to "en",
			"canada" to "en", "canadian" to "en", "australia" to "en", "australian" to "en",
			"indonesia" to "id", "indonesian" to "id",
			"thailand" to "th", "thai" to "th", "siam" to "th",
			"vietnam" to "vi", "vietnamese" to "vi", "viet nam" to "vi",
			"philippines" to "tl", "filipino" to "tl", "tagalog" to "tl", "pilipinas" to "tl",
			"malaysia" to "ms", "malaysian" to "ms", "malay" to "ms",
			"spain" to "es", "spanish" to "es", "españa" to "es", "espanol" to "es", "español" to "es",
			"mexico" to "es", "mexican" to "es", "méxico" to "es",
			"argentina" to "es", "argentinian" to "es", "chile" to "es", "chilean" to "es",
			"colombia" to "es", "colombian" to "es", "peru" to "es", "peruvian" to "es",
			"venezuela" to "es", "venezuelan" to "es", "ecuador" to "es", "ecuadorian" to "es",
			"uruguay" to "es", "uruguayan" to "es", "bolivia" to "es", "bolivian" to "es",
			"brazil" to "pt", "brazilian" to "pt", "brasil" to "pt", "português" to "pt",
			"portugal" to "pt", "portuguese" to "pt", "lusitano" to "pt",
			"france" to "fr", "french" to "fr", "français" to "fr", "francais" to "fr",
			"germany" to "de", "german" to "de", "deutsch" to "de", "deutschland" to "de",
			"italy" to "it", "italian" to "it", "italiano" to "it", "italia" to "it",
			"russia" to "ru", "russian" to "ru", "русский" to "ru", "россия" to "ru",
			"poland" to "pl", "polish" to "pl", "polska" to "pl", "polski" to "pl",
			"turkey" to "tr", "turkish" to "tr", "türkiye" to "tr", "türk" to "tr",
			"india" to "hi", "indian" to "hi", "hindi" to "hi", "भारत" to "hi",
			"arab" to "ar", "arabic" to "ar", "عربي" to "ar", "saudi" to "ar", "emirates" to "ar",
			"iran" to "fa", "iranian" to "fa", "persian" to "fa", "farsi" to "fa", "فارسی" to "fa",
			"israel" to "he", "israeli" to "he", "hebrew" to "he", "עברית" to "he"
		)
		
		for ((country, langCode) in countryToLanguage) {
			if (branchLower.contains(country)) {
				return langCode
			}
		}
		
		return null
	}
	
	private fun findLanguageByName(branchLower: String): String? {
		val languageNames = mapOf(
			// English variations
			"english" to "en", "inglês" to "en", "anglais" to "en", "inglese" to "en", "ingles" to "en",
			"английский" to "en", "angielski" to "en", "engleză" to "en", "engelska" to "en",
			
			// Japanese variations  
			"japanese" to "ja", "japonês" to "ja", "japonais" to "ja", "giapponese" to "ja", "japonés" to "ja",
			"日本語" to "ja", "にほんご" to "ja", "nihongo" to "ja", "japoński" to "ja", "japoneză" to "ja",
			
			// Korean variations
			"korean" to "ko", "coreano" to "ko", "coréen" to "ko", "coreano" to "ko", "한국어" to "ko",
			"hangugeo" to "ko", "hangukeo" to "ko", "koreański" to "ko", "coreeană" to "ko",
			
			// Chinese variations
			"chinese" to "zh", "chinês" to "zh", "chinois" to "zh", "cinese" to "zh", "chino" to "zh",
			"中文" to "zh", "zhongwen" to "zh", "中国话" to "zh", "chiński" to "zh", "chineză" to "zh",
			
			// Spanish variations
			"spanish" to "es", "espanhol" to "es", "espagnol" to "es", "spagnolo" to "es",
			"español" to "es", "castellano" to "es", "kastylijski" to "es", "spaniolă" to "es",
			
			// Portuguese variations
			"portuguese" to "pt", "português" to "pt", "portugais" to "pt", "portoghese" to "pt",
			"português" to "pt", "portugalski" to "pt", "portugheză" to "pt", "lusitano" to "pt",
			
			// French variations
			"french" to "fr", "francês" to "fr", "français" to "fr", "francese" to "fr", "francés" to "fr",
			"francuski" to "fr", "franceză" to "fr", "gallego" to "fr",
			
			// German variations
			"german" to "de", "alemão" to "de", "allemand" to "de", "tedesco" to "de", "alemán" to "de",
			"deutsch" to "de", "niemiecki" to "de", "germană" to "de",
			
			// Italian variations
			"italian" to "it", "italiano" to "it", "italien" to "it", "włoski" to "it", "italiană" to "it",
			
			// Russian variations
			"russian" to "ru", "russo" to "ru", "russe" to "ru", "russo" to "ru", "ruso" to "ru",
			"русский" to "ru", "rosyjski" to "ru", "rusă" to "ru",
			
			// Indonesian variations
			"indonesian" to "id", "indonésio" to "id", "indonésien" to "id", "indonesiano" to "id",
			"bahasa indonesia" to "id", "indonezyjski" to "id", "indoneziană" to "id",
			
			// Vietnamese variations
			"vietnamese" to "vi", "vietnamita" to "vi", "vietnamien" to "vi", "vietnamita" to "vi",
			"tiếng việt" to "vi", "việt" to "vi", "wietnamski" to "vi", "vietnameză" to "vi",
			
			// Thai variations
			"thai" to "th", "tailandês" to "th", "thaï" to "th", "tailandese" to "th", "tailandés" to "th",
			"ไทย" to "th", "ภาษาไทย" to "th", "tajski" to "th", "thailandeză" to "th",
			
			// Arabic variations
			"arabic" to "ar", "árabe" to "ar", "arabe" to "ar", "arabo" to "ar", "árabe" to "ar",
			"عربي" to "ar", "العربية" to "ar", "arabski" to "ar", "arabă" to "ar",
			
			// Turkish variations
			"turkish" to "tr", "turco" to "tr", "turc" to "tr", "turco" to "tr",
			"türkçe" to "tr", "türk" to "tr", "turecki" to "tr", "turcă" to "tr",
			
			// Polish variations
			"polish" to "pl", "polonês" to "pl", "polonais" to "pl", "polacco" to "pl", "polaco" to "pl",
			"polski" to "pl", "polszczyzna" to "pl", "poloneză" to "pl"
		)
		
		for ((name, langCode) in languageNames) {
			if (branchLower.contains(name)) {
				return langCode
			}
		}
		
		return null
	}
	
	private fun findLanguageByRegion(branchLower: String): String? {
		return when {
			// Spanish regional variants
			branchLower.contains("latinoamérica") || branchLower.contains("latinoamerica") ||
			branchLower.contains("latam") || branchLower.contains("américa latina") ||
			branchLower.contains("hispanoamérica") || branchLower.contains("sudamérica") -> "es"
			
			// Portuguese regional variants  
			branchLower.contains("brasil") || branchLower.contains("brazil") ||
			branchLower.contains("pt-br") || branchLower.contains("brasileiro") -> "pt"
			
			// Chinese regional variants
			branchLower.contains("simplified") || branchLower.contains("zh-cn") ||
			branchLower.contains("mainland") || branchLower.contains("prc") -> "zh"
			
			branchLower.contains("traditional") || branchLower.contains("zh-tw") ||
			branchLower.contains("zh-hk") || branchLower.contains("taiwan") ||
			branchLower.contains("hong kong") || branchLower.contains("hongkong") -> "zh"
			
			// English regional variants
			branchLower.contains("american") || branchLower.contains("us english") ||
			branchLower.contains("en-us") || branchLower.contains("usa") -> "en"
			
			branchLower.contains("british") || branchLower.contains("uk english") ||
			branchLower.contains("en-gb") || branchLower.contains("england") -> "en"
			
			else -> null
		}
	}
	
	private fun findLanguageByScript(branchLower: String): String? {
		return when {
			// Chinese script indicators
			branchLower.contains("simplified") || branchLower.contains("简体") ||
			branchLower.contains("简化字") -> "zh"
			
			branchLower.contains("traditional") || branchLower.contains("繁體") ||
			branchLower.contains("繁体") || branchLower.contains("正體") -> "zh"
			
			// Japanese script indicators
			branchLower.contains("hiragana") || branchLower.contains("katakana") ||
			branchLower.contains("kanji") || branchLower.contains("ひらがな") ||
			branchLower.contains("カタカナ") || branchLower.contains("漢字") -> "ja"
			
			// Korean script indicators  
			branchLower.contains("hangul") || branchLower.contains("hangeul") ||
			branchLower.contains("한글") -> "ko"
			
			// Arabic script indicators
			branchLower.contains("arabic script") || branchLower.contains("العربية") -> "ar"
			
			// Cyrillic script (Russian and others)
			branchLower.contains("cyrillic") || branchLower.contains("кириллица") -> "ru"
			
			else -> null
		}
	}
	
	private fun findLanguageByScanlationPatterns(branchLower: String): String? {
		return when {
			// English scanlation groups/patterns
			branchLower.contains("official") || branchLower.contains("english") ||
			branchLower.contains("webtoon") || branchLower.contains("viz") ||
			branchLower.contains("kodansha") || branchLower.contains("yen press") ||
			branchLower.contains("seven seas") || branchLower.contains("dark horse") -> "en"
			
			// Japanese official patterns
			branchLower.contains("raw") || branchLower.contains("生") ||
			branchLower.contains("jp") || branchLower.contains("jpn") -> "ja"
			
			// Spanish scanlation patterns
			branchLower.contains("scan") && (branchLower.contains("es") || branchLower.contains("esp")) -> "es"
			
			// Portuguese scanlation patterns  
			branchLower.contains("pt") && branchLower.contains("scan") -> "pt"
			
			// French scanlation patterns
			branchLower.contains("fr") && branchLower.contains("scan") -> "fr"
			
			// Generic scan group patterns by common abbreviations
			branchLower.matches(Regex(""".*\b(eng|en)\b.*""")) -> "en"
			branchLower.matches(Regex(""".*\b(esp|es)\b.*""")) -> "es"
			branchLower.matches(Regex(""".*\b(pt|por)\b.*""")) -> "pt"
			branchLower.matches(Regex(""".*\b(fr|fra)\b.*""")) -> "fr"
			branchLower.matches(Regex(""".*\b(de|ger)\b.*""")) -> "de"
			branchLower.matches(Regex(""".*\b(it|ita)\b.*""")) -> "it"
			
			else -> null
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
			// with a fix for common detection issues
			val branchLanguage = detectLanguageFromBranchFixed(branch ?: "")
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
		skipDecimalChapters = false,
		lastAppliedTranslationLanguages = null
	)
	
	/**
	 * Ensure manga is stored in database so preferences can be saved
	 */
	private suspend fun ensureMangaStoredInDatabase() {
		val fullManga = mangaWithChapters ?: return
		try {
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - manga.id=${fullManga.id}")
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - manga.title='${fullManga.title}'")
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - manga.source=${fullManga.source}")
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - chapters=${fullManga.chapters?.size ?: 0}")
			
			// Check if manga already exists in database
			val existingManga = database.getMangaDao().find(fullManga.id)
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - existing manga in DB: ${existingManga != null}")
			
			if (existingManga != null) {
				Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - Manga already exists, using replaceExisting=true")
				mangaDataRepository.storeManga(fullManga, replaceExisting = true)
			} else {
				Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - Manga doesn't exist, storing with replaceExisting=false")
				mangaDataRepository.storeManga(fullManga, replaceExisting = false)
			}
			
			// Verify it was stored
			val afterStorage = database.getMangaDao().find(fullManga.id)
			Log.d(TAG, "DEBUG: ensureMangaStoredInDatabase - After storage, manga exists: ${afterStorage != null}")
			
			Log.d(TAG, "DEBUG: Successfully stored manga in database")
			// Once manga is stored, we can treat preferences normally
			usingManualPreferences = false
		} catch (e: Exception) {
			Log.e(TAG, "Failed to store manga in database", e)
			throw e
		}
	}
}