package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaState

@Entity(tableName = "manga")
data class MangaEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val id: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "localized_title") val localizedTitle: String? = null,
	@ColumnInfo(name = "url") val url: String,
	@ColumnInfo(name = "rating") val rating: Float = Manga.NO_RATING, //normalized value [0..1] or -1
	@ColumnInfo(name = "cover_url") val coverUrl: String,
	@ColumnInfo(name = "large_cover_url") val largeCoverUrl: String? = null,
	@ColumnInfo(name = "summary") val summary: String,
	@ColumnInfo(name = "state") val state: String? = null,
	@ColumnInfo(name = "source") val source: String
) {

	fun toManga() = Manga(
		id = this.id,
		title = this.title,
		localizedTitle = this.localizedTitle,
		summary = this.summary,
		state = this.state?.let { MangaState.valueOf(it) },
		rating = this.rating,
		url = this.url,
		coverUrl = this.coverUrl,
		largeCoverUrl = this.largeCoverUrl,
		source = MangaSource.valueOf(this.source)
//		tags = this.tags.map(TagEntity::toMangaTag).toSet()
	)

	companion object {

		fun from(manga: Manga) = MangaEntity(
			id = manga.id,
			url = manga.url,
			source = manga.source.name,
			largeCoverUrl = manga.largeCoverUrl,
			coverUrl = manga.coverUrl,
			localizedTitle = manga.localizedTitle,
			rating = manga.rating,
			state = manga.state?.name,
			summary = manga.summary,
//			tags = manga.tags.map(TagEntity.Companion::fromMangaTag),
			title = manga.title
		)
	}
}