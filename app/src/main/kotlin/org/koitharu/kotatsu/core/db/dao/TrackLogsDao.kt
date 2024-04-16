package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.tracker.data.TrackLogEntity
import org.koitharu.kotatsu.tracker.data.TrackLogWithManga

@Dao
interface TrackLogsDao {

	@Transaction
	@Query("SELECT * FROM track_logs ORDER BY created_at DESC LIMIT :limit OFFSET 0")
	fun observeAll(limit: Int): Flow<List<TrackLogWithManga>>

	@Query("SELECT COUNT(*) FROM track_logs WHERE unread = 1")
	fun observeUnreadCount(): Flow<Int>

	@Query("DELETE FROM track_logs")
	suspend fun clear()

	@Query("UPDATE track_logs SET unread = 0 WHERE id = :id")
	suspend fun markAsRead(id: Long)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(entity: TrackLogEntity): Long

	@Query("DELETE FROM track_logs WHERE manga_id = :mangaId")
	suspend fun removeAll(mangaId: Long)

	@Query("DELETE FROM track_logs WHERE manga_id NOT IN (SELECT manga_id FROM tracks)")
	suspend fun gc()

	@Query("DELETE FROM track_logs WHERE id IN (SELECT id FROM track_logs ORDER BY created_at DESC LIMIT 0 OFFSET :size)")
	suspend fun trim(size: Int)

	@Query("SELECT COUNT(*) FROM track_logs")
	suspend fun count(): Int
}
