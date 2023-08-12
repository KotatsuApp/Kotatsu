package org.koitharu.kotatsu.bookmarks.data

import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.parsers.model.Manga
import java.time.Instant

fun BookmarkEntity.toBookmark(manga: Manga) = Bookmark(
	manga = manga,
	pageId = pageId,
	chapterId = chapterId,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = Instant.ofEpochMilli(createdAt),
	percent = percent,
)

fun Bookmark.toEntity() = BookmarkEntity(
	mangaId = manga.id,
	pageId = pageId,
	chapterId = chapterId,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = createdAt.toEpochMilli(),
	percent = percent,
)

fun Collection<BookmarkEntity>.toBookmarks(manga: Manga) = map {
	it.toBookmark(manga)
}

@JvmName("bookmarksIds")
fun Collection<Bookmark>.ids() = map { it.pageId }
