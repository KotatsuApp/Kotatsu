package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaQueryBuilder
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.tracker.data.TrackLogEntity
import org.koitharu.kotatsu.tracker.data.TrackLogWithManga

@Dao
abstract class TrackLogsDao : MangaQueryBuilder.ConditionCallback {

	fun observeAll(
		limit: Int,
		filterOptions: Set<ListFilterOption>,
	): Flow<List<TrackLogWithManga>> = observeAllImpl(
		MangaQueryBuilder("track_logs", this)
			.filters(filterOptions)
			.limit(limit)
			.orderBy("created_at DESC")
			.build(),
	)

	@Query("SELECT COUNT(*) FROM track_logs WHERE unread = 1")
	abstract fun observeUnreadCount(): Flow<Int>

	@Query("DELETE FROM track_logs")
	abstract suspend fun clear()

	@Query("UPDATE track_logs SET unread = 0 WHERE id = :id")
	abstract suspend fun markAsRead(id: Long)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insert(entity: TrackLogEntity): Long

	@Query("DELETE FROM track_logs WHERE manga_id NOT IN (SELECT manga_id FROM tracks)")
	abstract suspend fun gc()

	@Query("DELETE FROM track_logs WHERE id IN (SELECT id FROM track_logs ORDER BY created_at DESC LIMIT 0 OFFSET :size)")
	abstract suspend fun trim(size: Int)

	@Query("SELECT COUNT(*) FROM track_logs")
	abstract suspend fun count(): Int

	@Transaction
	@RawQuery(observedEntities = [TrackLogEntity::class])
	protected abstract fun observeAllImpl(query: SupportSQLiteQuery): Flow<List<TrackLogWithManga>>

	override fun getCondition(option: ListFilterOption): String? = when (option) {
		ListFilterOption.Macro.FAVORITE -> "EXISTS(SELECT * FROM favourites WHERE favourites.manga_id = track_logs.manga_id)"
		is ListFilterOption.Favorite -> "EXISTS(SELECT * FROM favourites WHERE favourites.manga_id = track_logs.manga_id AND favourites.category_id = ${option.category.id})"
		is ListFilterOption.Tag -> "EXISTS(SELECT * FROM manga_tags WHERE manga_tags.manga_id = track_logs.manga_id AND tag_id = ${option.tagId})"
		else -> null
	}
}
