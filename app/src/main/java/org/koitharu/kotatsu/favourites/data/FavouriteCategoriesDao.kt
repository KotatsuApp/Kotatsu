package org.koitharu.kotatsu.favourites.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavouriteCategoriesDao {

	@Query("SELECT * FROM favourite_categories WHERE category_id = :id")
	abstract suspend fun find(id: Int): FavouriteCategoryEntity

	@Query("SELECT * FROM favourite_categories ORDER BY sort_key")
	abstract suspend fun findAll(): List<FavouriteCategoryEntity>

	@Query("SELECT * FROM favourite_categories ORDER BY sort_key")
	abstract fun observeAll(): Flow<List<FavouriteCategoryEntity>>

	@MapInfo(valueColumn = "cover")
	@Query(
		"""
			SELECT favourite_categories.*, manga.cover_url AS cover 
			FROM favourite_categories JOIN manga ON manga.manga_id IN 
				(SELECT manga_id FROM favourites WHERE favourites.category_id == favourite_categories.category_id)
			ORDER BY favourite_categories.sort_key
		"""
	)
	abstract fun observeAllWithDetails(): Flow<Map<FavouriteCategoryEntity, List<String>>>

	@Query("SELECT * FROM favourite_categories WHERE category_id = :id")
	abstract fun observe(id: Long): Flow<FavouriteCategoryEntity?>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insert(category: FavouriteCategoryEntity): Long

	@Update
	abstract suspend fun update(category: FavouriteCategoryEntity): Int

	@Query("DELETE FROM favourite_categories WHERE category_id = :id")
	abstract suspend fun delete(id: Long)

	@Query("UPDATE favourite_categories SET title = :title WHERE category_id = :id")
	abstract suspend fun updateTitle(id: Long, title: String)

	@Query("UPDATE favourite_categories SET title = :title, `order` = :order, `track` = :tracker  WHERE category_id = :id")
	abstract suspend fun update(id: Long, title: String, order: String, tracker: Boolean)

	@Query("UPDATE favourite_categories SET `order` = :order WHERE category_id = :id")
	abstract suspend fun updateOrder(id: Long, order: String)

	@Query("UPDATE favourite_categories SET `track` = :isEnabled WHERE category_id = :id")
	abstract suspend fun updateTracking(id: Long, isEnabled: Boolean)

	@Query("UPDATE favourite_categories SET `show_in_lib` = :isEnabled WHERE category_id = :id")
	abstract suspend fun updateLibVisibility(id: Long, isEnabled: Boolean)

	@Query("UPDATE favourite_categories SET sort_key = :sortKey WHERE category_id = :id")
	abstract suspend fun updateSortKey(id: Long, sortKey: Int)

	@Query("SELECT MAX(sort_key) FROM favourite_categories")
	protected abstract suspend fun getMaxSortKey(): Int?

	suspend fun getNextSortKey(): Int {
		return (getMaxSortKey() ?: 0) + 1
	}

	@Transaction
	open suspend fun upsert(entity: FavouriteCategoryEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}
}