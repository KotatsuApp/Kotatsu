package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity

@Dao
abstract class MangaSourcesDao {

	@Query("SELECT * FROM sources ORDER BY sort_key")
	abstract suspend fun findAll(): List<MangaSourceEntity>

	@Query("SELECT * FROM sources WHERE enabled = 1 ORDER BY sort_key")
	abstract suspend fun findAllEnabled(): List<MangaSourceEntity>

	@Query("SELECT * FROM sources WHERE enabled = 1 ORDER BY sort_key")
	abstract fun observeEnabled(): Flow<List<MangaSourceEntity>>

	@Query("SELECT * FROM sources ORDER BY sort_key")
	abstract fun observeAll(): Flow<List<MangaSourceEntity>>

	@Query("SELECT IFNULL(MAX(sort_key),0) FROM sources")
	abstract suspend fun getMaxSortKey(): Int

	@Query("UPDATE sources SET enabled = 0")
	abstract suspend fun disableAllSources()

	@Query("UPDATE sources SET sort_key = :sortKey WHERE source = :source")
	abstract suspend fun setSortKey(source: String, sortKey: Int)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	@Transaction
	abstract suspend fun insertIfAbsent(entries: Collection<MangaSourceEntity>)

	@Upsert
	abstract suspend fun upsert(entry: MangaSourceEntity)

	@Transaction
	open suspend fun setEnabled(source: String, isEnabled: Boolean) {
		if (updateIsEnabled(source, isEnabled) == 0) {
			val entity = MangaSourceEntity(
				source = source,
				isEnabled = isEnabled,
				sortKey = getMaxSortKey() + 1,
			)
			upsert(entity)
		}
	}

	@Query("UPDATE sources SET enabled = :isEnabled WHERE source = :source")
	protected abstract suspend fun updateIsEnabled(source: String, isEnabled: Boolean): Int
}
