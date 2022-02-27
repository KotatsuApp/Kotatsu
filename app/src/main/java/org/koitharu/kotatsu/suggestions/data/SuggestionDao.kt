package org.koitharu.kotatsu.suggestions.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SuggestionDao {

	@Transaction
	@Query("SELECT * FROM suggestions ORDER BY relevance DESC")
	abstract fun observeAll(): Flow<List<SuggestionWithManga>>

	@Query("SELECT COUNT(*) FROM suggestions")
	abstract suspend fun count(): Int

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: SuggestionEntity): Long

	@Update
	abstract suspend fun update(entity: SuggestionEntity): Int

	@Query("DELETE FROM suggestions")
	abstract suspend fun deleteAll()

	@Transaction
	open suspend fun upsert(entity: SuggestionEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}
}