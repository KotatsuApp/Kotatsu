package org.koitharu.kotatsu.details.ui

import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.acra.ACRA
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.utils.ext.iterator
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.setCurrentManga

class MangaDetailsDelegate(
	private val intent: MangaIntent,
	private val settings: AppSettings,
	private val mangaDataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val localMangaRepository: LocalMangaRepository,
) {

	private val mangaData = MutableStateFlow(intent.manga)

	val selectedBranch = MutableStateFlow<String?>(null)

	// Remote manga for saved and saved for remote
	val relatedManga = MutableStateFlow<Manga?>(null)
	val manga: StateFlow<Manga?>
		get() = mangaData
	val mangaId = intent.manga?.id ?: intent.mangaId

	suspend fun doLoad() {
		var manga = mangaDataRepository.resolveIntent(intent)
			?: throw MangaNotFoundException("Cannot find manga")
		ACRA.setCurrentManga(manga)
		mangaData.value = manga
		manga = MangaRepository(manga.source).getDetails(manga)
		// find default branch
		val hist = historyRepository.getOne(manga)
		selectedBranch.value = if (hist != null) {
			val currentChapter = manga.chapters?.find { it.id == hist.chapterId }
			if (currentChapter != null) currentChapter.branch else predictBranch(manga.chapters)
		} else {
			predictBranch(manga.chapters)
		}
		mangaData.value = manga
		relatedManga.value = runCatching {
			if (manga.source == MangaSource.LOCAL) {
				val m = localMangaRepository.getRemoteManga(manga) ?: return@runCatching null
				MangaRepository(m.source).getDetails(m)
			} else {
				localMangaRepository.findSavedManga(manga)
			}
		}.onFailure { error ->
			error.printStackTraceDebug()
		}.getOrNull()
	}

	fun mapChapters(
		manga: Manga?,
		related: Manga?,
		history: MangaHistory?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val chapters = manga?.chapters ?: return emptyList()
		val relatedChapters = related?.chapters
		return if (related?.source != MangaSource.LOCAL && !relatedChapters.isNullOrEmpty()) {
			mapChaptersWithSource(chapters, relatedChapters, history?.chapterId, newCount, branch)
		} else {
			mapChapters(chapters, relatedChapters, history?.chapterId, newCount, branch)
		}
	}

	private fun mapChapters(
		chapters: List<MangaChapter>,
		downloadedChapters: List<MangaChapter>?,
		currentId: Long?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val result = ArrayList<ChapterListItem>(chapters.size)
		val dateFormat = settings.getDateFormat()
		val currentIndex = chapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = chapters.size - newCount
		val downloadedIds = downloadedChapters?.mapToSet { it.id }
		for (i in chapters.indices) {
			val chapter = chapters[i]
			if (chapter.branch != branch) {
				continue
			}
			result += chapter.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = false,
				isDownloaded = downloadedIds?.contains(chapter.id) == true,
				dateFormat = dateFormat,
			)
		}
		return result
	}

	private fun mapChaptersWithSource(
		chapters: List<MangaChapter>,
		sourceChapters: List<MangaChapter>,
		currentId: Long?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val chaptersMap = chapters.associateByTo(HashMap(chapters.size)) { it.id }
		val result = ArrayList<ChapterListItem>(sourceChapters.size)
		val currentIndex = sourceChapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = sourceChapters.size - newCount
		val dateFormat = settings.getDateFormat()
		for (i in sourceChapters.indices) {
			val chapter = sourceChapters[i]
			val localChapter = chaptersMap.remove(chapter.id)
			if (chapter.branch != branch) {
				continue
			}
			result += localChapter?.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = false,
				isDownloaded = false,
				dateFormat = dateFormat,
			) ?: chapter.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = true,
				isDownloaded = false,
				dateFormat = dateFormat,
			)
		}
		if (chaptersMap.isNotEmpty()) { // some chapters on device but not online source
			result.ensureCapacity(result.size + chaptersMap.size)
			chaptersMap.values.mapNotNullTo(result) {
				if (it.branch == branch) {
					it.toListItem(
						isCurrent = false,
						isUnread = true,
						isNew = false,
						isMissing = false,
						isDownloaded = false,
						dateFormat = dateFormat,
					)
				} else {
					null
				}
			}
			result.sortBy { it.chapter.number }
		}
		return result
	}

	private fun predictBranch(chapters: List<MangaChapter>?): String? {
		if (chapters.isNullOrEmpty()) {
			return null
		}
		val groups = chapters.groupBy { it.branch }
		for (locale in LocaleListCompat.getAdjustedDefault()) {
			var language = locale.getDisplayLanguage(locale).toTitleCase(locale)
			if (groups.containsKey(language)) {
				return language
			}
			language = locale.getDisplayName(locale).toTitleCase(locale)
			if (groups.containsKey(language)) {
				return language
			}
		}
		return groups.maxByOrNull { it.value.size }?.key
	}
}