package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.utils.ext.longHashCode

@Entity(tableName = "tags")
data class TagEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "tag_id") val id: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "key") val key: String,
	@ColumnInfo(name = "source") val source: String
) {

	fun toMangaTag() = MangaTag(
		key = this.key,
		title = this.title,
		source = MangaSource.valueOf(this.source)
	)

	companion object {

		fun fromMangaTag(tag: MangaTag) = TagEntity(
			title = tag.title,
			key = tag.key,
			source = tag.source.name,
			id = "${tag.key}_${tag.source.name}".longHashCode()
		)
	}
}