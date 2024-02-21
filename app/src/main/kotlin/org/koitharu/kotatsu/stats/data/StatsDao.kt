package org.koitharu.kotatsu.stats.data

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.parsers.model.Manga

@Dao
abstract class StatsDao {

	@Query("SELECT * FROM stats ORDER BY started_at")
	abstract suspend fun findAll(): List<StatsEntity>

	@Query("SELECT * FROM stats WHERE manga_id = :mangaId ORDER BY started_at")
	abstract suspend fun findAll(mangaId: Long): List<StatsEntity>

	@Query("SELECT IFNULL(SUM(pages),0) FROM stats WHERE manga_id = :mangaId")
	abstract suspend fun getReadPagesCount(mangaId: Long): Int

	@Query("SELECT IFNULL(SUM(duration)/SUM(pages), 0) FROM stats WHERE manga_id = :mangaId")
	abstract suspend fun getAverageTimePerPage(mangaId: Long): Long

	@Query("SELECT IFNULL(SUM(duration), 0) FROM stats WHERE manga_id = :mangaId")
	abstract suspend fun getReadingTime(mangaId: Long): Long

	@Query("SELECT IFNULL(SUM(duration), 0) FROM stats")
	abstract suspend fun getTotalReadingTime(): Long

	@Query("SELECT manga_id, SUM(duration) AS d FROM stats GROUP BY manga_id ORDER BY d")
	abstract suspend fun getDurationStats(): Map<@MapColumn("manga_id") Long, @MapColumn("d") Long>

	@Upsert
	abstract suspend fun upsert(entity: StatsEntity)
}
