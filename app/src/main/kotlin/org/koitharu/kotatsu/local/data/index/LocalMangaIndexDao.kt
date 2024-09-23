package org.koitharu.kotatsu.local.data.index

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
interface LocalMangaIndexDao {

	@Query("SELECT path FROM local_index WHERE manga_id = :mangaId")
	suspend fun findPath(mangaId: Long): String?

	@Upsert
	suspend fun upsert(entity: LocalMangaIndexEntity)

	@Query("DELETE FROM local_index WHERE manga_id = :mangaId")
	suspend fun delete(mangaId: Long)

	@Query("DELETE FROM local_index")
	suspend fun clear()
}
