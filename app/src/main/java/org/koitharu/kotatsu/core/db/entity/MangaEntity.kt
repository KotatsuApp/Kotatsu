package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga")
class MangaEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val id: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "alt_title") val altTitle: String?,
	@ColumnInfo(name = "url") val url: String,
	@ColumnInfo(name = "public_url") val publicUrl: String,
	@ColumnInfo(name = "rating") val rating: Float, // normalized value [0..1] or -1
	@ColumnInfo(name = "nsfw") val isNsfw: Boolean,
	@ColumnInfo(name = "cover_url") val coverUrl: String,
	@ColumnInfo(name = "large_cover_url") val largeCoverUrl: String?,
	@ColumnInfo(name = "state") val state: String?,
	@ColumnInfo(name = "author") val author: String?,
	@ColumnInfo(name = "source") val source: String
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaEntity

		if (id != other.id) return false
		if (title != other.title) return false
		if (altTitle != other.altTitle) return false
		if (url != other.url) return false
		if (publicUrl != other.publicUrl) return false
		if (rating != other.rating) return false
		if (isNsfw != other.isNsfw) return false
		if (coverUrl != other.coverUrl) return false
		if (largeCoverUrl != other.largeCoverUrl) return false
		if (state != other.state) return false
		if (author != other.author) return false
		if (source != other.source) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + (altTitle?.hashCode() ?: 0)
		result = 31 * result + url.hashCode()
		result = 31 * result + publicUrl.hashCode()
		result = 31 * result + rating.hashCode()
		result = 31 * result + isNsfw.hashCode()
		result = 31 * result + coverUrl.hashCode()
		result = 31 * result + (largeCoverUrl?.hashCode() ?: 0)
		result = 31 * result + (state?.hashCode() ?: 0)
		result = 31 * result + (author?.hashCode() ?: 0)
		result = 31 * result + source.hashCode()
		return result
	}
}