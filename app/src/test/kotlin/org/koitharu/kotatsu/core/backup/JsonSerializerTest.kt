package org.koitharu.kotatsu.core.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.concurrent.TimeUnit

class JsonSerializerTest {

	@Test
	fun toFavouriteEntity() {
		val entity = FavouriteEntity(
			mangaId = 40,
			categoryId = 20,
			sortKey = 1,
			createdAt = System.currentTimeMillis(),
			deletedAt = 0L,
		)
		val json = JsonSerializer(entity).toJson()
		val result = JsonDeserializer(json).toFavouriteEntity()
		assertEquals(entity, result)
	}

	@Test
	fun toMangaEntity() {
		val entity = MangaEntity(
			id = 231,
			title = "Lorem Ipsum",
			altTitle = "Lorem Ispum 2",
			url = "erw",
			publicUrl = "hthth",
			rating = 0.78f,
			isNsfw = true,
			coverUrl = "5345",
			largeCoverUrl = null,
			state = MangaState.FINISHED.name,
			author = "RERE",
			source = MangaParserSource.DUMMY.name,
		)
		val json = JsonSerializer(entity).toJson()
		val result = JsonDeserializer(json).toMangaEntity()
		assertEquals(entity, result)
	}

	@Test
	fun toTagEntity() {
		val entity = TagEntity(
			id = 934023534,
			title = "Adventure",
			key = "adventure",
			source = MangaParserSource.DUMMY.name,
		)
		val json = JsonSerializer(entity).toJson()
		val result = JsonDeserializer(json).toTagEntity()
		assertEquals(entity, result)
	}

	@Test
	fun toHistoryEntity() {
		val entity = HistoryEntity(
			mangaId = 304135341,
			createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6),
			updatedAt = System.currentTimeMillis(),
			chapterId = 29014843034,
			page = 35,
			scroll = 24.0f,
			percent = 0.6f,
			deletedAt = 0L,
			chaptersCount = 12,
		)
		val json = JsonSerializer(entity).toJson()
		val result = JsonDeserializer(json).toHistoryEntity()
		assertEquals(entity, result)
	}

	@Test
	fun toFavouriteCategoryEntity() {
		val entity = FavouriteCategoryEntity(
			categoryId = 142,
			createdAt = System.currentTimeMillis(),
			sortKey = 14,
			title = "Read later",
			order = SortOrder.RATING.name,
			track = false,
			isVisibleInLibrary = true,
			deletedAt = 0L,
		)
		val json = JsonSerializer(entity).toJson()
		val result = JsonDeserializer(json).toFavouriteCategoryEntity()
		assertEquals(entity, result)
	}
}
