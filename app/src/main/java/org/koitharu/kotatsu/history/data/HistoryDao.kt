package org.koitharu.kotatsu.history.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class HistoryDao {

	/**
	 * @hide
	 */
	@Transaction
	@Query("SELECT * FROM history ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<HistoryWithManga>

	@Transaction
	@Query("SELECT * FROM history WHERE manga_id IN (:ids)")
	abstract suspend fun findAll(ids: Collection<Long>): List<HistoryEntity?>

	@Transaction
	@Query("SELECT * FROM history ORDER BY updated_at DESC")
	abstract fun observeAll(): Flow<List<HistoryWithManga>>

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM history)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id
		INNER JOIN history ON history.manga_id = manga_tags.manga_id
		GROUP BY manga_tags.tag_id 
		ORDER BY COUNT(manga_tags.manga_id) DESC 
		LIMIT :limit"""
	)
	abstract suspend fun findPopularTags(limit: Int): List<TagEntity>

	@Query("SELECT * FROM history WHERE manga_id = :id")
	abstract suspend fun find(id: Long): HistoryEntity?

	@Query("SELECT * FROM history WHERE manga_id = :id")
	abstract fun observe(id: Long): Flow<HistoryEntity?>

	@Query("SELECT COUNT(*) FROM history")
	abstract fun observeCount(): Flow<Int>

	@Query("SELECT percent FROM history WHERE manga_id = :id")
	abstract fun findProgress(id: Long): Float?

	@Query("DELETE FROM history")
	abstract suspend fun clear()

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: HistoryEntity): Long

	@Query("UPDATE history SET page = :page, chapter_id = :chapterId, scroll = :scroll, percent = :percent, updated_at = :updatedAt WHERE manga_id = :mangaId")
	abstract suspend fun update(
		mangaId: Long,
		page: Int,
		chapterId: Long,
		scroll: Float,
		percent: Float,
		updatedAt: Long,
	): Int

	@Query("DELETE FROM history WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long)

	@Query("DELETE FROM history WHERE created_at >= :minDate")
	abstract suspend fun deleteAfter(minDate: Long)

	suspend fun update(entity: HistoryEntity) = update(
		mangaId = entity.mangaId,
		page = entity.page,
		chapterId = entity.chapterId,
		scroll = entity.scroll,
		percent = entity.percent,
		updatedAt = entity.updatedAt
	)

	@Transaction
	open suspend fun upsert(entity: HistoryEntity): Boolean {
		return if (update(entity) == 0) {
			insert(entity)
			true
		} else false
	}

	@Transaction
	open suspend fun upsert(entities: Iterable<HistoryEntity>) {
		for (e in entities) {
			if (update(e) == 0) {
				insert(e)
			}
		}
	}
}