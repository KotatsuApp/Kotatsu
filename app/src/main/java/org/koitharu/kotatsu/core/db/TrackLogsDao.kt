package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.TrackLogEntity
import org.koitharu.kotatsu.core.db.entity.TrackLogWithManga

@Dao
interface TrackLogsDao {

	@Transaction
	@Query("SELECT * FROM track_logs ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
	suspend fun findAll(offset: Int, limit: Int): List<TrackLogWithManga>

	@Query("DELETE FROM track_logs")
	suspend fun clear()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(entity: TrackLogEntity): Long

	@Query("DELETE FROM track_logs WHERE manga_id = :mangaId")
	suspend fun removeAll(mangaId: Long)
}