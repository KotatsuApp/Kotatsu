package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.SourceConfig
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.utils.AssertX
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class RemoteRepositoryTest(source: MangaSource) {

	private val repo = MangaProviderFactory.create(source)

	@Test
	fun getList() {
		val list = runBlocking { repo.getList(60) }
		Assert.assertFalse(list.isEmpty())
		val item = list.random()
		AssertX.assertContentType(item.coverUrl, "image")
		AssertX.assertContentType(item.url, "text", "html")
		Assert.assertFalse(item.title.isBlank())
	}

	@Test
	fun search() {
		val list = runBlocking { repo.getList(0, query = "tail") }
		Assert.assertFalse(list.isEmpty())
		val item = list.random()
		AssertX.assertContentType(item.coverUrl, "image")
		AssertX.assertContentType(item.url, "text", "html")
		Assert.assertFalse(item.title.isBlank())
	}

	@Test
	fun getTags() {
		val tags = runBlocking { repo.getTags() }
		Assert.assertFalse(tags.isEmpty())
		val tag = tags.random()
		Assert.assertFalse(tag.key.isBlank())
		Assert.assertFalse(tag.title.isBlank())
		val list = runBlocking { repo.getList(0, tag = tag) }
		Assert.assertFalse(list.isEmpty())
		val item = list.random()
		AssertX.assertContentType(item.coverUrl, "image")
		AssertX.assertContentType(item.url, "text", "html")
		Assert.assertFalse(item.title.isBlank())
	}

	@Test
	fun getDetails() {
		val manga = runBlocking { repo.getList(0) }.random()
		val details = runBlocking { repo.getDetails(manga) }
		Assert.assertFalse(details.chapters.isNullOrEmpty())
		Assert.assertFalse(details.description.isNullOrEmpty())
		val chapter = details.chapters!!.random()
		Assert.assertFalse(chapter.name.isBlank())
		AssertX.assertContentType(chapter.url, "text", "html")
	}

	@Test
	fun getPages() {
		val manga = runBlocking { repo.getList(0) }.random()
		val details = runBlocking { repo.getDetails(manga) }
		val pages = runBlocking { repo.getPages(details.chapters!!.random()) }
		Assert.assertFalse(pages.isEmpty())
		val page = pages.random()
		val fullUrl = runBlocking { repo.getPageFullUrl(page) }
		AssertX.assertContentType(fullUrl, "image")
	}

	companion object {

		@JvmStatic
		@BeforeClass
		fun initialize() {
			startKoin {
				modules(listOf(
					module {
						factory {
							OkHttpClient.Builder()
								.connectTimeout(20, TimeUnit.SECONDS)
								.readTimeout(60, TimeUnit.SECONDS)
								.writeTimeout(20, TimeUnit.SECONDS)
								.build()
						}
					},
					module {
						single<MangaLoaderContext> {
							object : MangaLoaderContext() {
								override fun getSettings(source: MangaSource): SourceConfig {
									return SourceConfigMock()
								}
							}
						}
					}
				))
			}
		}

		@JvmStatic
		@Parameterized.Parameters(name = "{0}")
		fun getProviders() = (MangaSource.values().toList() - MangaSource.LOCAL).toTypedArray()
	}
}