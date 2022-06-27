package org.koitharu.kotatsu.bookmarks.data

import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.*

fun BookmarkWithManga.toBookmark() = bookmark.toBookmark(
	manga.toManga(tags.toMangaTags())
)

fun BookmarkEntity.toBookmark(manga: Manga) = Bookmark(
	manga = manga,
	pageId = pageId,
	chapterId = chapterId,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = Date(createdAt),
	percent = percent,
)

fun Bookmark.toEntity() = BookmarkEntity(
	mangaId = manga.id,
	pageId = pageId,
	chapterId = chapterId,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = createdAt.time,
	percent = percent,
)