package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
class TagEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "tag_id") val id: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "key") val key: String,
	@ColumnInfo(name = "source") val source: String
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as TagEntity

		if (id != other.id) return false
		if (title != other.title) return false
		if (key != other.key) return false
		if (source != other.source) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + key.hashCode()
		result = 31 * result + source.hashCode()
		return result
	}
}