package org.koitharu.kotatsu.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class TagsDao {

	@Query("SELECT * FROM tags WHERE source = :source")
	abstract suspend fun findTags(source: String): List<TagEntity>

	@Query(
		"""SELECT tags.* FROM tags
		LEFT JOIN manga_tags ON tags.tag_id = manga_tags.tag_id
		WHERE manga_tags.manga_id IN (SELECT manga_id FROM history UNION SELECT manga_id FROM favourites)
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
		WHERE tags.source = :source  
		GROUP BY tags.title
		ORDER BY COUNT(manga_id) ASC 
		LIMIT :limit""",
	)
	abstract suspend fun findRareTags(source: String, limit: Int): List<TagEntity>

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
		WHERE title LIKE :query AND manga_tags.manga_id IN (SELECT manga_id FROM history UNION SELECT manga_id FROM favourites)
		GROUP BY tags.title
		ORDER BY COUNT(manga_id) DESC 
		LIMIT :limit""",
	)
	abstract suspend fun findTags(query: String, limit: Int): List<TagEntity>

	@Query(
		"""
		SELECT tags.* FROM manga_tags 
		LEFT JOIN tags ON tags.tag_id = manga_tags.tag_id 
		WHERE manga_tags.manga_id IN (SELECT manga_id FROM manga_tags WHERE tag_id = :tagId)
		GROUP BY tags.tag_id 
		ORDER BY COUNT(manga_id) DESC;
	""",
	)
	abstract suspend fun findRelatedTags(tagId: Long): List<TagEntity>

	@Query(
		"""
		SELECT tags.* FROM manga_tags 
		LEFT JOIN tags ON tags.tag_id = manga_tags.tag_id 
		WHERE manga_tags.manga_id IN (SELECT manga_id FROM manga_tags WHERE tag_id IN (:ids))
		GROUP BY tags.tag_id 
		ORDER BY COUNT(manga_id) DESC;
	""",
	)
	abstract suspend fun findRelatedTags(ids: Set<Long>): List<TagEntity>

	@Upsert
	abstract suspend fun upsert(tags: Iterable<TagEntity>)
}
