package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.runBlocking
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.network.UserAgentInterceptor
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.AssertX
import org.koitharu.kotatsu.utils.ext.isDistinctBy
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class RemoteRepositoryTest(source: MangaSource) : KoinTest {

	private val repo = try {
		source.cls.getDeclaredConstructor(MangaLoaderContext::class.java)
			.newInstance(get<MangaLoaderContext>())
	} catch (e: NoSuchMethodException) {
		source.cls.newInstance()
	} as RemoteMangaRepository

	@Test
	fun list() {
		val list = runBlocking { repo.getList(60) }
		Assert.assertFalse("List is empty", list.isEmpty())
		Assert.assertTrue("Mangas are not distinct", list.isDistinctBy { it.id })
		val item = list.random()
		AssertX.assertUrlRelative("Url is not relative", item.url)
		AssertX.assertUrlAbsolute("Url is not absolute", item.coverUrl)
		AssertX.assertContentType("Bad cover at ${item.url}", item.coverUrl, "image/*")
		AssertX.assertContentType(
			"invalid public url ${item.publicUrl}",
			item.publicUrl,
			"text/html"
		)
		Assert.assertFalse("Title is blank at ${item.url}", item.title.isBlank())
	}

	@Test
	fun search() {
		val list = runBlocking { repo.getList(0, query = "tail") }
		Assert.assertFalse("List is empty", list.isEmpty())
		Assert.assertTrue("Mangas are not distinct", list.isDistinctBy { it.id })
		val item = list.random()
		AssertX.assertUrlRelative("Url is not relative", item.url)
		AssertX.assertContentType("Bad cover at ${item.url}", item.coverUrl, "image/*")
		AssertX.assertContentType(
			"invalid public url ${item.publicUrl}",
			item.publicUrl,
			"text/html"
		)
		Assert.assertFalse("Title is blank at ${item.url}", item.title.isBlank())
	}

	@Test
	fun tags() {
		val tags = runBlocking { repo.getTags() }
		Assert.assertFalse("No tags found", tags.isEmpty())
		val tag = tags.random()
		Assert.assertFalse("Tag title is blank for ${tag}", tag.key.isBlank())
		Assert.assertFalse("Tag title is blank for ${tag}", tag.title.isBlank())
		val list = runBlocking { repo.getList(0, tag = tag) }
		Assert.assertFalse("List is empty", list.isEmpty())
		val item = list.random()
		AssertX.assertUrlRelative("Url is not relative", item.url)
		AssertX.assertContentType("Bad cover at ${item.coverUrl}", item.coverUrl, "image/*")
		AssertX.assertContentType(
			"invalid public url ${item.publicUrl}",
			item.publicUrl,
			"text/html"
		)
		Assert.assertFalse("Title is blank at ${item.url}", item.title.isBlank())
	}

	@Test
	fun details() {
		val manga = runBlocking { repo.getList(0) }.random()
		val details = runBlocking { repo.getDetails(manga) }
		Assert.assertFalse("No chapters at ${details.url}", details.chapters.isNullOrEmpty())
		AssertX.assertContentType(
			"invalid public url ${details.publicUrl}",
			details.publicUrl,
			"text/html"
		)
		Assert.assertFalse(
			"Description is empty at ${details.url}",
			details.description.isNullOrEmpty()
		)
		Assert.assertTrue(
			"Chapters are not distinct",
			details.chapters.orEmpty().isDistinctBy { it.id })
		val chapter = details.chapters?.randomOrNull() ?: return
		AssertX.assertUrlRelative("Url is not relative", chapter.url)
		Assert.assertFalse(
			"Chapter name missing at ${details.url}:${chapter.number}",
			chapter.name.isBlank()
		)
	}

	@Test
	fun pages() {
		val manga = runBlocking { repo.getList(0) }.random()
		val details = runBlocking { repo.getDetails(manga) }
		val chapter = checkNotNull(details.chapters?.randomOrNull()) {
			"No chapters at ${details.url}"
		}
		val pages = runBlocking { repo.getPages(chapter) }
		Assert.assertFalse("Cannot find any page at ${chapter.url}", pages.isEmpty())
		Assert.assertTrue("Pages are not distinct", pages.isDistinctBy { it.id })
		val page = pages.randomOrNull() ?: return
		val fullUrl = runBlocking { repo.getPageUrl(page) }
		AssertX.assertContentType("Wrong page response from $fullUrl", fullUrl, "image/*")
	}

	companion object {

		@JvmStatic
		@BeforeClass
		fun initialize() {
			startKoin {
				modules(
					module {
						single<CookieJar> { TemporaryCookieJar() }
						factory {
							OkHttpClient.Builder()
								.cookieJar(get())
								.addInterceptor(UserAgentInterceptor())
								.connectTimeout(20, TimeUnit.SECONDS)
								.readTimeout(60, TimeUnit.SECONDS)
								.writeTimeout(20, TimeUnit.SECONDS)
								.build()
						}
						single<MangaLoaderContext> {
							object : MangaLoaderContext(get(), get()) {
								override fun getSettings(source: MangaSource): SourceSettings {
									return SourceSettingsMock()
								}
							}
						}
					}
				)
			}
		}

		@JvmStatic
		@Parameterized.Parameters(name = "{0}")
		fun getProviders() = (MangaSource.values().toList() - MangaSource.LOCAL).toTypedArray()
	}
}