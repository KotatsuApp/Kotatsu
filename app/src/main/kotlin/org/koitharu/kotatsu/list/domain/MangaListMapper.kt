package org.koitharu.kotatsu.list.domain

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.IntDef
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
import org.koitharu.kotatsu.local.data.index.LocalMangaIndex
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
	private val localMangaIndex: LocalMangaIndex,
) {

	private val dict by lazy { readTagsDict(context) }

	suspend fun toListModelList(
		manga: Collection<Manga>,
		mode: ListMode,
		@Flags flags: Int
	): List<MangaListModel> = manga.map {
		toListModel(it, mode, flags)
	}

	suspend fun toListModelList(
		destination: MutableCollection<in MangaListModel>,
		manga: Collection<Manga>,
		mode: ListMode,
		@Flags flags: Int,
	) {
		manga.mapTo(destination) {
			toListModel(it, mode, flags)
		}
	}

	suspend fun toListModel(
		manga: Manga,
		mode: ListMode,
		@Flags flags: Int
	): MangaListModel = when (mode) {
		ListMode.LIST -> toCompactListModel(manga, flags)
		ListMode.DETAILED_LIST -> toDetailedListModel(manga, flags)
		ListMode.GRID -> toGridModel(manga, flags)
	}

	suspend fun toCompactListModel(manga: Manga, @Flags flags: Int) = MangaCompactListModel(
		id = manga.id,
		title = manga.title,
		subtitle = manga.tags.joinToString(", ") { it.title },
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id, flags),
		progress = getProgress(manga.id, flags),
		isFavorite = isFavorite(manga.id, flags),
		isSaved = isSaved(manga.id, flags),
	)

	suspend fun toDetailedListModel(manga: Manga, @Flags flags: Int) = MangaDetailedListModel(
		id = manga.id,
		title = manga.title,
		subtitle = manga.altTitle,
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id, flags),
		progress = getProgress(manga.id, flags),
		isFavorite = isFavorite(manga.id, flags),
		isSaved = isSaved(manga.id, flags),
		tags = mapTags(manga.tags),
	)

	suspend fun toGridModel(manga: Manga, @Flags flags: Int) = MangaGridModel(
		id = manga.id,
		title = manga.title,
		coverUrl = manga.coverUrl,
		manga = manga,
		counter = getCounter(manga.id, flags),
		progress = getProgress(manga.id, flags),
		isFavorite = isFavorite(manga.id, flags),
		isSaved = isSaved(manga.id, flags),
	)

	fun mapTags(tags: Collection<MangaTag>) = tags.map {
		ChipsView.ChipModel(
			tint = getTagTint(it),
			title = it.title,
			data = it,
		)
	}

	private suspend fun getCounter(mangaId: Long, @Flags flags: Int): Int {
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(mangaId)
		} else {
			0
		}
	}

	private suspend fun getProgress(mangaId: Long, @Flags flags: Int): ReadingProgress? {
		return if (flags.hasNoFlag(NO_PROGRESS)) {
			historyRepository.getProgress(mangaId, settings.progressIndicatorMode)
		} else {
			null
		}
	}

	private suspend fun isFavorite(mangaId: Long, @Flags flags: Int): Boolean {
		return flags.hasNoFlag(NO_FAVORITE) && favouritesRepository.isFavorite(mangaId)
	}

	private suspend fun isSaved(mangaId: Long, @Flags flags: Int): Boolean {
		return flags.hasNoFlag(NO_SAVED) && mangaId in localMangaIndex
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

	private fun Int.hasNoFlag(flag: Int) = this and flag == 0


	@IntDef(0, NO_SAVED, NO_PROGRESS, NO_FAVORITE)
	@Retention(AnnotationRetention.SOURCE)
	annotation class Flags

	companion object {

		const val NO_SAVED = 1
		const val NO_PROGRESS = 2
		const val NO_FAVORITE = 4
	}
}
