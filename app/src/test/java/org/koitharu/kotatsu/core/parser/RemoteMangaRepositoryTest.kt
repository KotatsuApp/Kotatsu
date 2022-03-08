package org.koitharu.kotatsu.core.parser

import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.component.inject
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.utils.CoroutineTestRule
import org.koitharu.kotatsu.utils.TestResponse
import org.koitharu.kotatsu.utils.ext.mapToSet
import org.koitharu.kotatsu.utils.ext.medianOrNull
import org.koitharu.kotatsu.utils.isAbsoluteUrl
import org.koitharu.kotatsu.utils.isNotAbsoluteUrl

@RunWith(Parameterized::class)
class RemoteMangaRepositoryTest(private val source: MangaSource) : KoinTest {

	private val repo by inject<RemoteMangaRepository> {
		parametersOf(source)
	}

	@get:Rule
	val koinTestRule = KoinTestRule.create {
		printLogger(Level.ERROR)
		modules(repositoryTestModule, parserModule)
	}

	@get:Rule
	val coroutineTestRule = CoroutineTestRule()

	@Test
	fun list() = coroutineTestRule.runBlockingTest {
		val list = repo.getList2(20, query = null, sortOrder = SortOrder.POPULARITY, tags = null)
		checkMangaList(list)
	}

	@Test
	fun search() = coroutineTestRule.runBlockingTest {
		val subject = repo.getList2(20, query = null, sortOrder = SortOrder.POPULARITY, tags = null)
			.first()
		val list = repo.getList2(offset = 0, query = subject.title, sortOrder = null, tags = null)
		checkMangaList(list)
		Truth.assertThat(list.map { it.url }).contains(subject.url)
	}

	@Test
	fun tags() = coroutineTestRule.runBlockingTest {
		val tags = repo.getTags()
		Truth.assertThat(tags).isNotEmpty()
		val keys = tags.map { it.key }
		Truth.assertThat(keys).containsNoDuplicates()
		Truth.assertThat(keys).doesNotContain("")
		val titles = tags.map { it.title }
		Truth.assertThat(titles).containsNoDuplicates()
		Truth.assertThat(titles).doesNotContain("")
		Truth.assertThat(tags.mapToSet { it.source }).containsExactly(source)

		val list = repo.getList2(offset = 0, tags = setOf(tags.last()), query = null, sortOrder = null)
		checkMangaList(list)
	}

	@Test
	fun details() = coroutineTestRule.runBlockingTest {
		val list = repo.getList2(20, query = null, sortOrder = SortOrder.POPULARITY, tags = null)
		val manga = list.first()
		println(manga.title + ": " + manga.url)
		val details = repo.getDetails(manga)

		Truth.assertThat(details.chapters).isNotEmpty()
		Truth.assertThat(details.publicUrl).isAbsoluteUrl()
		Truth.assertThat(details.description).isNotNull()
		Truth.assertThat(details.title).startsWith(manga.title)
		Truth.assertThat(details.source).isEqualTo(source)

		Truth.assertThat(details.chapters?.map { it.id }).containsNoDuplicates()
		Truth.assertThat(details.chapters?.map { it.number }).containsNoDuplicates()
		Truth.assertThat(details.chapters?.map { it.name }).doesNotContain("")
		Truth.assertThat(details.chapters?.mapToSet { it.source }).containsExactly(source)
	}

	@Test
	fun pages() = coroutineTestRule.runBlockingTest {
		val list = repo.getList2(20, query = null, sortOrder = SortOrder.POPULARITY, tags = null)
		val manga = list.first()
		println(manga.title + ": " + manga.url)
		val chapter = repo.getDetails(manga).chapters?.firstOrNull() ?: error("Chapter is null")
		val pages = repo.getPages(chapter)

		Truth.assertThat(pages).isNotEmpty()
		Truth.assertThat(pages.map { it.id }).containsNoDuplicates()
		Truth.assertThat(pages.mapToSet { it.source }).containsExactly(source)

		val page = pages.medianOrNull() ?: error("No page")
		val pageUrl = repo.getPageUrl(page)
		Truth.assertThat(pageUrl).isNotEmpty()
		Truth.assertThat(pageUrl).isAbsoluteUrl()
		val pageResponse = TestResponse.testRequest(pageUrl)
		Truth.assertThat(pageResponse.code).isIn(200..299)
		Truth.assertThat(pageResponse.type).isEqualTo("image")
	}

	private fun checkMangaList(list: List<Manga>) {
		Truth.assertThat(list).isNotEmpty()
		Truth.assertThat(list.map { it.id }).containsNoDuplicates()
		for (item in list) {
			Truth.assertThat(item.url).isNotEmpty()
			Truth.assertThat(item.url).isNotAbsoluteUrl()
			Truth.assertThat(item.coverUrl).isAbsoluteUrl()
			Truth.assertThat(item.title).isNotEmpty()
			Truth.assertThat(item.publicUrl).isAbsoluteUrl()
		}
	}

	companion object {

		@JvmStatic
		@Parameterized.Parameters(name = "{0}")
		fun getProviders() = (MangaSource.values().toList() - MangaSource.LOCAL).toTypedArray()
	}
}