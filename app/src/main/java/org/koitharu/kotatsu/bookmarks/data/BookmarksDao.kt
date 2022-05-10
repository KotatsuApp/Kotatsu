package org.koitharu.kotatsu.bookmarks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BookmarksDao {

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page")
	abstract fun observe(mangaId: Long, chapterId: Long, page: Int): Flow<BookmarkEntity?>

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId ORDER BY created_at DESC")
	abstract fun observe(mangaId: Long): Flow<List<BookmarkEntity>>

	@Insert
	abstract suspend fun insert(entity: BookmarkEntity)

	@Delete
	abstract suspend fun delete(entity: BookmarkEntity)

	@Query("DELETE FROM bookmarks WHERE manga_id = :mangaId AND page_id = :pageId")
	abstract suspend fun delete(mangaId: Long, pageId: Long)
}