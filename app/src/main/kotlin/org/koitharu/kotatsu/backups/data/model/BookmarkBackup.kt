package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.bookmarks.data.BookmarkEntity
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.parsers.util.mapToSet

@Serializable
class BookmarkBackup(
	@SerialName("manga") val manga: MangaBackup,
	@SerialName("tags") val tags: Set<TagBackup>,
	@SerialName("bookmarks") val bookmarks: List<Bookmark>,
) {

	@Serializable
	class Bookmark(
		@SerialName("manga_id") val mangaId: Long,
		@SerialName("page_id") val pageId: Long,
		@SerialName("chapter_id") val chapterId: Long,
		@SerialName("page") val page: Int,
		@SerialName("scroll") val scroll: Int,
		@SerialName("image_url") val imageUrl: String,
		@SerialName("created_at") val createdAt: Long,
		@SerialName("percent") val percent: Float,
	) {

		fun toEntity() = BookmarkEntity(
			mangaId = mangaId,
			pageId = pageId,
			chapterId = chapterId,
			page = page,
			scroll = scroll,
			imageUrl = imageUrl,
			createdAt = createdAt,
			percent = percent,
		)
	}

	constructor(manga: MangaWithTags, entities: List<BookmarkEntity>) : this(
		manga = MangaBackup(manga.copy(tags = emptyList())),
		tags = manga.tags.mapToSet { TagBackup(it) },
		bookmarks = entities.map {
			Bookmark(
				mangaId = it.mangaId,
				pageId = it.pageId,
				chapterId = it.chapterId,
				page = it.page,
				scroll = it.scroll,
				imageUrl = it.imageUrl,
				createdAt = it.createdAt,
				percent = it.percent,
			)
		},
	)
}
