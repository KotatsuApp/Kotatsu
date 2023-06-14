package org.koitharu.kotatsu.settings.onboard

import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.map
import org.koitharu.kotatsu.core.util.ext.mapToSet
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
	private val settings: AppSettings,
) : BaseViewModel() {

	private val allSources = settings.remoteMangaSources

	private val locales = allSources.groupBy { it.locale }

	private val selectedLocales = locales.keys.toMutableSet()

	val list = MutableStateFlow<List<SourceLocale>?>(null)

	init {
		if (settings.isSourcesSelected) {
			selectedLocales.removeAll(settings.hiddenSources.mapNotNullToSet { x -> MangaSource(x).locale })
		} else {
			val deviceLocales = LocaleListCompat.getDefault().mapToSet { x ->
				x.language
			}
			selectedLocales.retainAll(deviceLocales)
			if (selectedLocales.isEmpty()) {
				selectedLocales += "en"
			}
			selectedLocales += null
		}
		rebuildList()
	}

	fun setItemChecked(key: String?, isChecked: Boolean) {
		val isModified = if (isChecked) {
			selectedLocales.add(key)
		} else {
			selectedLocales.remove(key)
		}
		if (isModified) {
			rebuildList()
		}
	}

	fun apply() {
		settings.hiddenSources = allSources.filterNot { x ->
			x.locale in selectedLocales
		}.mapToSet { x -> x.name }
		settings.markKnownSources(settings.newSources)
	}

	private fun rebuildList() {
		list.value = locales.map { (key, srcs) ->
			val locale = if (key != null) {
				Locale(key)
			} else null
			SourceLocale(
				key = key,
				title = locale?.getDisplayLanguage(locale)?.toTitleCase(locale),
				summary = srcs.joinToString { it.title },
				isChecked = key in selectedLocales,
			)
		}.sortedWith(SourceLocaleComparator())
	}

	private class SourceLocaleComparator : Comparator<SourceLocale?> {

		private val deviceLocales = LocaleListCompat.getAdjustedDefault()
			.map { it.language }

		override fun compare(a: SourceLocale?, b: SourceLocale?): Int {
			return when {
				a === b -> 0
				a?.key == null -> 1
				b?.key == null -> -1
				else -> {
					val indexA = deviceLocales.indexOf(a.key)
					val indexB = deviceLocales.indexOf(b.key)
					if (indexA == -1 && indexB == -1) {
						compareValues(a.title, b.title)
					} else {
						-2 - (indexA - indexB)
					}
				}
			}
		}
	}
}
