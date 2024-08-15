package org.koitharu.kotatsu.list.ui.preview

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaListMapper: MangaListMapper,
	private val repositoryFactory: MangaRepository.Factory,
	private val historyRepository: HistoryRepository,
	private val imageGetter: Html.ImageGetter,
) : BaseViewModel() {

	val manga = MutableStateFlow(
		savedStateHandle.require<ParcelableManga>(MangaIntent.KEY_MANGA).manga,
	)

	val footer = combine(
		manga,
		historyRepository.observeOne(manga.value.id),
		manga.flatMapLatest { historyRepository.observeShouldSkip(it) }.distinctUntilChanged(),
	) { m, history, incognito ->
		if (m.chapters == null) {
			return@combine null
		}
		val b = m.getPreferredBranch(history)
		val chapters = m.getChapters(b).orEmpty()
		FooterInfo(
			percent = history?.percent ?: PROGRESS_NONE,
			currentChapter = history?.chapterId?.let {
				chapters.indexOfFirst { x -> x.id == it }
			} ?: -1,
			totalChapters = chapters.size,
			isIncognito = incognito,
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, null)

	val description = manga
		.distinctUntilChangedBy { it.description.orEmpty() }
		.transformLatest {
			val description = it.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans().sanitize())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.combine(isLoading) { desc, loading ->
			if (loading) null else desc ?: ""
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), null)

	val tagsChips = manga.map {
		mangaListMapper.mapTags(it.tags)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	init {
		launchLoadingJob(Dispatchers.Default) {
			val repo = repositoryFactory.create(manga.value.source)
			manga.value = repo.getDetails(manga.value)
		}
	}

	private fun Spanned.filterSpans(): CharSequence {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable.trim()
	}

	data class FooterInfo(
		val currentChapter: Int,
		val totalChapters: Int,
		val isIncognito: Boolean,
		val percent: Float,
	) {

		fun isInProgress() = currentChapter >= 0
	}
}
