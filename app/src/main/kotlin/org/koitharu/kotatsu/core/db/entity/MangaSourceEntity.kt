package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
	tableName = "sources",
)
data class MangaSourceEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "source")
	val source: String,
	@ColumnInfo(name = "enabled") val isEnabled: Boolean,
	@ColumnInfo(name = "sort_key", index = true) val sortKey: Int,
)
