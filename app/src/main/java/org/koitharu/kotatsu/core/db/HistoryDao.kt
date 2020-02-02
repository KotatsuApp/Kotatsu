package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.HistoryEntity
import org.koitharu.kotatsu.core.db.entity.HistoryWithManga
import org.koitharu.kotatsu.core.db.entity.MangaEntity


@Dao
abstract class HistoryDao {

    /**
     * @hide
     */
    @Transaction
    @Query("SELECT * FROM history ORDER BY :orderBy LIMIT :limit OFFSET :offset")
    abstract suspend fun getAll(offset: Int, limit: Int, orderBy: String): List<HistoryWithManga>

    @Query("SELECT * FROM history WHERE manga_id = :id")
    abstract suspend fun getOneOrNull(id: Long): HistoryEntity?

    @Query("DELETE FROM history")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertManga(manga: MangaEntity): Long

    @Query("UPDATE history SET page = :page, chapter_id = :chapterId, updated_at = :updatedAt WHERE manga_id = :mangaId")
    abstract suspend fun update(mangaId: Long, page: Int, chapterId: Long, updatedAt: Long): Int

    suspend fun update(entity: HistoryWithManga) = update(entity.manga.id, entity.history.page, entity.history.chapterId, entity.history.updatedAt)

    @Transaction
    open suspend fun upsert(entity: HistoryWithManga) {
        if (update(entity) == 0) {
            insertManga(entity.manga)
            insert(entity.history)
        }
    }

}