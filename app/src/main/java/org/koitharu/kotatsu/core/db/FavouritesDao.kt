package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.FavouriteEntity
import org.koitharu.kotatsu.core.db.entity.FavouriteManga

@Dao
abstract class FavouritesDao {

	@Transaction
	@Query("SELECT * FROM favourites ORDER BY :orderBy LIMIT :limit OFFSET :offset")
	abstract suspend fun findAll(offset: Int, limit: Int, orderBy: String): List<FavouriteManga>

	@Transaction
	@Query("SELECT * FROM favourites WHERE manga_id = :id")
	abstract suspend fun find(id: Long): FavouriteManga?

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun add(favourite: FavouriteEntity)

	@Delete
	abstract suspend fun delete(favourite: FavouriteEntity)
}