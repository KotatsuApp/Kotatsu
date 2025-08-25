package org.koitharu.kotatsu.core.util

import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Utility for detecting languages from branch names and managing language preferences
 */
object LanguageDetectionUtils {

	/**
	 * Extract language code from branch name using various detection methods
	 */
	fun detectLanguageFromBranch(branch: String?): String? {
		if (branch.isNullOrBlank()) return null
		
		val cleanBranch = branch.trim()
		
		// Method 1: Direct ISO language code matches (en, ja, es, zh-CN, etc.)
		val directMatch = findDirectLanguageCode(cleanBranch)
		if (directMatch != null) return directMatch
		
		// Method 2: Display name matches using system locales (English, Japanese, etc.)
		val displayNameMatch = findByDisplayName(cleanBranch)
		if (displayNameMatch != null) return displayNameMatch
		
		// Method 3: Common pattern matching
		return findByCommonPatterns(cleanBranch)
	}
	
	/**
	 * Get available languages with display names for preferences
	 */
	fun getAvailableLanguages(): Map<String, String> {
		val result = mutableMapOf<String, String>()
		
		// Add languages from system locales
		val systemLocales = LocaleListCompat.getAdjustedDefault()
		for (i in 0 until systemLocales.size()) {
			val locale = systemLocales[i] ?: continue
			result[locale.language] = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
		}
		
		// Add common manga languages that might not be in system locales
		val commonMangaLanguages = mapOf(
			"en" to "English",
			"ja" to "Japanese",
			"ko" to "Korean", 
			"zh" to "Chinese",
			"es" to "Spanish",
			"fr" to "French",
			"de" to "German",
			"it" to "Italian",
			"pt" to "Portuguese",
			"ru" to "Russian",
			"ar" to "Arabic",
			"th" to "Thai",
			"vi" to "Vietnamese",
			"id" to "Indonesian",
			"tr" to "Turkish"
		)
		
		// Add common languages, preferring system display names
		commonMangaLanguages.forEach { (code, name) ->
			if (!result.containsKey(code)) {
				// Try to get proper display name from locale
				val displayName = runCatching {
					Locale(code).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }
				}.getOrElse { name }
				result[code] = displayName
			}
		}
		
		return result.toSortedMap { a, b -> result[a]!!.compareTo(result[b]!!) }
	}
	
	/**
	 * Get languages matching user's system preferences for auto-configuration
	 */
	fun getPreferredSystemLanguages(): Set<String> {
		val systemLocales = LocaleListCompat.getAdjustedDefault()
		val languages = mutableSetOf<String>()
		
		for (i in 0 until minOf(systemLocales.size(), 3)) { // Top 3 system languages
			val locale = systemLocales[i]
			if (locale != null) {
				languages.add(locale.language)
			}
		}
		
		return languages
	}
	
	private fun findDirectLanguageCode(branch: String): String? {
		// Check for exact ISO language codes
		val availableLanguages = Locale.getAvailableLocales()
		
		// Check for exact match (case insensitive)
		for (locale in availableLanguages) {
			if (locale.language.equals(branch, ignoreCase = true)) {
				return locale.language
			}
			// Check for language-country format (en-US, zh-CN, etc.)
			val localeString = "${locale.language}-${locale.country}"
			if (localeString.equals(branch, ignoreCase = true)) {
				return locale.language
			}
		}
		
		return null
	}
	
	private fun findByDisplayName(branch: String): String? {
		val systemLocales = LocaleListCompat.getAdjustedDefault()
		
		// Check against system locale display names
		for (i in 0 until systemLocales.size()) {
			val locale = systemLocales[i] ?: continue
			
			val displayLanguage = locale.getDisplayLanguage(locale)
			val displayLanguageEn = locale.getDisplayLanguage(Locale.ENGLISH)
			
			// Check if branch contains the display language name
			if (branch.contains(displayLanguage, ignoreCase = true) ||
				branch.contains(displayLanguageEn, ignoreCase = true)) {
				return locale.language
			}
		}
		
		return null
	}
	
	private fun findByCommonPatterns(branch: String): String? {
		// Common patterns in manga branch names
		val patterns = mapOf(
			"english" to "en",
			"japanese" to "ja", 
			"korean" to "ko",
			"chinese" to "zh",
			"spanish" to "es",
			"french" to "fr",
			"german" to "de",
			"italian" to "it",
			"portuguese" to "pt",
			"russian" to "ru",
			"arabic" to "ar",
			"thai" to "th",
			"vietnamese" to "vi",
			"indonesian" to "id",
			"turkish" to "tr",
			// Short forms
			"eng" to "en",
			"jap" to "ja",
			"kor" to "ko",
			"chi" to "zh",
			"spa" to "es",
			"fre" to "fr",
			"ger" to "de",
			"ita" to "it",
			"por" to "pt",
			"rus" to "ru"
		)
		
		val lowerBranch = branch.lowercase()
		for ((pattern, languageCode) in patterns) {
			if (lowerBranch.contains(pattern)) {
				return languageCode
			}
		}
		
		return null
	}
}