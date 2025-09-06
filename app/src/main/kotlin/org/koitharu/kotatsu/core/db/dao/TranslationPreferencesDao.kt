package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.TranslationPreferencesEntity

@Dao
abstract class TranslationPreferencesDao {

	@Query("SELECT * FROM translation_preferences WHERE manga_id = :mangaId ORDER BY priority ASC, last_used DESC")
	abstract suspend fun getPreferences(mangaId: Long): List<TranslationPreferencesEntity>

	@Query("SELECT * FROM translation_preferences WHERE manga_id = :mangaId ORDER BY priority ASC, last_used DESC")
	abstract fun observePreferences(mangaId: Long): Flow<List<TranslationPreferencesEntity>>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertOrReplace(preferences: List<TranslationPreferencesEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertOrReplace(preference: TranslationPreferencesEntity)

	@Update
	abstract suspend fun update(preference: TranslationPreferencesEntity)

	@Query("UPDATE translation_preferences SET last_used = :timestamp WHERE manga_id = :mangaId AND branch = :branch")
	abstract suspend fun updateLastUsed(mangaId: Long, branch: String, timestamp: Long)

	@Query("UPDATE translation_preferences SET priority = :priority WHERE manga_id = :mangaId AND branch = :branch")
	abstract suspend fun updatePriority(mangaId: Long, branch: String, priority: Int)

	@Query("DELETE FROM translation_preferences WHERE manga_id = :mangaId")
	abstract suspend fun deleteByMangaId(mangaId: Long)

	@Query("DELETE FROM translation_preferences WHERE manga_id = :mangaId AND branch = :branch")
	abstract suspend fun delete(mangaId: Long, branch: String)

	@Query("SELECT COUNT(*) FROM translation_preferences WHERE manga_id = :mangaId")
	abstract suspend fun getPreferencesCount(mangaId: Long): Int
}