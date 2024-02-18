package org.koitharu.kotatsu.main.ui.welcome

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
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	private val allSources = repository.allMangaSources
	private val localesGroups by lazy { allSources.groupBy { it.locale?.let { x -> Locale(x) } } }

	private var updateJob: Job

	val locales = MutableStateFlow(
		FilterProperty<Locale?>(
			availableItems = listOf(null),
			selectedItems = setOf(null),
			isLoading = true,
			error = null,
		),
	)

	val types = MutableStateFlow(
		FilterProperty(
			availableItems = ContentType.entries.toList(),
			selectedItems = setOf(ContentType.MANGA),
			isLoading = false,
			error = null,
		),
	)

	init {
		updateJob = launchJob(Dispatchers.Default) {
			val languages = localesGroups.keys.associateBy { x -> x?.language }
			val selectedLocales = HashSet<Locale?>(2)
			selectedLocales += ConfigurationCompat.getLocales(context.resources.configuration).toList()
				.firstNotNullOfOrNull { lc -> languages[lc.language] }
			selectedLocales += null
			locales.value = locales.value.copy(
				availableItems = localesGroups.keys.sortedWithSafe(nullsFirst(LocaleComparator())),
				selectedItems = selectedLocales,
				isLoading = false,
			)
			repository.assimilateNewSources()
			commit()
		}
	}

	fun setLocaleChecked(locale: Locale?, isChecked: Boolean) {
		val snapshot = locales.value
		locales.value = snapshot.copy(
			selectedItems = if (isChecked) {
				snapshot.selectedItems + locale
			} else {
				snapshot.selectedItems - locale
			},
		)
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.Default) {
			prevJob.join()
			commit()
		}
	}

	fun setTypeChecked(type: ContentType, isChecked: Boolean) {
		val snapshot = types.value
		types.value = snapshot.copy(
			selectedItems = if (isChecked) {
				snapshot.selectedItems + type
			} else {
				snapshot.selectedItems - type
			},
		)
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.Default) {
			prevJob.join()
			commit()
		}
	}

	private suspend fun commit() {
		val languages = locales.value.selectedItems.mapToSet { it?.language }
		val types = types.value.selectedItems
		val enabledSources = allSources.filterTo(EnumSet.noneOf(MangaSource::class.java)) { x ->
			x.contentType in types && x.locale in languages
		}
		repository.setSourcesEnabledExclusive(enabledSources)
	}
}
