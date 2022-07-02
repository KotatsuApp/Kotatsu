package org.koitharu.kotatsu.scrobbling.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScrobblingDao {

	@Query("SELECT * FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract suspend fun find(scrobbler: Int, mangaId: Long): ScrobblingEntity?

	@Query("SELECT * FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract fun observe(scrobbler: Int, mangaId: Long): Flow<ScrobblingEntity?>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insert(entity: ScrobblingEntity)

	@Update
	abstract suspend fun update(entity: ScrobblingEntity)

	@Query("DELETE FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract suspend fun delete(scrobbler: Int, mangaId: Long)
}