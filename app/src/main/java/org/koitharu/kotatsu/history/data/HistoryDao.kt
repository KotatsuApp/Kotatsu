package org.koitharu.kotatsu.history.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class HistoryDao {

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<HistoryWithManga>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 AND manga_id IN (:ids)")
	abstract suspend fun findAll(ids: Collection<Long>): List<HistoryEntity?>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC")
	abstract fun observeAll(): Flow<List<HistoryWithManga>>

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM history WHERE deleted_at = 0)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id
		INNER JOIN history ON history.manga_id = manga_tags.manga_id
		WHERE history.deleted_at = 0
		GROUP BY manga_tags.tag_id 
		ORDER BY COUNT(manga_tags.manga_id) DESC 
		LIMIT :limit"""
	)
	abstract suspend fun findPopularTags(limit: Int): List<TagEntity>

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract suspend fun find(id: Long): HistoryEntity?

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract fun observe(id: Long): Flow<HistoryEntity?>

	@Query("SELECT COUNT(*) FROM history WHERE deleted_at = 0")
	abstract fun observeCount(): Flow<Int>

	@Query("UPDATE history SET deleted_at = :now WHERE deleted_at = 0")
	abstract suspend fun clear(now: Long = System.currentTimeMillis())

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: HistoryEntity): Long

	@Query(
		"UPDATE history SET page = :page, chapter_id = :chapterId, scroll = :scroll, updated_at = :updatedAt " +
			"WHERE manga_id = :mangaId"
	)
	abstract suspend fun update(
		mangaId: Long,
		page: Int,
		chapterId: Long,
		scroll: Float,
		updatedAt: Long
	): Int

	@Query("UPDATE history SET deleted_at = :now WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long, now: Long = System.currentTimeMillis())

	@Query("DELETE FROM history WHERE deleted_at != 0")
	abstract suspend fun gc()

	suspend fun update(entity: HistoryEntity) =
		update(entity.mangaId, entity.page, entity.chapterId, entity.scroll, entity.updatedAt)

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