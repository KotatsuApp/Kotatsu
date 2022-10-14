package org.koitharu.kotatsu.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaWithTags

@Dao
abstract class TracksDao {

	@Query("SELECT * FROM tracks")
	abstract suspend fun findAll(): List<TrackEntity>

	@Query("SELECT * FROM tracks WHERE manga_id IN (:ids)")
	abstract suspend fun findAll(ids: Collection<Long>): List<TrackEntity>

	@Query("SELECT * FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun find(mangaId: Long): TrackEntity?

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun findNewChapters(mangaId: Long): Int?

	@MapInfo(keyColumn = "manga_id", valueColumn = "chapters_new")
	@Query("SELECT manga_id, chapters_new FROM tracks")
	abstract fun observeNewChaptersMap(): Flow<Map<Long, Int>>

	@Query("SELECT chapters_new FROM tracks")
	abstract fun observeNewChapters(): Flow<List<Int>>

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract fun observeNewChapters(mangaId: Long): Flow<Int?>

	@Transaction
	@MapInfo(valueColumn = "chapters_new")
	@Query("SELECT manga.*, chapters_new FROM tracks LEFT JOIN manga ON manga.manga_id = tracks.manga_id WHERE chapters_new > 0 ORDER BY chapters_new DESC")
	abstract fun observeUpdatedManga(): Flow<Map<MangaWithTags, Int>>

	@Query("DELETE FROM tracks")
	abstract suspend fun clear()

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: TrackEntity): Long

	@Update
	abstract suspend fun update(entity: TrackEntity): Int

	@Query("DELETE FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long)

	@Query("DELETE FROM tracks WHERE manga_id NOT IN (SELECT manga_id FROM history UNION SELECT manga_id FROM favourites)")
	abstract suspend fun gc()

	@Transaction
	open suspend fun upsert(entity: TrackEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}
}
