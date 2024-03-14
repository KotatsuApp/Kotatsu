package org.koitharu.kotatsu.history.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.list.domain.ListSortOrder

@Dao
abstract class HistoryDao {

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<HistoryWithManga>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 AND manga_id IN (:ids)")
	abstract suspend fun findAll(ids: Collection<Long>): List<HistoryEntity>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC")
	abstract fun observeAll(): Flow<List<HistoryWithManga>>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC LIMIT :limit")
	abstract fun observeAll(limit: Int): Flow<List<HistoryWithManga>>

	fun observeAll(order: ListSortOrder): Flow<List<HistoryWithManga>> {
		val orderBy = when (order) {
			ListSortOrder.LAST_READ -> "history.updated_at DESC"
			ListSortOrder.LONG_AGO_READ -> "history.updated_at ASC"
			ListSortOrder.NEWEST -> "history.created_at DESC"
			ListSortOrder.OLDEST -> "history.created_at ASC"
			ListSortOrder.PROGRESS -> "history.percent DESC"
			ListSortOrder.UNREAD -> "history.percent ASC"
			ListSortOrder.ALPHABETIC -> "manga.title"
			ListSortOrder.ALPHABETIC_REVERSE -> "manga.title DESC"
			ListSortOrder.NEW_CHAPTERS -> "IFNULL((SELECT chapters_new FROM tracks WHERE tracks.manga_id = manga.manga_id), 0) DESC"
			else -> throw IllegalArgumentException("Sort order $order is not supported")
		}

		@Language("RoomSql")
		val query = SimpleSQLiteQuery(
			"SELECT * FROM history LEFT JOIN manga ON history.manga_id = manga.manga_id " +
				"WHERE history.deleted_at = 0 GROUP BY history.manga_id ORDER BY $orderBy",
		)
		return observeAllImpl(query)
	}

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM history WHERE deleted_at = 0)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id
		INNER JOIN history ON history.manga_id = manga_tags.manga_id
		WHERE history.deleted_at = 0
		GROUP BY manga_tags.tag_id 
		ORDER BY COUNT(manga_tags.manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findPopularTags(limit: Int): List<TagEntity>

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract suspend fun find(id: Long): HistoryEntity?

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract fun observe(id: Long): Flow<HistoryEntity?>

	@Query("SELECT COUNT(*) FROM history WHERE deleted_at = 0")
	abstract fun observeCount(): Flow<Int>

	@Query("SELECT percent FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract suspend fun findProgress(id: Long): Float?

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: HistoryEntity): Long

	@Query(
		"UPDATE history SET page = :page, chapter_id = :chapterId, scroll = :scroll, percent = :percent, updated_at = :updatedAt, deleted_at = 0 WHERE manga_id = :mangaId",
	)
	abstract suspend fun update(
		mangaId: Long,
		page: Int,
		chapterId: Long,
		scroll: Float,
		percent: Float,
		updatedAt: Long,
	): Int

	suspend fun delete(mangaId: Long) = setDeletedAt(mangaId, System.currentTimeMillis())

	suspend fun recover(mangaId: Long) = setDeletedAt(mangaId, 0L)

	@Query("DELETE FROM history WHERE deleted_at != 0 AND deleted_at < :maxDeletionTime")
	abstract suspend fun gc(maxDeletionTime: Long)

	suspend fun deleteAfter(minDate: Long) = setDeletedAtAfter(minDate, System.currentTimeMillis())

	suspend fun clear() = setDeletedAtAfter(0L, System.currentTimeMillis())

	suspend fun update(entity: HistoryEntity) = update(
		mangaId = entity.mangaId,
		page = entity.page,
		chapterId = entity.chapterId,
		scroll = entity.scroll,
		percent = entity.percent,
		updatedAt = entity.updatedAt,
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

	@Query("UPDATE history SET deleted_at = :deletedAt WHERE manga_id = :mangaId")
	protected abstract suspend fun setDeletedAt(mangaId: Long, deletedAt: Long)

	@Query("UPDATE history SET deleted_at = :deletedAt WHERE created_at >= :minDate AND deleted_at = 0")
	protected abstract suspend fun setDeletedAtAfter(minDate: Long, deletedAt: Long)

	@Transaction
	@RawQuery(observedEntities = [HistoryEntity::class])
	protected abstract fun observeAllImpl(query: SupportSQLiteQuery): Flow<List<HistoryWithManga>>
}
