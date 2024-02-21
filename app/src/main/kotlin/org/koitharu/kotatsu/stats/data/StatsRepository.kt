package org.koitharu.kotatsu.stats.data

import androidx.collection.ArrayMap
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.stats.domain.StatsRecord
import java.util.Date
import javax.inject.Inject

class StatsRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	suspend fun getReadingStats(): List<StatsRecord> = db.withTransaction {
		val stats = db.getStatsDao().getDurationStats()
		val mangaDao = db.getMangaDao()
		val result = ArrayList<StatsRecord>(stats.size)
		for ((mangaId, duration) in stats) {
			val manga = mangaDao.find(mangaId)?.toManga() ?: continue
			result += StatsRecord(
				manga = manga,
				duration = duration,
			)
		}
		result
	}
}
