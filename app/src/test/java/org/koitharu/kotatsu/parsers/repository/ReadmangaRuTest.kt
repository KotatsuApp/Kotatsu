package org.koitharu.kotatsu.parsers.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.domain.repository.ReadmangaRepository
import org.koitharu.kotatsu.parsers.MangaParserTest
import org.koitharu.kotatsu.parsers.RepositoryTestEnvironment
import org.koitharu.kotatsu.utils.MyAsserts
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
        MyAsserts.assertValidUrl(item.url)
        MyAsserts.assertValidUrl(item.coverUrl)
    }

    companion object : RepositoryTestEnvironment() {

        @JvmStatic
        @BeforeClass
        fun setUp() = initialize(ReadmangaRepository::class.java)
    }
}