package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.HistoryEntity
import org.koitharu.kotatsu.core.db.entity.HistoryWithManga


@Dao
abstract class HistoryDao {

    /**
     * @hide
     */
    @Transaction
    @Query("SELECT * FROM history ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun findAll(offset: Int, limit: Int): List<HistoryWithManga>

    @Query("SELECT * FROM history WHERE manga_id = :id")
    abstract suspend fun find(id: Long): HistoryEntity?

    @Query("DELETE FROM history")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: HistoryEntity): Long

    @Query("UPDATE history SET page = :page, chapter_id = :chapterId, updated_at = :updatedAt WHERE manga_id = :mangaId")
    abstract suspend fun update(mangaId: Long, page: Int, chapterId: Long, updatedAt: Long): Int

    @Query("DELETE FROM history WHERE manga_id = :mangaId")
    abstract suspend fun delete(mangaId: Long)

    suspend fun update(entity: HistoryEntity) = update(entity.mangaId, entity.page, entity.chapterId, entity.updatedAt)

    @Transaction
    open suspend fun upsert(entity: HistoryEntity) {
        if (update(entity) == 0) {
            insert(entity)
        }
    }

}