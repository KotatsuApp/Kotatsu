package org.koitharu.kotatsu.history.data

import android.database.DatabaseUtils.sqlEscapeString
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.koitharu.kotatsu.core.db.MangaQueryBuilder
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_COMPLETED

@Dao
abstract class HistoryDao : MangaQueryBuilder.ConditionCallback {

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<HistoryWithManga>

	@Transaction
	@Query("SELECT manga.* FROM history LEFT JOIN manga ON manga.manga_id = history.manga_id WHERE history.deleted_at = 0 AND (manga.title LIKE :query OR manga.alt_title LIKE :query) LIMIT :limit")
	abstract suspend fun searchByTitle(query: String, limit: Int): List<MangaWithTags>

	@Transaction
	@Query("SELECT manga.* FROM history LEFT JOIN manga ON manga.manga_id = history.manga_id WHERE history.deleted_at = 0 AND (manga.author LIKE :query) LIMIT :limit")
	abstract suspend fun searchByAuthor(query: String, limit: Int): List<MangaWithTags>

	@Transaction
	@Query("SELECT manga.* FROM history LEFT JOIN manga ON manga.manga_id = history.manga_id WHERE history.deleted_at = 0 AND EXISTS(SELECT 1 FROM tags LEFT JOIN manga_tags ON manga_tags.tag_id = tags.tag_id WHERE manga_tags.manga_id = manga.manga_id AND tags.title LIKE :query) LIMIT :limit")
	abstract suspend fun searchByTag(query: String, limit: Int): List<MangaWithTags>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC")
	abstract fun observeAll(): Flow<List<HistoryWithManga>>

	@Transaction
	@Query("SELECT * FROM history WHERE deleted_at = 0 ORDER BY updated_at DESC LIMIT :limit")
	abstract fun observeAll(limit: Int): Flow<List<HistoryWithManga>>

