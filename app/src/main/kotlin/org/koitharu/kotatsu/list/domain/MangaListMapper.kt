package org.koitharu.kotatsu.list.domain

import android.content.Context
import androidx.annotation.ColorRes
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.ui.model.MangaCompactListModel
import org.koitharu.kotatsu.list.ui.model.MangaDetailedListModel
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaListMapper @Inject constructor(
	@ApplicationContext context: Context,
	private val settings: AppSettings,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
) {

	private val dict by lazy { readTagsDict(context) }

	suspend fun toListModelList(manga: Collection<Manga>, mode: ListMode): List<MangaListModel> = manga.map {
		toListModel(it, mode)
	}

	suspend fun toListModelList(
		destination: MutableCollection<in MangaListModel>,
		manga: Collection<Manga>,
		mode: ListMode
	) = manga.mapTo(destination) {
		toListModel(it, mode)
	}

	suspend fun toListModel(manga: Manga, mode: ListMode): MangaListModel = when (mode) {
		ListMode.LIST -> toCompactListModel(manga)
		ListMode.DETAILED_LIST -> toDetailedListModel(manga)
		ListMode.GRID -> toGridModel(manga)
	}

	suspend fun toCompactListModel(manga: Manga) = MangaCompactListModel(
		id = manga.id,
		title = manga.title,
		subtitle = manga.tags.joinToString(", ") { it.title },
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id),
		progress = getProgress(manga.id),
		isFavorite = isFavorite(manga.id),
	)

	suspend fun toDetailedListModel(manga: Manga) = MangaDetailedListModel(
		id = manga.id,
		title = manga.title,
		subtitle = manga.altTitle,
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id),
		progress = getProgress(manga.id),
		isFavorite = isFavorite(manga.id),
		tags = mapTags(manga.tags),
	)

	suspend fun toGridModel(manga: Manga) = MangaGridModel(
		id = manga.id,
		title = manga.title,
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id),
		progress = getProgress(manga.id),
		isFavorite = isFavorite(manga.id),
	)

	fun mapTags(tags: Collection<MangaTag>) = tags.map {
		ChipsView.ChipModel(
			tint = getTagTint(it),
			title = it.title,
			data = it,
		)
	}

	private suspend fun getCounter(mangaId: Long): Int {
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(mangaId)
		} else {
			0
		}
	}

	private suspend fun getProgress(mangaId: Long): ReadingProgress? {
		return historyRepository.getProgress(mangaId, settings.progressIndicatorMode)
	}

	private fun isFavorite(mangaId: Long): Boolean {
		return false // TODO favouritesRepository.isFavorite(mangaId)
	}

	@ColorRes
	private fun getTagTint(tag: MangaTag): Int {
		return if (tag.title.lowercase() in dict) {
			R.color.warning
		} else {
			0
		}
	}

	private fun readTagsDict(context: Context): ScatterSet<String> =
		context.resources.openRawResource(R.raw.tags_redlist).use {
			val set = MutableScatterSet<String>()
			it.bufferedReader().forEachLine { x ->
				val line = x.trim()
				if (line.isNotEmpty()) {
					set.add(line)
				}
			}
			set.trim()
			set
		}
}
