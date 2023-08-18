package org.koitharu.kotatsu.settings.onboard

import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.map
import org.koitharu.kotatsu.core.util.ext.mapToSet
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
) : BaseViewModel() {

	private val allSources = repository.allMangaSources
	private val locales = allSources.groupBy { it.locale }
	private val selectedLocales = HashSet<String?>()
	val list = MutableStateFlow<List<SourceLocale>?>(null)
	private var updateJob: Job

	init {
		updateJob = launchJob(Dispatchers.Default) {
			if (repository.isSetupRequired()) {
				val deviceLocales = LocaleListCompat.getDefault().mapToSet { x ->
					x.language
				}
				selectedLocales.addAll(deviceLocales)
				if (selectedLocales.isEmpty()) {
					selectedLocales += "en"
				}
				selectedLocales += null
			} else {
				selectedLocales.addAll(
					repository.getEnabledSources().mapNotNullToSet { x -> x.locale },
				)
			}
			commit()
		}
	}

	fun setItemChecked(key: String?, isChecked: Boolean) {
		val isModified = if (isChecked) {
			selectedLocales.add(key)
		} else {
			selectedLocales.remove(key)
		}
		if (isModified) {
			val prevJob = updateJob
			updateJob = launchJob(Dispatchers.Default) {
				prevJob.join()
				commit()
			}
		}
	}

	private suspend fun commit() {
		val enabledSources = allSources.filterTo(EnumSet.noneOf(MangaSource::class.java)) { x ->
			x.locale in selectedLocales
		}
		repository.setSourcesEnabledExclusive(enabledSources)
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
