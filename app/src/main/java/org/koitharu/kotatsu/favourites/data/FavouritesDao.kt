package org.koitharu.kotatsu.favourites.data

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.parsers.model.SortOrder

@Dao
abstract class FavouritesDao {

	@Transaction
	@Query("SELECT * FROM favourites WHERE deleted_at = 0 GROUP BY manga_id ORDER BY created_at DESC")
	abstract suspend fun findAll(): List<FavouriteManga>

	fun observeAll(order: SortOrder): Flow<List<FavouriteManga>> {
		val orderBy = getOrderBy(order)
		@Language("RoomSql") val query = SimpleSQLiteQuery(
			"SELECT * FROM favourites LEFT JOIN manga ON favourites.manga_id = manga.manga_id " +
				"WHERE favourites.deleted_at = 0 GROUP BY favourites.manga_id ORDER BY $orderBy",
		)
		return observeAllRaw(query)
	}

	@Transaction
	@Query(
		"SELECT * FROM favourites WHERE deleted_at = 0 " +
			"GROUP BY manga_id ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
	)
	abstract suspend fun findAll(offset: Int, limit: Int): List<FavouriteManga>

	@Transaction
	@Query(
		"SELECT * FROM favourites WHERE category_id = :categoryId AND deleted_at = 0 " +
			"GROUP BY manga_id ORDER BY created_at DESC"
	)
	abstract suspend fun findAll(categoryId: Long): List<FavouriteManga>

	fun observeAll(categoryId: Long, order: SortOrder): Flow<List<FavouriteManga>> {
		val orderBy = getOrderBy(order)
		@Language("RoomSql") val query = SimpleSQLiteQuery(
			"SELECT * FROM favourites LEFT JOIN manga ON favourites.manga_id = manga.manga_id " +
				"WHERE category_id = ? AND deleted_at = 0 GROUP BY favourites.manga_id ORDER BY $orderBy",
			arrayOf<Any>(categoryId),
		)
		return observeAllRaw(query)
	}

	@Transaction
	@Query(
		"SELECT * FROM favourites WHERE category_id = :categoryId AND deleted_at = 0 " +
			"GROUP BY manga_id ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
	)
	abstract suspend fun findAll(categoryId: Long, offset: Int, limit: Int): List<FavouriteManga>

	@Query(
		"SELECT * FROM manga WHERE manga_id IN " +
			"(SELECT manga_id FROM favourites WHERE category_id = :categoryId AND deleted_at = 0)"
	)
	abstract suspend fun findAllManga(categoryId: Int): List<MangaEntity>

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM favourites WHERE deleted_at = 0)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id AND deleted_at = 0 GROUP BY manga_id")
	abstract suspend fun find(id: Long): FavouriteManga?

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id AND deleted_at = 0 GROUP BY manga_id")
	abstract fun observe(id: Long): Flow<FavouriteManga?>

	@Query("SELECT DISTINCT category_id FROM favourites WHERE manga_id = :id AND deleted_at = 0")
	abstract fun observeIds(id: Long): Flow<List<Long>>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(favourite: FavouriteEntity)

	@Update
	abstract suspend fun update(favourite: FavouriteEntity): Int

	@Query("UPDATE favourites SET deleted_at = :now WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long, now: Long = System.currentTimeMillis())

	@Query("UPDATE favourites SET deleted_at = :now WHERE manga_id = :mangaId AND category_id = :categoryId")
	abstract suspend fun delete(categoryId: Long, mangaId: Long, now: Long = System.currentTimeMillis())

	suspend fun recover(mangaId: Long) = delete(mangaId, 0L)

	suspend fun recover(categoryId: Long, mangaId: Long) = delete(categoryId, mangaId, 0L)

	@Query("DELETE FROM favourites WHERE deleted_at != 0")
	abstract suspend fun gc()

	@Transaction
	open suspend fun upsert(entity: FavouriteEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}

	@Transaction
	@RawQuery(observedEntities = [FavouriteEntity::class])
	protected abstract fun observeAllRaw(query: SupportSQLiteQuery): Flow<List<FavouriteManga>>

	private fun getOrderBy(sortOrder: SortOrder) = when (sortOrder) {
		SortOrder.RATING -> "rating DESC"
		SortOrder.NEWEST,
		SortOrder.UPDATED -> "created_at DESC"
		SortOrder.ALPHABETICAL -> "title ASC"
		else -> throw IllegalArgumentException("Sort order $sortOrder is not supported")
	}
}