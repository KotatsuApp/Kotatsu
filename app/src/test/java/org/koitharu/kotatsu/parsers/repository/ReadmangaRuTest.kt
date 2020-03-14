package org.koitharu.kotatsu.parsers.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.parsers.MangaParserTest
import org.koitharu.kotatsu.parsers.RepositoryTestEnvironment
import org.koitharu.kotatsu.utils.AssertX
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReadmangaRuTest : MangaParserTest {

	@Test
	override fun testMangaList() {
		val list = getMangaList()
		Assert.assertTrue(list.size == 70)
		val item = list[40]
		Assert.assertTrue(item.title.isNotEmpty())
		Assert.assertTrue(item.rating in 0f..1f)
		AssertX.assertValidUrl(item.url)
		AssertX.assertValidUrl(item.coverUrl)
		Assert.assertEquals(item.source, MangaSource.READMANGA_RU)
	}

	@Test
	override fun testMangaDetails() {
		val manga = getMangaItem()
		Assert.assertNotNull(manga.largeCoverUrl)
		AssertX.assertValidUrl(manga.largeCoverUrl!!)
		Assert.assertNotNull(manga.chapters)
		val chapter = manga.chapters!!.last()
		Assert.assertEquals(chapter.source, MangaSource.READMANGA_RU)
		AssertX.assertValidUrl(chapter.url)
	}

	@Test
	override fun testMangaPages() {
		val chapter = getMangaItem().chapters!!.first()
		val pages = runBlocking { repository.getPages(chapter) }
		Assert.assertFalse(pages.isEmpty())
		Assert.assertEquals(pages.first().source, MangaSource.READMANGA_RU)
		AssertX.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.first()) })
		AssertX.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.last()) })
	}

	@Test
	override fun testTags() {
		val tags = getTags()
		Assert.assertFalse(tags.isEmpty())
		val tag = tags.first()
		Assert.assertFalse(tag.title.isBlank())
		Assert.assertEquals(tag.source, MangaSource.READMANGA_RU)
		AssertX.assertValidUrl("https://readmanga.me/list/genre/${tag.key}")
	}

	companion object : RepositoryTestEnvironment() {

		@JvmStatic
		@BeforeClass
		fun setUp() = initialize(MangaSource.READMANGA_RU)
	}
}