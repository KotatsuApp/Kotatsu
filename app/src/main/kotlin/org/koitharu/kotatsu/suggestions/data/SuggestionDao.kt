package org.koitharu.kotatsu.suggestions.data

import android.database.DatabaseUtils.sqlEscapeString
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaQueryBuilder
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.list.domain.ListFilterOption

@Dao
abstract class SuggestionDao : MangaQueryBuilder.ConditionCallback {

	@Transaction
	@Query("SELECT * FROM suggestions ORDER BY relevance DESC")
	abstract fun observeAll(): Flow<List<SuggestionWithManga>>

	fun observeAll(
		limit: Int,
		filterOptions: Collection<ListFilterOption>
	): Flow<List<SuggestionWithManga>> = observeAllImpl(
		MangaQueryBuilder("suggestions", this)
			.filters(filterOptions)
			.orderBy("relevance DESC")
			.limit(limit)
			.build(),
	)

	@Transaction
	@Query("SELECT manga.* FROM suggestions LEFT JOIN manga ON manga.manga_id = suggestions.manga_id ORDER BY relevance DESC LIMIT :limit")
	abstract suspend fun getTopManga(limit: Int): List<MangaWithTags>

	@Transaction
	open suspend fun getRandom(limit: Int): List<MangaWithTags> {
		val ids = getRandomIds(limit)
		return getByIds(ids)
	}

	@Query("SELECT COUNT(*) FROM suggestions")
	abstract suspend fun count(): Int

	@Query("SELECT manga.title FROM suggestions LEFT JOIN manga ON suggestions.manga_id = manga.manga_id WHERE manga.title LIKE :query")
	abstract suspend fun getTitles(query: String): List<String>

	@Query("SELECT tags.* FROM suggestions LEFT JOIN tags ON (tag_id IN (SELECT tag_id FROM manga_tags WHERE manga_tags.manga_id = suggestions.manga_id)) GROUP BY tag_id ORDER BY COUNT(tags.tag_id) DESC LIMIT :limit")
	abstract suspend fun getTopTags(limit: Int): List<TagEntity>

	@Query("SELECT manga.source AS count FROM suggestions LEFT JOIN manga ON manga.manga_id = suggestions.manga_id GROUP BY manga.source ORDER BY COUNT(manga.source) DESC LIMIT :limit")
	abstract suspend fun getTopSources(limit: Int): List<String>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract suspend fun insert(entity: SuggestionEntity): Long

	@Update
	abstract suspend fun update(entity: SuggestionEntity): Int

	@Query("DELETE FROM suggestions")
	abstract suspend fun deleteAll()

	@Transaction
	open suspend fun upsert(entity: SuggestionEntity) {
		if (update(entity) == 0) {
			insert(entity)
		}
	}

	@Query("SELECT * FROM manga WHERE manga_id IN (:ids)")
	protected abstract suspend fun getByIds(ids: LongArray): List<MangaWithTags>

	@Query("SELECT manga_id FROM suggestions ORDER BY RANDOM() LIMIT :limit")
	protected abstract suspend fun getRandomIds(limit: Int): LongArray

	@Transaction
	@RawQuery(observedEntities = [SuggestionEntity::class])
	protected abstract fun observeAllImpl(query: SupportSQLiteQuery): Flow<List<SuggestionWithManga>>

	override fun getCondition(option: ListFilterOption): String? = when (option) {
		ListFilterOption.Macro.NSFW -> "(SELECT nsfw FROM manga WHERE manga.manga_id = suggestions.manga_id) = 1"
		is ListFilterOption.Tag -> "EXISTS(SELECT * FROM manga_tags WHERE manga_tags.manga_id = suggestions.manga_id AND tag_id = ${option.tagId})"
		is ListFilterOption.Source -> "(SELECT source FROM manga WHERE manga.manga_id = suggestions.manga_id) = ${
			sqlEscapeString(
				option.mangaSource.name,
			)
		}"

		else -> null
	}
}
