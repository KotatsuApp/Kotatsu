package org.koitharu.kotatsu.parsers.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.parsers.MangaParserTest
import org.koitharu.kotatsu.parsers.RepositoryTestEnvironment
import org.koitharu.kotatsu.utils.TestUtil
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SelfMangaTest : MangaParserTest {

	@Test
	override fun testMangaList() {
		val list = getMangaList()
		Assert.assertTrue(list.size == 70)
		val item = list[40]
		Assert.assertTrue(item.title.isNotEmpty())
		Assert.assertTrue(item.rating in 0f..1f)
		TestUtil.assertValidUrl(item.url)
		TestUtil.assertValidUrl(item.coverUrl)
		Assert.assertEquals(item.source, MangaSource.SELFMANGA)
	}

	@Test
	override fun testMangaDetails() {
		val manga = getMangaItem()
		Assert.assertNotNull(manga.largeCoverUrl)
		TestUtil.assertValidUrl(manga.largeCoverUrl!!)
		Assert.assertNotNull(manga.chapters)
		val chapter = manga.chapters!!.last()
		Assert.assertEquals(chapter.source, MangaSource.SELFMANGA)
		TestUtil.assertValidUrl(chapter.url)
	}

	@Test
	override fun testMangaPages() {
		val chapter = getMangaItem().chapters!!.first()
		val pages = runBlocking { repository.getPages(chapter) }
		Assert.assertFalse(pages.isEmpty())
		Assert.assertEquals(pages.first().source, MangaSource.SELFMANGA)
		TestUtil.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.first()) })
		TestUtil.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.last()) })
	}

	@Test
	override fun testTags() {
		val tags = getTags()
		Assert.assertFalse(tags.isEmpty())
		val tag = tags.first()
		Assert.assertFalse(tag.title.isBlank())
		Assert.assertEquals(tag.source, MangaSource.SELFMANGA)
		TestUtil.assertValidUrl("https://selfmanga.ru/list/genre/${tag.key}")
	}

	companion object : RepositoryTestEnvironment() {

		@JvmStatic
		@BeforeClass
		fun setUp() = initialize(MangaSource.SELFMANGA)
	}
}