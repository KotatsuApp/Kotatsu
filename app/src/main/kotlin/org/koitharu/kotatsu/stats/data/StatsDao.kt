package org.koitharu.kotatsu.stats.data

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StatsDao {

	@Query("SELECT * FROM stats ORDER BY started_at")
	suspend fun findAll(): List<StatsEntity>

	@Query("SELECT * FROM stats WHERE manga_id = :mangaId ORDER BY started_at")
	suspend fun findAll(mangaId: Long): List<StatsEntity>

	@Query("SELECT IFNULL(SUM(pages),0) FROM stats WHERE manga_id = :mangaId")
	suspend fun getReadPagesCount(mangaId: Long): Int

	@Query("SELECT IFNULL(SUM(duration)/SUM(pages), 0) FROM stats WHERE manga_id = :mangaId")
	suspend fun getAverageTimePerPage(mangaId: Long): Long

	@Query("SELECT IFNULL(SUM(duration)/SUM(pages), 0) FROM stats")
	suspend fun getAverageTimePerPage(): Long

	@Query("SELECT IFNULL(SUM(duration), 0) FROM stats WHERE manga_id = :mangaId")
	suspend fun getReadingTime(mangaId: Long): Long

	@Query("SELECT IFNULL(SUM(duration), 0) FROM stats")
	suspend fun getTotalReadingTime(): Long

	@Query("SELECT manga_id, SUM(duration) AS d FROM stats WHERE started_at >= :fromDate GROUP BY manga_id ORDER BY d DESC")
	suspend fun getDurationStats(fromDate: Long): Map<@MapColumn("manga_id") Long, @MapColumn("d") Long>

	@Query("DELETE FROM stats")
	suspend fun clear()

	@Upsert
	suspend fun upsert(entity: StatsEntity)
}
