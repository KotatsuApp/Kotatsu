package org.koitharu.kotatsu.core.db.dao

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.TrackEntity

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