package org.koitharu.kotatsu.suggestions.data

import androidx.annotation.FloatRange
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.db.entity.MangaEntity

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
class SuggestionEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@FloatRange(from = 0.0, to = 1.0)
	@ColumnInfo(name = "relevance") val relevance: Float,
	@ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)