package org.koitharu.kotatsu.parsers

import okhttp3.OkHttpClient
import org.junit.BeforeClass
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.MangaRepository

abstract class RepositoryTestEnvironment {

    lateinit var repository: MangaRepository

    @BeforeClass
    fun initialize(cls: Class<out MangaRepository>) {
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
        val constructor = cls.getConstructor(MangaLoaderContext::class.java)
        repository = constructor.newInstance(MangaLoaderContext())
    }
}