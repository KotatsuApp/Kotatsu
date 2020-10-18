package org.koitharu.kotatsu.core.db.dao

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
abstract class TagsDao {

	@Query("SELECT * FROM tags")
	abstract suspend fun getAllTags(): List<TagEntity>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(tag: TagEntity): Long

	@Update(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun update(tag: TagEntity): Int

	@Transaction
	open suspend fun upsert(tags: Iterable<TagEntity>) {
		tags.forEach { tag ->
			if (update(tag) <= 0) {
				insert(tag)
			}
		}
	}
}