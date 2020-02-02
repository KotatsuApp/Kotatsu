package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.BeforeClass
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.repository.ReadmangaRuTest

abstract class RepositoryTestEnvironment {

	lateinit var repository: MangaRepository

	@BeforeClass
	fun initialize(source: MangaSource) {
		startKoin {
			modules(listOf(
				module {
					factory {
						OkHttpClient()
					}
				}, module {
					single {
						MangaLoaderContext()
					}
				}
			))
		}
		val constructor = source.cls.getConstructor(MangaLoaderContext::class.java)
		repository = constructor.newInstance(MangaLoaderContext())
	}

	fun getMangaList() = runBlocking { repository.getList(2) }

	fun getMangaItem() = runBlocking { repository.getDetails(repository.getList(4).last()) }

	fun getTags() = runBlocking { repository.getTags() }
}