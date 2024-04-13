package org.koitharu.kotatsu.tracker.data

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TracksDao {

	@Query("SELECT * FROM tracks")
	abstract suspend fun findAll(): List<TrackEntity>

	@Transaction
	@Query("SELECT * FROM tracks ORDER BY last_check_time ASC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<TrackWithManga>

	@Transaction
	@Query("SELECT * FROM tracks ORDER BY last_check_time DESC")
	abstract fun observeAll(): Flow<List<TrackWithManga>>

	@Query("SELECT manga_id FROM tracks")
	abstract suspend fun findAllIds(): LongArray

	@Query("SELECT * FROM tracks WHERE manga_id IN (:ids)")
	abstract suspend fun findAll(ids: Collection<Long>): List<TrackEntity>

	@Query("SELECT * FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun find(mangaId: Long): TrackEntity?

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun findNewChapters(mangaId: Long): Int?

	@Query("SELECT COUNT(*) FROM tracks")
	abstract suspend fun getTracksCount(): Int

	@Query("SELECT manga_id, chapters_new FROM tracks")
	abstract fun observeNewChaptersMap(): Flow<Map<@MapColumn(columnName = "manga_id") Long, @MapColumn(columnName = "chapters_new") Int>>

	@Query("SELECT chapters_new FROM tracks")
	abstract fun observeNewChapters(): Flow<List<Int>>

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract fun observeNewChapters(mangaId: Long): Flow<Int?>

	@Transaction
	@Query("SELECT * FROM tracks WHERE chapters_new > 0 ORDER BY last_chapter_date DESC")
	abstract fun observeUpdatedManga(): Flow<List<MangaWithTrack>>

	@Transaction
	@Query("SELECT * FROM tracks WHERE chapters_new > 0 ORDER BY last_chapter_date DESC LIMIT :limit")
	abstract fun observeUpdatedManga(limit: Int): Flow<List<MangaWithTrack>>

	@Query("DELETE FROM tracks")
	abstract suspend fun clear()

	@Query("UPDATE tracks SET chapters_new = 0")
	abstract suspend fun clearCounters()

	@Query("UPDATE tracks SET chapters_new = 0 WHERE manga_id = :mangaId")
	abstract suspend fun clearCounter(mangaId: Long)

	@Query("DELETE FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long)

	@Query("DELETE FROM tracks WHERE manga_id NOT IN (SELECT manga_id FROM history UNION SELECT manga_id FROM favourites WHERE category_id IN (SELECT category_id FROM favourite_categories WHERE track = 1))")
	abstract suspend fun gc()

	@Upsert
	abstract suspend fun upsert(entity: TrackEntity)
}
