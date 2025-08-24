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
				val fullManga = mangaDetails.toManga()
				val prefs = translationFallbackManager.getAvailableTranslationsWithPreferences(fullManga)
				_preferences.value = prefs
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

	private fun loadSkipDecimalChapters() {
		launchJob {
			val prefs = database.getPreferencesDao().find(manga.id)
			_skipDecimalChapters.value = prefs?.skipDecimalChapters ?: false
		}
	}

	fun setSkipDecimalChapters(skip: Boolean) {
		viewModelScope.launch {
			val existingPrefs = database.getPreferencesDao().find(manga.id) ?: newMangaPrefsEntity(manga.id)
			database.getPreferencesDao().upsert(existingPrefs.copy(skipDecimalChapters = skip))
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