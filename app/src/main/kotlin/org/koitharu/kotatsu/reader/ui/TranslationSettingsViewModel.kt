package org.koitharu.kotatsu.reader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.nav.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.reader.data.TranslationPreferencesRepository
import org.koitharu.kotatsu.reader.domain.MangaTranslationPreference
import org.koitharu.kotatsu.reader.domain.TranslationFallbackManager
import org.koitharu.kotatsu.core.util.LanguageDetectionUtils
import javax.inject.Inject

@HiltViewModel
class TranslationSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val translationPreferencesRepository: TranslationPreferencesRepository,
	private val translationFallbackManager: TranslationFallbackManager,
	private val appSettings: AppSettings,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val database: MangaDatabase,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	val manga = intent.manga ?: throw IllegalArgumentException("Manga is required")

	private val _preferences = MutableStateFlow<List<MangaTranslationPreference>>(emptyList())
	val preferences = _preferences.asStateFlow()

	private val _skipDecimalChapters = MutableStateFlow(false)
	val skipDecimalChapters = _skipDecimalChapters.asStateFlow()

	val isGlobalFallbackEnabled = appSettings.isTranslationFallbackEnabled

	init {
		loadPreferences()
		loadSkipDecimalChapters()
	}

	private fun loadPreferences() {
		launchJob {
			// Load complete manga details with chapters
			detailsLoadUseCase(intent, force = false).collect { mangaDetails ->
				// Ensure we have loaded chapters before proceeding
				if (mangaDetails.isLoaded && mangaDetails.allChapters.isNotEmpty()) {
					// Create manga with ALL chapters (not filtered by branch)
					val baseManga = mangaDetails.toManga()
					val fullManga = baseManga.copy(chapters = mangaDetails.allChapters)
					val prefs = translationFallbackManager.getAvailableTranslationsWithPreferences(fullManga)
					_preferences.value = prefs
				}
			}
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