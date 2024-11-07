package org.koitharu.kotatsu.local.data.index

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LocalMangaIndexDao {

	@Query("SELECT path FROM local_index WHERE manga_id = :mangaId")
	suspend fun findPath(mangaId: Long): String?

	@Query("SELECT title FROM local_index LEFT JOIN manga_tags ON manga_tags.manga_id = local_index.manga_id LEFT JOIN tags ON tags.tag_id = manga_tags.tag_id WHERE title IS NOT NULL GROUP BY title")
	suspend fun findTags(): List<String>

	@Query("SELECT title FROM local_index LEFT JOIN manga_tags ON manga_tags.manga_id = local_index.manga_id LEFT JOIN tags ON tags.tag_id = manga_tags.tag_id WHERE (SELECT nsfw FROM manga WHERE manga.manga_id = local_index.manga_id) = :isNsfw AND title IS NOT NULL GROUP BY title")
	suspend fun findTags(isNsfw: Boolean): List<String>

	@Upsert
	suspend fun upsert(entity: LocalMangaIndexEntity)

	@Query("DELETE FROM local_index WHERE manga_id = :mangaId")
	suspend fun delete(mangaId: Long)

	@Query("DELETE FROM local_index")
	suspend fun clear()
}
