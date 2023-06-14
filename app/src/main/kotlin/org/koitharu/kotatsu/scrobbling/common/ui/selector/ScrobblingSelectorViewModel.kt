package org.koitharu.kotatsu.scrobbling.common.ui.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.NO_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.ui.selector.model.ScrobblerHint
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import javax.inject.Inject

@HiltViewModel
class ScrobblingSelectorViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(MangaIntent.KEY_MANGA).manga

	val availableScrobblers = scrobblers.filter { it.isAvailable }

	val selectedScrobblerIndex = MutableStateFlow(0)

	private val scrobblerMangaList = MutableStateFlow<List<ScrobblerManga>>(emptyList())
	private val hasNextPage = MutableStateFlow(true)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private var doneJob: Job? = null
	private var initJob: Job? = null

	private val currentScrobbler: Scrobbler
		get() = availableScrobblers[selectedScrobblerIndex.requireValue()]

	val content: StateFlow<List<ListModel>> = combine(
		scrobblerMangaList,
		listError,
		hasNextPage,
	) { list, error, isHasNextPage ->
		if (list.isNotEmpty()) {
			if (isHasNextPage) {
				list + LoadingFooter()
			} else {
				list
			}
		} else {
			listOf(
				when {
					error != null -> errorHint(error)
					isHasNextPage -> LoadingFooter()
					else -> emptyResultsHint()
				},
			)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	val selectedItemId = MutableStateFlow(NO_ID)
	val searchQuery = MutableStateFlow(manga.title)
	val onClose = MutableEventFlow<Unit>()

	val isEmpty: Boolean
		get() = scrobblerMangaList.value.isEmpty()

	init {
		initialize()
	}

	fun search(query: String) {
		loadingJob?.cancel()
		searchQuery.value = query
		loadList(append = false)
	}

	fun loadNextPage() {
		if (scrobblerMangaList.value.isNotEmpty() && hasNextPage.value) {
			loadList(append = true)
		}
	}

	fun retry() {
		loadingJob?.cancel()
		hasNextPage.value = true
		scrobblerMangaList.value = emptyList()
		loadList(append = false)
	}

	private fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			listError.value = null
			val offset = if (append) scrobblerMangaList.value.size else 0
			runCatchingCancellable {
				currentScrobbler.findManga(checkNotNull(searchQuery.value), offset)
			}.onSuccess { list ->
				if (!append) {
					scrobblerMangaList.value = list
				} else if (list.isNotEmpty()) {
					scrobblerMangaList.value = scrobblerMangaList.value + list
				}
				hasNextPage.value = list.isNotEmpty()
			}.onFailure { error ->
				error.printStackTraceDebug()
				listError.value = error
			}
		}
	}

	fun onDoneClick() {
		if (doneJob?.isActive == true) {
			return
		}
		val targetId = selectedItemId.value
		if (targetId == NO_ID) {
			onClose.call(Unit)
		}
		doneJob = launchJob(Dispatchers.Default) {
			currentScrobbler.linkManga(manga.id, targetId)
			onClose.call(Unit)
		}
	}

	fun setScrobblerIndex(index: Int) {
		if (index == selectedScrobblerIndex.value || index !in availableScrobblers.indices) return
		selectedScrobblerIndex.value = index
		initialize()
	}

	private fun initialize() {
		initJob?.cancel()
		loadingJob?.cancel()
		hasNextPage.value = true
		scrobblerMangaList.value = emptyList()
		initJob = launchJob(Dispatchers.Default) {
			try {
				val info = currentScrobbler.getScrobblingInfoOrNull(manga.id)
				if (info != null) {
					selectedItemId.value = info.targetId
				}
			} finally {
				loadList(append = false)
			}
		}
	}

	private fun emptyResultsHint() = ScrobblerHint(
		icon = R.drawable.ic_empty_history,
		textPrimary = R.string.nothing_found,
		textSecondary = R.string.text_search_holder_secondary,
		error = null,
		actionStringRes = R.string.search,
	)

	private fun errorHint(e: Throwable) = ScrobblerHint(
		icon = R.drawable.ic_error_large,
		textPrimary = R.string.error_occurred,
		error = e,
		textSecondary = 0,
		actionStringRes = R.string.try_again,
	)
}
