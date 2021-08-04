package org.koitharu.kotatsu.favourites.data

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.model.SortOrder

@Dao
abstract class FavouritesDao {

	@Transaction
	@Query("SELECT * FROM favourites GROUP BY manga_id ORDER BY created_at DESC")
	abstract suspend fun findAll(): List<FavouriteManga>

	fun observeAll(order: SortOrder): Flow<List<FavouriteManga>> {
		val orderBy = getOrderBy(order)
		val query = SimpleSQLiteQuery(
			"SELECT * FROM favourites LEFT JOIN manga ON favourites.manga_id = manga.manga_id GROUP BY favourites.manga_id ORDER BY $orderBy",
		)
		return observeAllRaw(query)
	}

	@Transaction
	@Query("SELECT * FROM favourites GROUP BY manga_id ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<FavouriteManga>

	@Transaction
	@Query("SELECT * FROM favourites WHERE category_id = :categoryId GROUP BY manga_id ORDER BY created_at DESC")
	abstract suspend fun findAll(categoryId: Long): List<FavouriteManga>

	fun observeAll(categoryId: Long, order: SortOrder): Flow<List<FavouriteManga>> {
		val orderBy = getOrderBy(order)
		val query = SimpleSQLiteQuery(
			"SELECT * FROM favourites LEFT JOIN manga ON favourites.manga_id = manga.manga_id WHERE category_id = ? GROUP BY favourites.manga_id ORDER BY $orderBy",
			arrayOf<Any>(categoryId),
		)
		return observeAllRaw(query)
	}

	@Transaction
	@Query("SELECT * FROM favourites WHERE category_id = :categoryId GROUP BY manga_id ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(categoryId: Long, offset: Int, limit: Int): List<FavouriteManga>

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM favourites)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id GROUP BY manga_id")
	abstract suspend fun find(id: Long): FavouriteManga?

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id GROUP BY manga_id")
	abstract fun observe(id: Long): Flow<FavouriteManga?>

	@Query("SELECT DISTINCT category_id FROM favourites WHERE manga_id = :id")
	abstract fun observeIds(id: Long): Flow<List<Long>>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(favourite: FavouriteEntity)

	@Update
	abstract suspend fun update(favourite: FavouriteEntity): Int

	@Query("DELETE FROM favourites WHERE manga_id = :mangaId")
	abstract suspend fun delete(mangaId: Long)

	@Query("DELETE FROM favourites WHERE manga_id = :mangaId AND category_id = :categoryId")
	abstract suspend fun delete(categoryId: Long, mangaId: Long)

	@Transaction
	open suspend fun upsert(entity: FavouriteEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}

	@Transaction
	@RawQuery(observedEntities = [FavouriteEntity::class])
	protected abstract fun observeAllRaw(query: SupportSQLiteQuery): Flow<List<FavouriteManga>>

	private fun getOrderBy(sortOrder: SortOrder) = when(sortOrder) {
		SortOrder.RATING -> "rating DESC"
		SortOrder.NEWEST,
		SortOrder.UPDATED -> "created_at DESC"
		SortOrder.ALPHABETICAL -> "title ASC"
		else -> throw IllegalArgumentException("Sort order $sortOrder is not supported")
	}
}