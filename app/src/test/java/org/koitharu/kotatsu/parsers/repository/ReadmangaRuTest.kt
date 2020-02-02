package org.koitharu.kotatsu.parsers.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.domain.repository.ReadmangaRepository
import org.koitharu.kotatsu.parsers.MangaParserTest
import org.koitharu.kotatsu.parsers.RepositoryTestEnvironment
import org.koitharu.kotatsu.utils.TestUtil
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReadmangaRuTest : MangaParserTest {

	@Test
	override fun testMangaList() {
		val list = runBlocking { repository.getList(1) }
		Assert.assertTrue(list.size == 70)
		val item = list[40]
		Assert.assertTrue(item.title.isNotEmpty())
		Assert.assertTrue(item.rating in 0f..1f)
		TestUtil.assertValidUrl(item.url)
		TestUtil.assertValidUrl(item.coverUrl)
	}

	@Test
	override fun testMangaDetails() {
		val manga = runBlocking { repository.getDetails(repository.getList(1).last()) }
		Assert.assertNotNull(manga.largeCoverUrl)
		TestUtil.assertValidUrl(manga.largeCoverUrl!!)
		Assert.assertNotNull(manga.chapters)
		val chapter = manga.chapters!!.last()
		TestUtil.assertValidUrl(chapter.url)
	}

	@Test
	override fun testMangaPages() {
		val chapter = runBlocking { repository.getDetails(repository.getList(1).last()).chapters!!.first() }
		val pages = runBlocking { repository.getPages(chapter) }
		Assert.assertFalse(pages.isEmpty())
		TestUtil.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.first()) })
		TestUtil.assertValidUrl(runBlocking { repository.getPageFullUrl(pages.last()) })
	}

	companion object : RepositoryTestEnvironment() {

		@JvmStatic
		@BeforeClass
		fun setUp() = initialize(ReadmangaRepository::class.java)
	}
}