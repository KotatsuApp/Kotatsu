package org.koitharu.kotatsu.settings.onboard

import android.content.Context
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.sortedWithSafe
import org.koitharu.kotatsu.core.util.ext.toList
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
	@ApplicationContext context: Context,
) : BaseViewModel() {

	private val allSources = repository.allMangaSources
	private val locales = allSources.groupBy { it.locale }
	private val selectedLocales = HashSet<String?>()
	val list = MutableStateFlow<List<SourceLocale>?>(null)
	private var updateJob: Job

	init {
		updateJob = launchJob(Dispatchers.Default) {
			if (repository.isSetupRequired()) {
				selectedLocales += ConfigurationCompat.getLocales(context.resources.configuration).toList()
					.firstOrNull { lc -> lc.language in locales.keys }?.language
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
			val locale = key?.let { Locale(it) }
			SourceLocale(
				key = key,
				title = locale?.getDisplayLanguage(locale)?.toTitleCase(locale),
				summary = srcs.joinToString { it.title },
				isChecked = key in selectedLocales,
			)
		}.sortedWithSafe(SourceLocaleComparator())
	}

	private class SourceLocaleComparator : Comparator<SourceLocale> {

		private val delegate = nullsFirst(LocaleComparator())

		override fun compare(a: SourceLocale, b: SourceLocale): Int {
			val localeA = a.key?.let { Locale(it) }
			val localeB = b.key?.let { Locale(it) }
			return delegate.compare(localeA, localeB)
		}
	}
}
