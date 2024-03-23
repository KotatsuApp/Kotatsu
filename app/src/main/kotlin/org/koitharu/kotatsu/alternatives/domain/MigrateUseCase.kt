package org.koitharu.kotatsu.alternatives.domain

import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.details.domain.ProgressUpdateUseCase
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.history.data.toMangaHistory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

class MigrateUseCase @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaDataRepository: MangaDataRepository,
	private val database: MangaDatabase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
) {

	suspend operator fun invoke(oldManga: Manga, newManga: Manga) {
		val oldDetails = if (oldManga.chapters.isNullOrEmpty()) {
			runCatchingCancellable {
				mangaRepositoryFactory.create(oldManga.source).getDetails(oldManga)
			}.getOrDefault(oldManga)
		} else {
			oldManga
		}
		val newDetails = if (newManga.chapters.isNullOrEmpty()) {
			mangaRepositoryFactory.create(newManga.source).getDetails(newManga)
		} else {
			newManga
		}
		mangaDataRepository.storeManga(newDetails)
		database.withTransaction {
			// replace favorites
			val favoritesDao = database.getFavouritesDao()
			val oldFavourites = favoritesDao.findAllRaw(oldDetails.id)
			if (oldFavourites.isNotEmpty()) {
				favoritesDao.delete(oldManga.id)
				for (f in oldFavourites) {
					val e = f.copy(
						mangaId = newManga.id,
					)
					favoritesDao.upsert(e)
				}
			}
			// replace history
			val historyDao = database.getHistoryDao()
			val oldHistory = historyDao.find(oldDetails.id)
			if (oldHistory != null) {
				val newHistory = makeNewHistory(oldDetails, newDetails, oldHistory)
				historyDao.delete(oldDetails.id)
				historyDao.upsert(newHistory)
			}
		}
		progressUpdateUseCase(newManga)
	}

	private fun makeNewHistory(
		oldManga: Manga,
		newManga: Manga,
		history: HistoryEntity,
	): HistoryEntity {
		if (oldManga.chapters.isNullOrEmpty()) { // probably broken manga/source
			val branch = newManga.getPreferredBranch(null)
			val chapters = checkNotNull(newManga.getChapters(branch))
			val currentChapter = if (history.percent in 0f..1f) {
				chapters[(chapters.lastIndex * history.percent).toInt()]
			} else {
				chapters.first()
			}
			return HistoryEntity(
				mangaId = newManga.id,
				createdAt = history.createdAt,
				updatedAt = System.currentTimeMillis(),
				chapterId = currentChapter.id,
				page = history.page,
				scroll = history.scroll,
				percent = history.percent,
				deletedAt = 0,
				chaptersCount = chapters.size,
			)
		}
		val branch = oldManga.getPreferredBranch(history.toMangaHistory())
		val oldChapters = checkNotNull(oldManga.getChapters(branch))
		var index = oldChapters.indexOfFirst { it.id == history.chapterId }
		if (index < 0) {
			index = if (history.percent in 0f..1f) {
				(oldChapters.lastIndex * history.percent).toInt()
			} else {
				0
			}
		}
		val newChapters = checkNotNull(newManga.chapters).groupBy { it.branch }
		val newBranch = if (newChapters.containsKey(branch)) {
			branch
		} else {
			newManga.getPreferredBranch(null)
		}
		val newChapterId = checkNotNull(newChapters[newBranch]).let {
			val oldChapter = oldChapters[index]
			it.findByNumber(oldChapter.volume, oldChapter.number) ?: it.getOrNull(index) ?: it.last()
		}.id

		return HistoryEntity(
			mangaId = newManga.id,
			createdAt = history.createdAt,
			updatedAt = System.currentTimeMillis(),
			chapterId = newChapterId,
			page = history.page,
			scroll = history.scroll,
			percent = PROGRESS_NONE,
			deletedAt = 0,
			chaptersCount = checkNotNull(newChapters[newBranch]).size,
		)
	}

	private fun List<MangaChapter>.findByNumber(volume: Int, number: Float): MangaChapter? {
		return if (number <= 0f) {
			null
		} else {
			firstOrNull { it.volume == volume && it.number == number }
		}
	}
}
