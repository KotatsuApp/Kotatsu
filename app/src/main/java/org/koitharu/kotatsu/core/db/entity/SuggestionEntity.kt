package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
	tableName = "suggestions",
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
data class SuggestionEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "relevance") val relevance: Float,
	@ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
