package org.koitharu.kotatsu.settings.onboard

import androidx.collection.ArraySet
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale
import org.koitharu.kotatsu.utils.ext.map
import org.koitharu.kotatsu.utils.ext.mapTo
import org.koitharu.kotatsu.utils.ext.mapToSet
import java.util.*

class OnboardViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	private val allSources = MangaSource.values().filterNot { x -> x == MangaSource.LOCAL }

	private val locales = allSources.mapTo(ArraySet()) {
			it.locale
		}

	private val selectedLocales = locales.toMutableSet()

	val list = MutableLiveData<List<SourceLocale>?>()

	init {
		if (settings.isSourcesSelected) {
			selectedLocales.removeAll(settings.hiddenSources.map { x -> MangaSource.valueOf(x).locale })
		} else {
			LocaleListCompat.getDefault().mapTo(selectedLocales) { x ->
				x.language
			}
			selectedLocales.retainAll(allSources.map { x -> x.locale })
			if (selectedLocales.isEmpty()) {
				selectedLocales += "en"
			}
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
	}

	private fun rebuildList() {
		list.value = locales.map { key ->
			val locale = if (key != null) {
				Locale(key)
			} else null
			SourceLocale(
				key = key,
				title = locale?.getDisplayLanguage(locale)?.capitalize(locale),
				isChecked = key in selectedLocales
			)
		}.sortedWith(SourceLocaleComparator())
	}

	private class SourceLocaleComparator : Comparator<SourceLocale> {

		private val deviceLocales = LocaleListCompat.getAdjustedDefault()
			.map { it.language }

		override fun compare(a: SourceLocale?, b: SourceLocale?): Int {
			return when {
				a === b -> 0
				a?.key == null -> 1
				b?.key == null -> -1
				else -> {
					val index = deviceLocales.indexOf(a.key)
					if (index == -1) {
						compareValues(a.title, b.title)
					} else {
						-2 - index
					}
				}
			}
		}
	}
}