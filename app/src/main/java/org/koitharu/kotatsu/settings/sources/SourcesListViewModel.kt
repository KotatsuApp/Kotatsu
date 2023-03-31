package org.koitharu.kotatsu.settings.sources

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.model.getLocaleTitle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.map
import org.koitharu.kotatsu.utils.ext.move
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
) : BaseViewModel() {

	val items = MutableLiveData<List<SourceConfigItem>>(emptyList())
	val onActionDone = SingleLiveEvent<ReversibleAction>()
	private val mutex = Mutex()

	private val expandedGroups = HashSet<String?>()
	private var searchQuery: String? = null

	init {
		launchAtomicJob(Dispatchers.Default) {
			buildList()
		}
	}

	fun reorderSources(oldPos: Int, newPos: Int): Boolean {
		val snapshot = items.value?.toMutableList() ?: return false
		if ((snapshot[oldPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		if ((snapshot[newPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		launchAtomicJob(Dispatchers.Default) {
			snapshot.move(oldPos, newPos)
			settings.sourcesOrder = snapshot.mapNotNull {
				(it as? SourceConfigItem.SourceItem)?.source?.name
			}
			buildList()
		}
		return true
	}

	fun canReorder(oldPos: Int, newPos: Int): Boolean {
		val snapshot = items.value?.toMutableList() ?: return false
		if ((snapshot[oldPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		if ((snapshot[newPos] as? SourceConfigItem.SourceItem)?.isEnabled != true) return false
		return true
	}

	fun setEnabled(source: MangaSource, isEnabled: Boolean) {
		launchAtomicJob(Dispatchers.Default) {
			settings.hiddenSources = if (isEnabled) {
				settings.hiddenSources - source.name
			} else {
				settings.hiddenSources + source.name
			}
			if (isEnabled) {
				settings.markKnownSources(setOf(source))
			} else {
				val rollback = ReversibleHandle {
					setEnabled(source, true)
				}
				onActionDone.postCall(ReversibleAction(R.string.source_disabled, rollback))
			}
			buildList()
		}
	}

	fun disableAll() {
		launchAtomicJob(Dispatchers.Default) {
			settings.hiddenSources = settings.getMangaSources(includeHidden = true).mapToSet {
				it.name
			}
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

	private suspend fun buildList() = runInterruptible(Dispatchers.Default) {
		val sources = settings.getMangaSources(includeHidden = true)
		val hiddenSources = settings.hiddenSources
		val query = searchQuery
		if (!query.isNullOrEmpty()) {
			items.postValue(
				sources.mapNotNull {
					if (!it.title.contains(query, ignoreCase = true)) {
						return@mapNotNull null
					}
					SourceConfigItem.SourceItem(
						source = it,
						summary = it.getLocaleTitle(),
						isEnabled = it.name !in hiddenSources,
						isDraggable = false,
					)
				}.ifEmpty {
					listOf(SourceConfigItem.EmptySearchResult)
				},
			)
			return@runInterruptible
		}
		val map = sources.groupByTo(TreeMap(LocaleKeyComparator())) {
			if (it.name !in hiddenSources) {
				KEY_ENABLED
			} else {
				it.locale
			}
		}
		val result = ArrayList<SourceConfigItem>(sources.size + map.size + 2)
		val enabledSources = map.remove(KEY_ENABLED)
		if (!enabledSources.isNullOrEmpty()) {
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
		if (enabledSources?.size != sources.size) {
			result += SourceConfigItem.Header(R.string.available_sources)
			for ((key, list) in map) {
				list.sortBy { it.ordinal }
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
		items.postValue(result)
	}

	private fun getLocaleTitle(localeKey: String?): String? {
		val locale = Locale(localeKey ?: return null)
		return locale.getDisplayLanguage(locale).toTitleCase(locale)
	}

	private inline fun launchAtomicJob(
		context: CoroutineContext = EmptyCoroutineContext,
		crossinline block: suspend CoroutineScope.() -> Unit
	) = launchJob(context) {
		mutex.withLock {
			block()
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
