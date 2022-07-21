package org.koitharu.kotatsu.favourites.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavouriteCategoriesDao {

	@Query("SELECT * FROM favourite_categories WHERE category_id = :id AND deleted_at = 0")
	abstract suspend fun find(id: Int): FavouriteCategoryEntity

	@Query("SELECT * FROM favourite_categories WHERE deleted_at = 0 ORDER BY sort_key")
	abstract suspend fun findAll(): List<FavouriteCategoryEntity>

	@Query("SELECT * FROM favourite_categories WHERE deleted_at = 0 ORDER BY sort_key")
	abstract fun observeAll(): Flow<List<FavouriteCategoryEntity>>

	@Query("SELECT * FROM favourite_categories WHERE category_id = :id AND deleted_at = 0")
	abstract fun observe(id: Long): Flow<FavouriteCategoryEntity?>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insert(category: FavouriteCategoryEntity): Long

	@Update
	abstract suspend fun update(category: FavouriteCategoryEntity): Int

	suspend fun delete(id: Long) = setDeletedAt(id, System.currentTimeMillis())

	@Query("UPDATE favourite_categories SET title = :title, `order` = :order, `track` = :tracker  WHERE category_id = :id")
	abstract suspend fun update(id: Long, title: String, order: String, tracker: Boolean)

	@Query("UPDATE favourite_categories SET `order` = :order WHERE category_id = :id")
	abstract suspend fun updateOrder(id: Long, order: String)

	// @Query("UPDATE favourite_categories SET `track` = :isEnabled WHERE category_id = :id")
	// abstract suspend fun updateTracking(id: Long, isEnabled: Boolean)

	@Query("UPDATE favourite_categories SET `show_in_lib` = :isEnabled WHERE category_id = :id")
	abstract suspend fun updateLibVisibility(id: Long, isEnabled: Boolean)

	@Query("UPDATE favourite_categories SET sort_key = :sortKey WHERE category_id = :id")
	abstract suspend fun updateSortKey(id: Long, sortKey: Int)

	@Query("DELETE FROM favourite_categories WHERE deleted_at != 0 AND deleted_at < :maxDeletionTime")
	abstract suspend fun gc(maxDeletionTime: Long)

	@Query("SELECT MAX(sort_key) FROM favourite_categories WHERE deleted_at = 0")
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

	@Query("UPDATE favourite_categories SET deleted_at = :deletedAt WHERE category_id = :id")
	protected abstract suspend fun setDeletedAt(id: Long, deletedAt: Long)
}
