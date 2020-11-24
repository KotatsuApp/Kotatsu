package org.koitharu.kotatsu.favourites.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaEntity

@Dao
abstract class FavouritesDao {

	@Transaction
	@Query("SELECT * FROM favourites GROUP BY manga_id ORDER BY created_at")
	abstract suspend fun findAll(): List<FavouriteManga>

	@Transaction
	@Query("SELECT * FROM favourites GROUP BY manga_id ORDER BY created_at")
	abstract fun observeAll(): Flow<List<FavouriteManga>>

	@Transaction
	@Query("SELECT * FROM favourites GROUP BY manga_id ORDER BY created_at LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int): List<FavouriteManga>

	@Transaction
	@Query("SELECT * FROM favourites WHERE category_id = :categoryId GROUP BY manga_id ORDER BY created_at")
	abstract suspend fun findAll(categoryId: Long): List<FavouriteManga>

	@Transaction
	@Query("SELECT * FROM favourites WHERE category_id = :categoryId GROUP BY manga_id ORDER BY created_at")
	abstract fun observeAll(categoryId: Long): Flow<List<FavouriteManga>>

	@Transaction
	@Query("SELECT * FROM favourites WHERE category_id = :categoryId GROUP BY manga_id ORDER BY created_at LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(categoryId: Long, offset: Int, limit: Int): List<FavouriteManga>

	@Query("SELECT * FROM manga WHERE manga_id IN (SELECT manga_id FROM favourites)")
	abstract suspend fun findAllManga(): List<MangaEntity>

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id GROUP BY manga_id")
	abstract suspend fun find(id: Long): FavouriteManga?

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id GROUP BY manga_id")
	abstract fun observe(id: Long): Flow<FavouriteManga?>

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
}