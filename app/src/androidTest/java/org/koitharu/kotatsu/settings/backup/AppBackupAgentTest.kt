package org.koitharu.kotatsu.settings.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import org.koitharu.kotatsu.SampleData
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class AppBackupAgentTest : KoinTest {

	private val historyRepository by inject<HistoryRepository>()
	private val favouritesRepository by inject<FavouritesRepository>()
	private val backupRepository by inject<BackupRepository>()
	private val database by inject<MangaDatabase>()

	@Before
	fun setUp() {
		database.clearAllTables()
	}

	@Test
	fun testBackupRestore() = runTest {
		val category = favouritesRepository.createCategory(
			title = SampleData.favouriteCategory.title,
			sortOrder = SampleData.favouriteCategory.order,
			isTrackerEnabled = SampleData.favouriteCategory.isTrackingEnabled,
		)
		favouritesRepository.addToCategory(categoryId = category.id, mangas = listOf(SampleData.manga))
		historyRepository.addOrUpdate(
			manga = SampleData.mangaDetails,
			chapterId = SampleData.mangaDetails.chapters!![2].id,
			page = 3,
			scroll = 40,
			percent = 0.2f,
		)
		val history = checkNotNull(historyRepository.getOne(SampleData.manga))

		val agent = AppBackupAgent()
		val backup = agent.createBackupFile(get(), backupRepository)

		database.clearAllTables()
		assertTrue(favouritesRepository.getAllManga().isEmpty())
		assertNull(historyRepository.getLastOrNull())

		backup.inputStream().use {
			agent.restoreBackupFile(it.fd, backup.length(), backupRepository)
		}

		assertEquals(category, favouritesRepository.getCategory(category.id))
		assertEquals(history, historyRepository.getOne(SampleData.manga))
		assertContentEquals(listOf(SampleData.manga), favouritesRepository.getManga(category.id))

		val allTags = database.tagsDao.findTags(SampleData.tag.source.name).toMangaTags()
		assertContains(allTags, SampleData.tag)
	}
}