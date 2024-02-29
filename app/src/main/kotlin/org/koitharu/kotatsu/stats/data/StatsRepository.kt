package org.koitharu.kotatsu.stats.data

import androidx.room.withTransaction
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.stats.domain.StatsRecord
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatsRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	suspend fun getReadingStats(): List<StatsRecord> = db.withTransaction {
		val stats = db.getStatsDao().getDurationStats()
		val minute = TimeUnit.MINUTES.toMillis(1)
		val mangaDao = db.getMangaDao()
		val result = ArrayList<StatsRecord>(stats.size)
		var other = StatsRecord(null, 0)
		for ((mangaId, duration) in stats) {
			val manga = mangaDao.find(mangaId)?.toManga()
			if (manga == null || duration < minute) {
				other = other.copy(duration = other.duration + duration)
			} else {
				result += StatsRecord(
					manga = manga,
					duration = duration,
				)
			}
		}
		if (other.duration != 0L) {
			result += other
		}
		result
	}

	suspend fun getTimePerPage(mangaId: Long): Long = db.withTransaction {
		val dao = db.getStatsDao()
		val pages = dao.getReadPagesCount(mangaId)
		val time = if (pages >= 10) {
			dao.getAverageTimePerPage(mangaId)
		} else {
			dao.getAverageTimePerPage()
		}
		time
	}
}
