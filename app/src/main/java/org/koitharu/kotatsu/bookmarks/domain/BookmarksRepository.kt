package org.koitharu.kotatsu.bookmarks.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.bookmarks.data.toBookmark
import org.koitharu.kotatsu.bookmarks.data.toEntity
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.mapItems

class BookmarksRepository(
	private val db: MangaDatabase,
) {

	fun observeBookmark(manga: Manga, chapterId: Long, page: Int): Flow<Bookmark?> {
		return db.bookmarksDao.observe(manga.id, chapterId, page).map { it?.toBookmark(manga) }
	}

	fun observeBookmarks(manga: Manga): Flow<List<Bookmark>> {
		return db.bookmarksDao.observe(manga.id).mapItems { it.toBookmark(manga) }
	}

	suspend fun addBookmark(bookmark: Bookmark) {
		db.withTransaction {
			val tags = bookmark.manga.tags.toEntities()
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(bookmark.manga.toEntity(), tags)
			db.bookmarksDao.insert(bookmark.toEntity())
		}
	}

	suspend fun removeBookmark(mangaId: Long, pageId: Long) {
		db.bookmarksDao.delete(mangaId, pageId)
	}
}