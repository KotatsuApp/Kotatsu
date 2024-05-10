package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
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
	@Query("SELECT * FROM manga WHERE public_url = :publicUrl")
	abstract suspend fun findByPublicUrl(publicUrl: String): MangaWithTags?

	@Transaction
	@Query("SELECT * FROM manga WHERE source = :source")
	abstract suspend fun findAllBySource(source: String): List<MangaWithTags>

	@Query("SELECT author FROM manga WHERE author LIKE :query GROUP BY author ORDER BY COUNT(author) DESC LIMIT :limit")
	abstract suspend fun findAuthors(query: String, limit: Int): List<String>

	@Transaction
	@Query("SELECT * FROM manga WHERE (title LIKE :query OR alt_title LIKE :query) AND manga_id IN (SELECT manga_id FROM favourites UNION SELECT manga_id FROM history) LIMIT :limit")
	abstract suspend fun searchByTitle(query: String, limit: Int): List<MangaWithTags>

	@Transaction
	@Query("SELECT * FROM manga WHERE (title LIKE :query OR alt_title LIKE :query) AND source = :source AND manga_id IN (SELECT manga_id FROM favourites UNION SELECT manga_id FROM history) LIMIT :limit")
	abstract suspend fun searchByTitle(query: String, source: String, limit: Int): List<MangaWithTags>

	@Upsert
	protected abstract suspend fun upsert(manga: MangaEntity)

	@Update(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun update(manga: MangaEntity): Int

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insertTagRelation(tag: MangaTagsEntity): Long

	@Query("DELETE FROM manga_tags WHERE manga_id = :mangaId")
	abstract suspend fun clearTagRelation(mangaId: Long)

	@Transaction
	@Delete
	abstract suspend fun delete(subjects: Collection<MangaEntity>)

	@Transaction
	open suspend fun upsert(manga: MangaEntity, tags: Iterable<TagEntity>? = null) {
		upsert(manga)
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
