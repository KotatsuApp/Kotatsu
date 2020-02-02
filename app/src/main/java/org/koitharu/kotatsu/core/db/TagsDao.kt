package org.koitharu.kotatsu.core.db

import androidx.room.*
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
interface TagsDao {

	@Query("SELECT * FROM tags")
	suspend fun getAllTags(): List<TagEntity>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insert(tag: TagEntity): Long

	@Update(onConflict = OnConflictStrategy.IGNORE)
	suspend fun update(tag: TagEntity): Int

	@Transaction
	suspend fun upsert(tags: Iterable<TagEntity>) {
		tags.forEach { tag ->
			if (update(tag) <= 0) {
				insert(tag)
			}
		}
	}
}