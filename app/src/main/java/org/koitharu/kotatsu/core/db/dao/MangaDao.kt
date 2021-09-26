package org.koitharu.kotatsu.core.db.dao

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaTagsEntity
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class MangaDao {

	@Transaction
	@Query("SELECT * FROM manga WHERE manga_id = :id")
	abstract suspend fun find(id: Long): MangaWithTags?

	@Transaction
	@Query("SELECT * FROM manga WHERE title LIKE :query OR alt_title LIKE :query LIMIT :limit")
	abstract suspend fun searchByTitle(query: String, limit: Int): List<MangaWithTags>

	@Transaction
	@Query("SELECT * FROM manga WHERE (title LIKE :query OR alt_title LIKE :query) AND source = :source LIMIT :limit")
	abstract suspend fun searchByTitle(query: String, source: String, limit: Int): List<MangaWithTags>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(manga: MangaEntity): Long

	@Update(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun update(manga: MangaEntity): Int

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insertTagRelation(tag: MangaTagsEntity): Long

	@Query("DELETE FROM manga_tags WHERE manga_id = :mangaId")
	abstract suspend fun clearTagRelation(mangaId: Long)

	@Transaction
	open suspend fun upsert(manga: MangaEntity, tags: Iterable<TagEntity>? = null) {
		if (update(manga) <= 0) {
			insert(manga)
			if (tags != null) {
				clearTagRelation(manga.id)
				tags.map {
					MangaTagsEntity(manga.id, it.id)
				}.forEach {
					insertTagRelation(it)
				}
			}
		}
	}
}