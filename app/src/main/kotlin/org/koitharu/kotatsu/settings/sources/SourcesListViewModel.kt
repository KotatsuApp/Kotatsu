package org.koitharu.kotatsu.settings.sources

import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.AlphanumComparator
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.map
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import java.util.EnumSet
import java.util.Locale
import java.util.TreeMap
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val KEY_ENABLED = "!"
private const val TIP_REORDER = "src_reorder"

@HiltViewModel
class SourcesListViewModel @Inject constructor(
	private val settings: AppSettings,
	private val repository: MangaSourcesRepository,
) : BaseViewModel() {

	val items = MutableStateFlow<List<SourceConfigItem>>(emptyList())
	val onActionDone = MutableEventFlow<ReversibleAction>()
	private val mutex = Mutex()

	private val expandedGroups = HashSet<String?>()
	private var searchQuery: String? = null

	init {
		launchAtomicJob(Dispatchers.Default) {
			buildList()
		}
	}

	fun reorderSources(oldPos: Int, newPos: Int): Boolean {
		val snapshot = items.value.toMutableList()
		val item = (snapshot[oldPos] as? SourceConfigItem.SourceItem) ?: return false
		if ((snapshot[newPos] as? SourceConfigItem.SourceItem)?.isDraggable != true) return false
		launchAtomicJob(Dispatchers.Default) {
			var targetPosition = 0
			for ((i, x) in snapshot.withIndex()) {
				if (i == newPos) {
					break
				}
				if (x is SourceConfigItem.SourceItem) {
					targetPosition++
				}
			}
			repository.setPosition(item.source, targetPosition)
			buildList()
		}
		return true
	}

	fun canReorder(oldPos: Int, newPos: Int): Boolean {
		val snapshot = items.value.toMutableList()
		if ((snapshot[oldPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		return (snapshot[newPos] as? SourceConfigItem.SourceItem)?.isEnabled == true
	}

	fun setEnabled(source: MangaSource, isEnabled: Boolean) {
		launchAtomicJob(Dispatchers.Default) {
			val rollback = repository.setSourceEnabled(source, isEnabled)
			if (!isEnabled) {
				onActionDone.call(ReversibleAction(R.string.source_disabled, rollback))
			}
			buildList()
		}
	}

	fun disableAll() {
		launchAtomicJob(Dispatchers.Default) {
			repository.disableAllSources()
			buildList()
		}
	}

	fun expandOrCollapse(headerId: String?) {
		launchAtomicJob {
			if (headerId in expandedGroups) {
				expandedGroups.remove(headerId)
			} else {
				expandedGroups.add(headerId)
			}
			buildList()
		}
	}

	fun performSearch(query: String?) {
		launchAtomicJob {
			searchQuery = query?.trim()
			buildList()
		}
	}

	fun onTipClosed(item: SourceConfigItem.Tip) {
		launchAtomicJob(Dispatchers.Default) {
			settings.closeTip(item.key)
			buildList()
		}
	}

	private suspend fun buildList() = withContext(Dispatchers.Default) {
		val allSources = repository.allMangaSources
		val enabledSources = repository.getEnabledSources()
		val enabledSet = EnumSet.copyOf(enabledSources)
		val query = searchQuery
		if (!query.isNullOrEmpty()) {
			items.value = allSources.mapNotNull {
				if (!it.title.contains(query, ignoreCase = true)) {
					return@mapNotNull null
				}
				SourceConfigItem.SourceItem(
					source = it,
					summary = it.getLocaleTitle(),
					isEnabled = it in enabledSet,
					isDraggable = false,
				)
			}.ifEmpty {
				listOf(SourceConfigItem.EmptySearchResult)
			}
			return@withContext
		}
		val map = allSources.groupByTo(TreeMap(LocaleKeyComparator())) {
			if (it in enabledSet) {
				KEY_ENABLED
			} else {
				it.locale
			}
		}
		map.remove(KEY_ENABLED)
		val result = ArrayList<SourceConfigItem>(allSources.size + map.size + 2)
		if (enabledSources.isNotEmpty()) {
			result += SourceConfigItem.Header(R.string.enabled_sources)
			if (settings.isTipEnabled(TIP_REORDER)) {
				result += SourceConfigItem.Tip(TIP_REORDER, R.drawable.ic_tap_reorder, R.string.sources_reorder_tip)
			}
			enabledSources.mapTo(result) {
				SourceConfigItem.SourceItem(
					source = it,
					summary = it.getLocaleTitle(),
					isEnabled = true,
					isDraggable = true,
				)
			}
		}
		if (enabledSources.size != allSources.size) {
			result += SourceConfigItem.Header(R.string.available_sources)
			val comparator = compareBy<MangaSource, String>(AlphanumComparator()) { it.name }
			for ((key, list) in map) {
				list.sortWith(comparator)
				val isExpanded = key in expandedGroups
				result += SourceConfigItem.LocaleGroup(
					localeId = key,
					title = getLocaleTitle(key),
					isExpanded = isExpanded,
				)
				if (isExpanded) {
					list.mapTo(result) {
						SourceConfigItem.SourceItem(
							source = it,
							summary = null,
							isEnabled = false,
							isDraggable = false,
						)
					}
				}
			}
		}
		items.value = result
	}

	private fun getLocaleTitle(localeKey: String?): String? {
		val locale = Locale(localeKey ?: return null)
		return locale.getDisplayLanguage(locale).toTitleCase(locale)
	}

	private fun launchAtomicJob(
		context: CoroutineContext = EmptyCoroutineContext,
		block: suspend CoroutineScope.() -> Unit
	) = launchJob(start = CoroutineStart.ATOMIC) {
		mutex.withLock {
			withContext(context, block)
		}
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
