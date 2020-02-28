package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaState
import org.koitharu.kotatsu.core.model.MangaTag

@Entity(tableName = "manga")
data class MangaEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val id: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "alt_title") val altTitle: String? = null,
	@ColumnInfo(name = "url") val url: String,
	@ColumnInfo(name = "rating") val rating: Float = Manga.NO_RATING, //normalized value [0..1] or -1
	@ColumnInfo(name = "cover_url") val coverUrl: String,
	@ColumnInfo(name = "large_cover_url") val largeCoverUrl: String? = null,
	@ColumnInfo(name = "state") val state: String? = null,
	@ColumnInfo(name = "author") val author: String? = null,
	@ColumnInfo(name = "source") val source: String
) {

	fun toManga(tags: Set<MangaTag> = emptySet()) = Manga(
		id = this.id,
		title = this.title,
		altTitle = this.altTitle,
		state = this.state?.let { MangaState.valueOf(it) },
		rating = this.rating,
		url = this.url,
		coverUrl = this.coverUrl,
		largeCoverUrl = this.largeCoverUrl,
		author = this.author,
		source = MangaSource.valueOf(this.source),
		tags = tags
	)

	companion object {

		fun from(manga: Manga) = MangaEntity(
			id = manga.id,
			url = manga.url,
			source = manga.source.name,
			largeCoverUrl = manga.largeCoverUrl,
			coverUrl = manga.coverUrl,
			altTitle = manga.altTitle,
			rating = manga.rating,
			state = manga.state?.name,
			title = manga.title,
			author = manga.author
		)
	}
}