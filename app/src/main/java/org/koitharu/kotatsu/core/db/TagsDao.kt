package org.koitharu.kotatsu.core.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Dao
interface TagsDao {

	@Transaction
	@Query("SELECT * FROM tags")
	fun getAllTags(): List<TagEntity>
}