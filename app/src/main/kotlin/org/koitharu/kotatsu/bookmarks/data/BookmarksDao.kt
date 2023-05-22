package org.koitharu.kotatsu.bookmarks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.entity.MangaWithTags

@Dao
abstract class BookmarksDao {

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId AND page_id = :pageId")
	abstract suspend fun find(mangaId: Long, pageId: Long): BookmarkEntity?

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page")
	abstract fun observe(mangaId: Long, chapterId: Long, page: Int): Flow<BookmarkEntity?>

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId ORDER BY created_at DESC")
	abstract fun observe(mangaId: Long): Flow<List<BookmarkEntity>>

	@Transaction
	@Query(
		"SELECT * FROM manga JOIN bookmarks ON bookmarks.manga_id = manga.manga_id ORDER BY bookmarks.created_at"
	)
	abstract fun observe(): Flow<Map<MangaWithTags, List<BookmarkEntity>>>

	@Insert
	abstract suspend fun insert(entity: BookmarkEntity)

	@Delete
	abstract suspend fun delete(entity: BookmarkEntity)

	@Query("DELETE FROM bookmarks WHERE manga_id = :mangaId AND page_id = :pageId")
	abstract suspend fun delete(mangaId: Long, pageId: Long)
}