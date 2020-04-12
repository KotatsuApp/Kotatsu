package org.koitharu.kotatsu.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.koitharu.kotatsu.core.db.entity.FavouriteCategoryEntity

@Dao
abstract class FavouriteCategoriesDao {

	@Query("SELECT category_id,title,created_at FROM favourite_categories ORDER BY :orderBy")
	abstract suspend fun findAll(orderBy: String): List<FavouriteCategoryEntity>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insert(category: FavouriteCategoryEntity): Long

	@Query("DELETE FROM favourite_categories WHERE category_id = :id")
	abstract suspend fun delete(id: Long)

	@Query("UPDATE favourite_categories SET title = :title WHERE category_id = :id")
	abstract suspend fun update(id: Long, title: String)
}