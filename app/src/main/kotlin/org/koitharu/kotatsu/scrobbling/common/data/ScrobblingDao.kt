package org.koitharu.kotatsu.scrobbling.common.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScrobblingDao {

	@Query("SELECT * FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract suspend fun find(scrobbler: Int, mangaId: Long): ScrobblingEntity?

	@Query("SELECT * FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract fun observe(scrobbler: Int, mangaId: Long): Flow<ScrobblingEntity?>

	@Query("SELECT * FROM scrobblings WHERE scrobbler = :scrobbler")
	abstract fun observe(scrobbler: Int): Flow<List<ScrobblingEntity>>

	@Upsert
	abstract suspend fun upsert(entity: ScrobblingEntity)

	@Query("DELETE FROM scrobblings WHERE scrobbler = :scrobbler AND manga_id = :mangaId")
	abstract suspend fun delete(scrobbler: Int, mangaId: Long)
}
