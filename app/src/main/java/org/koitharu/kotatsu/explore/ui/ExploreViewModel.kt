package org.koitharu.kotatsu.explore.ui

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.utils.ext.map
import java.util.*

private const val KEY_ENABLED = "!"

class ExploreViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	val items = MutableLiveData<List<ExploreItem>>(emptyList())

	init {
		buildList()
	}

	private fun buildList() {
		val sources = settings.getMangaSources(includeHidden = true)
		val hiddenSources = settings.hiddenSources
		val map = sources.groupByTo(TreeMap(LocaleKeyComparator())) {
			if (it.name !in hiddenSources) {
				KEY_ENABLED
			} else {
				it.locale
			}
		}
		val result = ArrayList<ExploreItem>(sources.size + map.size + 1)
		result += ExploreItem.Buttons
		val enabledSources = map.remove(KEY_ENABLED)
		if (!enabledSources.isNullOrEmpty()) {
			result += ExploreItem.Header(R.string.enabled_sources)
			enabledSources.mapTo(result) {
				ExploreItem.Source(
					source = it,
					summary = it.getLocaleTitle(),
				)
			}
		}
		items.value = result
	}

	private class LocaleKeyComparator : Comparator<String?> {

		private val deviceLocales = LocaleListCompat.getAdjustedDefault()
			.map { it.language }

		override fun compare(a: String?, b: String?): Int {
			when {
				a == b -> return 0
				a == null -> return 1
				b == null -> return -1
			}
			val ai = deviceLocales.indexOf(a!!)
			val bi = deviceLocales.indexOf(b!!)
			return when {
				ai < 0 && bi < 0 -> a.compareTo(b)
				ai < 0 -> 1
				bi < 0 -> -1
				else -> ai.compareTo(bi)
			}
		}
	}
}