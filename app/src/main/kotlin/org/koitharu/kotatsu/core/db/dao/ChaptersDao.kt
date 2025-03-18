package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.koitharu.kotatsu.core.db.entity.ChapterEntity

@Dao
abstract class ChaptersDao {

	@Query("SELECT * FROM chapters WHERE manga_id = :mangaId ORDER BY `index` ASC")
	abstract suspend fun findAll(mangaId: Long): List<ChapterEntity>

	@Query("DELETE FROM chapters WHERE manga_id = :mangaId")
	abstract suspend fun deleteAll(mangaId: Long)

	@Query("DELETE FROM chapters WHERE manga_id NOT IN (SELECT manga_id FROM history WHERE deleted_at = 0) AND manga_id NOT IN (SELECT manga_id FROM favourites WHERE deleted_at = 0)")
	abstract suspend fun gc()

	@Transaction
	open suspend fun replaceAll(mangaId: Long, entities: Collection<ChapterEntity>) {
		deleteAll(mangaId)
		insert(entities)
	}

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	protected abstract suspend fun insert(entities: Collection<ChapterEntity>)
}