	fun observeAll(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<HistoryWithManga>> = observeAllImpl(
		MangaQueryBuilder(TABLE_HISTORY, this)
			.join("LEFT JOIN manga ON history.manga_id = manga.manga_id")
			.where("history.deleted_at = 0")
			.filters(filterOptions)
			.orderBy(
				orderBy = when (order) {
					ListSortOrder.LAST_READ -> "history.updated_at DESC"
					ListSortOrder.LONG_AGO_READ -> "history.updated_at ASC"
					ListSortOrder.NEWEST -> "history.created_at DESC"
					ListSortOrder.OLDEST -> "history.created_at ASC"
					ListSortOrder.PROGRESS -> "history.percent DESC"
					ListSortOrder.UNREAD -> "history.percent ASC"
					ListSortOrder.ALPHABETIC -> "manga.title"
					ListSortOrder.ALPHABETIC_REVERSE -> "manga.title DESC"
					ListSortOrder.NEW_CHAPTERS -> "IFNULL((SELECT chapters_new FROM tracks WHERE tracks.manga_id = manga.manga_id), 0) DESC"
					ListSortOrder.UPDATED -> "IFNULL((SELECT last_chapter_date FROM tracks WHERE tracks.manga_id = manga.manga_id), 0) DESC"
					else -> throw IllegalArgumentException("Sort order $order is not supported")
				},
			)
			.groupBy("history.manga_id")
			.limit(limit)
			.build(),
	)

	@Query("SELECT manga_id FROM history WHERE deleted_at = 0")
	abstract suspend fun findAllIds(): LongArray

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

	@Query("SELECT manga.source AS count FROM history LEFT JOIN manga ON manga.manga_id = history.manga_id GROUP BY manga.source ORDER BY COUNT(manga.source) DESC LIMIT :limit")
	abstract suspend fun findPopularSources(limit: Int): List<String>

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract suspend fun find(id: Long): HistoryEntity?

	@Query("SELECT * FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract fun observe(id: Long): Flow<HistoryEntity?>

	@Query("SELECT COUNT(*) FROM history WHERE deleted_at = 0")
	abstract fun observeCount(): Flow<Int>

	@Query("SELECT COUNT(*) FROM history WHERE deleted_at = 0")
	abstract suspend fun getCount(): Int

	@Query("SELECT percent FROM history WHERE manga_id = :id AND deleted_at = 0")
	abstract suspend fun findProgress(id: Long): Float?

	fun dump(): Flow<HistoryWithManga> = flow {
		val window = 10
		var offset = 0
		while (currentCoroutineContext().isActive) {
			val list = findAll(offset, window)
			if (list.isEmpty()) {
				break
			}
			offset += window
			list.forEach { emit(it) }
		}
	}

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: HistoryEntity): Long

	@Query(
		"UPDATE history SET page = :page, chapter_id = :chapterId, scroll = :scroll, percent = :percent, updated_at = :updatedAt, chapters = :chapters, deleted_at = 0 WHERE manga_id = :mangaId",
	)
	abstract suspend fun update(
		mangaId: Long,
		page: Int,
		chapterId: Long,
		scroll: Float,
		percent: Float,
		chapters: Int,
		updatedAt: Long,
	): Int

	suspend fun delete(mangaId: Long) = setDeletedAt(mangaId, System.currentTimeMillis())

	suspend fun recover(mangaId: Long) = setDeletedAt(mangaId, 0L)

	@Query("DELETE FROM history WHERE deleted_at != 0 AND deleted_at < :maxDeletionTime")
	abstract suspend fun gc(maxDeletionTime: Long)

	suspend fun deleteAfter(minDate: Long) = setDeletedAtAfter(minDate, System.currentTimeMillis())

	suspend fun deleteNotFavorite() = setDeletedAtNotFavorite(System.currentTimeMillis())

	suspend fun clear() = setDeletedAtAfter(0L, System.currentTimeMillis())

	suspend fun update(entity: HistoryEntity) = update(
		mangaId = entity.mangaId,
		page = entity.page,
		chapterId = entity.chapterId,
		scroll = entity.scroll,
		percent = entity.percent,
		chapters = entity.chaptersCount,
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

	@Query("UPDATE history SET deleted_at = :deletedAt WHERE deleted_at = 0 AND NOT EXISTS(SELECT * FROM favourites WHERE history.manga_id = favourites.manga_id)")
	protected abstract suspend fun setDeletedAtNotFavorite(deletedAt: Long)

	@Transaction
	@RawQuery(observedEntities = [HistoryEntity::class])
	protected abstract fun observeAllImpl(query: SupportSQLiteQuery): Flow<List<HistoryWithManga>>

	override fun getCondition(option: ListFilterOption): String? = when (option) {
		is ListFilterOption.Favorite -> "EXISTS(SELECT * FROM favourites WHERE history.manga_id = favourites.manga_id AND category_id = ${option.category.id})"
		ListFilterOption.Macro.COMPLETED -> "percent >= $PROGRESS_COMPLETED"
		ListFilterOption.Macro.NEW_CHAPTERS -> "(SELECT chapters_new FROM tracks WHERE tracks.manga_id = history.manga_id) > 0"
		ListFilterOption.Macro.FAVORITE -> "EXISTS(SELECT * FROM favourites WHERE history.manga_id = favourites.manga_id)"
		ListFilterOption.Macro.NSFW -> "manga.nsfw = 1"
		is ListFilterOption.Tag -> "EXISTS(SELECT * FROM manga_tags WHERE history.manga_id = manga_tags.manga_id AND tag_id = ${option.tagId})"
		ListFilterOption.Downloaded -> "EXISTS(SELECT * FROM local_index WHERE local_index.manga_id = history.manga_id)"
		is ListFilterOption.Source -> "manga.source = ${sqlEscapeString(option.mangaSource.name)}"
		else -> null
	}
}
