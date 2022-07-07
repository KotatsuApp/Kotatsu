package org.koitharu.kotatsu.bookmarks.domain

import android.database.SQLException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.bookmarks.data.BookmarkEntity
import org.koitharu.kotatsu.bookmarks.data.toBookmark
import org.koitharu.kotatsu.bookmarks.data.toBookmarks
import org.koitharu.kotatsu.bookmarks.data.toEntity
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.mapItems
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

class BookmarksRepository(
	private val db: MangaDatabase,
) {

	fun observeBookmark(manga: Manga, chapterId: Long, page: Int): Flow<Bookmark?> {
		return db.bookmarksDao.observe(manga.id, chapterId, page).map { it?.toBookmark(manga) }
	}

	fun observeBookmarks(manga: Manga): Flow<List<Bookmark>> {
		return db.bookmarksDao.observe(manga.id).mapItems { it.toBookmark(manga) }
	}

	fun observeBookmarks(): Flow<Map<Manga, List<Bookmark>>> {
		return db.bookmarksDao.observe().map { map ->
			val res = LinkedHashMap<Manga, List<Bookmark>>(map.size)
			for ((k, v) in map) {
				val manga = k.toManga()
				res[manga] = v.toBookmarks(manga)
			}
			res
		}
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

	suspend fun removeBookmarks(ids: Map<Manga, Set<Long>>): ReversibleHandle {
		val entities = ArrayList<BookmarkEntity>(ids.size)
		db.withTransaction {
			val dao = db.bookmarksDao
			for ((manga, idSet) in ids) {
				for (pageId in idSet) {
					val e = dao.find(manga.id, pageId)
					if (e != null) {
						entities.add(e)
					}
					dao.delete(manga.id, pageId)
				}
			}
		}
		return BookmarksRestorer(entities)
	}

	private inner class BookmarksRestorer(
		private val entities: Collection<BookmarkEntity>,
	) : ReversibleHandle {

		override suspend fun reverse() {
			db.withTransaction {
				for (e in entities) {
					try {
						db.bookmarksDao.insert(e)
					} catch (e: SQLException) {
						e.printStackTraceDebug()
					}
				}
			}
		}
	}
}