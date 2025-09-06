package org.koitharu.kotatsu.reader.domain

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.LanguageDetectionUtils
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.data.TranslationPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic application of translation language preferences
 * when users enter manga details or reader
 */
@Singleton
class AutoTranslationConfigManager @Inject constructor(
    private val appSettings: AppSettings,
    private val translationPreferencesRepository: TranslationPreferencesRepository,
    private val translationFallbackManager: TranslationFallbackManager,
    @ApplicationContext private val context: android.content.Context,
) {
    
    private val prefs: SharedPreferences by lazy { 
        PreferenceManager.getDefaultSharedPreferences(context) 
    }

    /**
     * Automatically configure translation preferences for a manga based on global settings
     * Only applies if global settings have changed since last application
     * Should be called when user enters manga details or reader
     */
    suspend fun autoConfigureIfNeeded(manga: Manga) {
        val defaultLanguages = appSettings.defaultTranslationLanguages
        if (defaultLanguages.isEmpty()) return
        
        try {
            // Get current preferences for this manga
            val currentPreferences = translationFallbackManager.getAvailableTranslationsWithPreferences(manga)
            
            // Check if we need to reconfigure based on whether global settings changed
            if (shouldReconfigure(manga.id, currentPreferences, defaultLanguages)) {
                applyDefaultLanguages(manga.id, currentPreferences, defaultLanguages)
                // Store the current global settings for this manga to track changes
                storeLastAppliedSettings(manga.id, defaultLanguages)
            }
        } catch (e: Exception) {
            // Don't let auto-configuration errors break manga loading
            // Just silently continue with existing preferences
        }
    }

    /**
     * Force apply default languages regardless of current state
     */
    suspend fun forceApplyDefaults(mangaId: Long) {
        val defaultLanguages = appSettings.defaultTranslationLanguages
        if (defaultLanguages.isEmpty()) return
        
        try {
            val manga = getMangaFromId(mangaId) ?: return
            val currentPreferences = translationFallbackManager.getAvailableTranslationsWithPreferences(manga)
            applyDefaultLanguages(mangaId, currentPreferences, defaultLanguages)
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    /**
     * Check if preferences should be reconfigured based on whether global settings changed
     */
    private suspend fun shouldReconfigure(
        mangaId: Long,
        currentPreferences: List<MangaTranslationPreference>,
        defaultLanguages: Set<String>
    ): Boolean {
        // Always reconfigure if no preferences are enabled (first time setup)
        if (currentPreferences.none { it.isEnabled }) {
            return true
        }

        // Get the last applied settings for this manga
        val lastAppliedSettings = getLastAppliedSettings(mangaId)
        
        // Reconfigure only if global settings have changed since last application
        return lastAppliedSettings != defaultLanguages
    }

    /**
     * Store the last applied global settings for a manga
     */
    private suspend fun storeLastAppliedSettings(mangaId: Long, defaultLanguages: Set<String>) {
        try {
            // Store as comma-separated string in shared preferences or database
            val settingsString = defaultLanguages.sorted().joinToString(",")
            // Using shared preferences to store with manga-specific key
            prefs.edit().putString("last_applied_langs_$mangaId", settingsString).apply()
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }

    /**
     * Get the last applied global settings for a manga
     */
    private suspend fun getLastAppliedSettings(mangaId: Long): Set<String> {
        return try {
            val settingsString = prefs.getString("last_applied_langs_$mangaId", null)
            if (settingsString.isNullOrEmpty()) {
                emptySet()
            } else {
                settingsString.split(",").toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Apply default language settings to manga preferences
     */
    private suspend fun applyDefaultLanguages(
        mangaId: Long,
        currentPreferences: List<MangaTranslationPreference>,
        defaultLanguages: Set<String>
    ) {
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

        translationPreferencesRepository.setPreferences(mangaId, updatedPreferences)
    }

    /**
     * Mark current global settings as applied for a manga to prevent auto-reconfiguration
     * Should be called when user manually changes preferences
     */
    suspend fun markSettingsAsApplied(mangaId: Long, defaultLanguages: Set<String>) {
        storeLastAppliedSettings(mangaId, defaultLanguages)
    }

    /**
     * Helper to get manga from ID - this might need to be injected differently
     * depending on how the app architecture works
     */
    private suspend fun getMangaFromId(mangaId: Long): Manga? {
        // TODO: This needs to be implemented based on how manga retrieval works in the app
        // Might need to inject a repository or use case for this
        return null
    }
}