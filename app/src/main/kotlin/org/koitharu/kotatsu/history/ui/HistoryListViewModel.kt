package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.calculateTimeAgo
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.combine
import org.koitharu.kotatsu.core.util.ext.onFirst
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.domain.MarkAsReadUseCase
import org.koitharu.kotatsu.history.domain.model.MangaWithHistory
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyHint
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.QuickFilter
import org.koitharu.kotatsu.list.ui.model.TipModel
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import java.time.Instant
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val PAGE_SIZE = 20

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	settings: AppSettings,
	private val mangaListMapper: MangaListMapper,
	private val localMangaRepository: LocalMangaRepository,
	private val markAsReadUseCase: MarkAsReadUseCase,
	networkState: NetworkState,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	private val sortOrder: StateFlow<ListSortOrder> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_HISTORY_ORDER,
		valueProducer = { historySortOrder },
	)

	private val filterOptions = MutableStateFlow<Set<ListFilterOption>>(EnumSet.noneOf(ListFilterOption::class.java))

	override val listMode = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_LIST_MODE_HISTORY,
		valueProducer = { historyListMode },
	)

	private val isGroupingEnabled = settings.observeAsFlow(
		key = AppSettings.KEY_HISTORY_GROUPING,
		valueProducer = { isHistoryGroupingEnabled },
	).combine(sortOrder) { g, s ->
		g && s.isGroupingSupported()
	}

	private val limit = MutableStateFlow(PAGE_SIZE)
	private val isReady = AtomicBoolean(false)

	val isStatsEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_STATS_ENABLED,
		valueProducer = { isStatsEnabled },
	)

	override val content = combine(
		filterOptions,
		observeHistory(),
		isGroupingEnabled,
		observeListModeWithTriggers(),
		networkState,
		settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) { isIncognitoModeEnabled },
	) { filters, list, grouped, mode, online, incognito ->
		when {
			list.isEmpty() -> {
				if (filters.isEmpty()) {
					listOf(getEmptyState(hasFilters = false))
				} else {
					listOf(filterItem(filters), getEmptyState(hasFilters = true))
				}
			}

			else -> {
				isReady.set(true)
				mapList(filters, list, grouped, mode, online, incognito)
			}
		}
	}.onStart {
		loadingCounter.increment()
	}.onFirst {
		loadingCounter.decrement()
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory(minDate: Instant) {
		launchJob(Dispatchers.Default) {
			val stringRes = if (minDate <= Instant.EPOCH) {
				repository.clear()
				R.string.history_cleared
			} else {
				repository.deleteAfter(minDate.toEpochMilli())
				R.string.removed_from_history
			}
			onActionDone.call(ReversibleAction(stringRes, null))
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.delete(ids)
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun markAsRead(items: Set<Manga>) {
		launchLoadingJob(Dispatchers.Default) {
			markAsReadUseCase(items)
		}
	}

	fun requestMoreItems() {
		if (isReady.compareAndSet(true, false)) {
			limit.value += PAGE_SIZE
		}
	}

	fun onFilterOptionClick(option: ListFilterOption) {
		filterOptions.value = EnumSet.copyOf(filterOptions.value).also {
			if (option in it) {
				it.remove(option)
			} else {
				it.add(option)
			}
		}
	}

	private fun observeHistory() = combine(sortOrder, filterOptions, limit, ::Triple)
		.flatMapLatest { repository.observeAllWithHistory(it.first, it.second - ListFilterOption.DOWNLOADED, it.third) }

	private suspend fun mapList(
		filters: Set<ListFilterOption>,
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode,
		isOnline: Boolean,
		isIncognito: Boolean,
	): List<ListModel> {
		val result = ArrayList<ListModel>((if (grouped) (list.size * 1.4).toInt() else list.size) + 3)
		result += filterItem(filters)
		if (isIncognito) {
			result += TipModel(
				key = AppSettings.KEY_INCOGNITO_MODE,
				title = R.string.incognito_mode,
				text = R.string.incognito_mode_hint,
				icon = R.drawable.ic_incognito,
				primaryButtonText = 0,
				secondaryButtonText = 0,
			)
		}
		val order = sortOrder.value
		var prevHeader: ListHeader? = null
		if (!isOnline) {
			result += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.network_unavailable,
				textSecondary = R.string.network_unavailable_hint,
				actionStringRes = R.string.manage,
			)
		}
		var isEmpty = true
		for ((m, history) in list) {
			val manga = if ((!isOnline && !m.isLocal) || ListFilterOption.DOWNLOADED in filters) {
				localMangaRepository.findSavedManga(m)?.manga ?: continue
			} else {
				m
			}
			isEmpty = false
			if (grouped) {
				val header = history.header(order)
				if (header != prevHeader) {
					if (header != null) {
						result += header
					}
					prevHeader = header
				}
			}
			result += mangaListMapper.toListModel(manga, mode)
		}
		if (filters.isNotEmpty() && isEmpty) {
			result += getEmptyState(hasFilters = true)
		}
		return result
	}

	private fun MangaHistory.header(order: ListSortOrder): ListHeader? = when (order) {
		ListSortOrder.LAST_READ,
		ListSortOrder.LONG_AGO_READ -> ListHeader(calculateTimeAgo(updatedAt))

		ListSortOrder.OLDEST,
		ListSortOrder.NEWEST -> ListHeader(calculateTimeAgo(createdAt))

		ListSortOrder.UNREAD,
		ListSortOrder.PROGRESS -> ListHeader(
			when (percent) {
				1f -> R.string.status_completed
				in 0f..0.01f -> R.string.status_planned
				in 0f..1f -> R.string.status_reading
				else -> R.string.unknown
			},
		)

		ListSortOrder.ALPHABETIC,
		ListSortOrder.ALPHABETIC_REVERSE,
		ListSortOrder.RELEVANCE,
		ListSortOrder.NEW_CHAPTERS,
		ListSortOrder.UPDATED,
		ListSortOrder.RATING -> null
	}

	private fun filterItem(selected: Set<ListFilterOption>) = QuickFilter(
		items = ListFilterOption.HISTORY.map { option ->
			ChipsView.ChipModel(
				titleResId = option.titleResId,
				icon = option.iconResId,
				isCheckable = true,
				isChecked = option in selected,
				data = option,
			)
		},
	)

	private fun getEmptyState(hasFilters: Boolean) = if (hasFilters) {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.nothing_found,
			textSecondary = R.string.text_history_holder_secondary_filtered,
			actionStringRes = 0,
		)
	} else {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.text_history_holder_primary,
			textSecondary = R.string.text_history_holder_secondary,
			actionStringRes = 0,
		)
	}
}
