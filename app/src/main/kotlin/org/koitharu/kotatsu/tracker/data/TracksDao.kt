package org.koitharu.kotatsu.tracker.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaQueryBuilder
import org.koitharu.kotatsu.list.domain.ListFilterOption

@Dao
abstract class TracksDao : MangaQueryBuilder.ConditionCallback {

	@Transaction
	@Query("SELECT * FROM tracks ORDER BY last_check_time ASC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<TrackWithManga>

	@Transaction
	@Query("SELECT * FROM tracks ORDER BY last_check_time DESC")
	abstract fun observeAll(): Flow<List<TrackWithManga>>

	@Query("SELECT manga_id FROM tracks")
	abstract suspend fun findAllIds(): LongArray

	@Query("SELECT * FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun find(mangaId: Long): TrackEntity?

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun findNewChapters(mangaId: Long): Int?

	@Query("SELECT COUNT(*) FROM tracks")
	abstract suspend fun getTracksCount(): Int

	@Query("SELECT chapters_new FROM tracks")
	abstract fun observeNewChapters(): Flow<List<Int>>

	@Query("SELECT chapters_new FROM tracks WHERE manga_id = :mangaId")
	abstract fun observeNewChapters(mangaId: Long): Flow<Int?>

	@Transaction
	@Query("SELECT * FROM tracks WHERE chapters_new > 0 ORDER BY last_chapter_date DESC")
	abstract fun observeUpdatedManga(): Flow<List<MangaWithTrack>>

	fun observeUpdatedManga(
		limit: Int,
		filterOptions: Set<ListFilterOption>,
	): Flow<List<MangaWithTrack>> = observeMangaImpl(
		MangaQueryBuilder("tracks", this)
			.where("chapters_new > 0")
			.filters(filterOptions)
			.limit(limit)
			.orderBy("last_chapter_date DESC")
			.build(),
	)

	@Query("DELETE FROM tracks")
	abstract suspend fun clear()

	@Query("UPDATE tracks SET chapters_new = 0")
	abstract suspend fun clearCounters()

	@Query("UPDATE tracks SET chapters_new = 0 WHERE manga_id = :mangaId")
	abstract suspend fun clearCounter(mangaId: Long)

	@Query("DELETE FROM tracks WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long)

	@Query("DELETE FROM tracks WHERE manga_id NOT IN (SELECT manga_id FROM history UNION SELECT manga_id FROM favourites WHERE category_id IN (SELECT category_id FROM favourite_categories WHERE track = 1))")
	abstract suspend fun gc()

	@Upsert
	abstract suspend fun upsert(entity: TrackEntity)

	@Transaction
	@RawQuery(observedEntities = [TrackEntity::class])
	protected abstract fun observeMangaImpl(query: SupportSQLiteQuery): Flow<List<MangaWithTrack>>

	override fun getCondition(option: ListFilterOption): String? = when (option) {
		ListFilterOption.Macro.FAVORITE -> "EXISTS(SELECT * FROM favourites WHERE favourites.manga_id = tracks.manga_id)"
		is ListFilterOption.Favorite -> "EXISTS(SELECT * FROM favourites WHERE favourites.manga_id = tracks.manga_id AND favourites.category_id = ${option.category.id})"
		is ListFilterOption.Tag -> "EXISTS(SELECT * FROM manga_tags WHERE manga_tags.manga_id = tracks.manga_id AND tag_id = ${option.tagId})"
		else -> null
	}
}
