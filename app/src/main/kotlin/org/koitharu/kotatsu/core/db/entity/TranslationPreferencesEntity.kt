package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.koitharu.kotatsu.core.db.TABLE_TRANSLATION_PREFERENCES

@Entity(
	tableName = TABLE_TRANSLATION_PREFERENCES,
	primaryKeys = ["manga_id", "branch"],
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [
		Index(value = ["manga_id"]),
	],
)
data class TranslationPreferencesEntity(
	@ColumnInfo(name = "manga_id")
	val mangaId: Long,
	@ColumnInfo(name = "branch")
	val branch: String,
	@ColumnInfo(name = "priority")
	val priority: Int, // Lower number = higher priority (0 = highest)
	@ColumnInfo(name = "is_enabled")
	val isEnabled: Boolean,
	@ColumnInfo(name = "last_used")
	val lastUsed: Long?, // Timestamp when this translation was last used
)