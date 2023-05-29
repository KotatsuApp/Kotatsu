package org.koitharu.kotatsu.core.db.dao

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class TagsDao {

	@Query("SELECT * FROM tags WHERE source = :source")
	abstract suspend fun findTags(source: String): List<TagEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id
		GROUP BY tags.title 
		ORDER BY COUNT(manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findPopularTags(limit: Int): List<TagEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id 
		WHERE tags.source = :source 
		GROUP BY tags.title
		ORDER BY COUNT(manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findPopularTags(source: String, limit: Int): List<TagEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id 
		WHERE tags.source = :source AND title LIKE :query
		GROUP BY tags.title
		ORDER BY COUNT(manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findTags(source: String, query: String, limit: Int): List<TagEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id 
		WHERE title LIKE :query
		GROUP BY tags.title
		ORDER BY COUNT(manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findTags(query: String, limit: Int): List<TagEntity>

	@Upsert
	abstract suspend fun upsert(tags: Iterable<TagEntity>)
}
