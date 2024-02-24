package org.koitharu.kotatsu.settings.backup

import android.content.res.AssetManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.SampleData
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppBackupAgentTest {

	@get:Rule
	var hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var favouritesRepository: FavouritesRepository

	@Inject
	lateinit var backupRepository: BackupRepository

	@Inject
	lateinit var database: MangaDatabase

	@Before
	fun setUp() {
		hiltRule.inject()
		database.clearAllTables()
	}

	@Test
	fun backupAndRestore() = runTest {
		val category = favouritesRepository.createCategory(
			title = SampleData.favouriteCategory.title,
			sortOrder = SampleData.favouriteCategory.order,
			isTrackerEnabled = SampleData.favouriteCategory.isTrackingEnabled,
			isVisibleOnShelf = SampleData.favouriteCategory.isVisibleInLibrary,
		)
		favouritesRepository.addToCategory(categoryId = category.id, mangas = listOf(SampleData.manga))
		historyRepository.addOrUpdate(
			manga = SampleData.mangaDetails,
			chapterId = SampleData.mangaDetails.chapters!![2].id,
			page = 3,
			scroll = 40,
			percent = 0.2f,
			force = false,
		)
		val history = checkNotNull(historyRepository.getOne(SampleData.manga))

		val agent = AppBackupAgent()
		val backup = agent.createBackupFile(
			context = InstrumentationRegistry.getInstrumentation().targetContext,
			repository = backupRepository,
		)

		database.clearAllTables()
		assertTrue(favouritesRepository.getAllManga().isEmpty())
		assertNull(historyRepository.getLastOrNull())

		backup.inputStream().use {
			agent.restoreBackupFile(it.fd, backup.length(), backupRepository)
		}

		assertEquals(category, favouritesRepository.getCategory(category.id))
		assertEquals(history, historyRepository.getOne(SampleData.manga))
		assertEquals(listOf(SampleData.manga), favouritesRepository.getManga(category.id))

		val allTags = database.getTagsDao().findTags(SampleData.tag.source.name).toMangaTags()
		assertTrue(SampleData.tag in allTags)
	}

	@Test
	fun restoreOldBackup() {
		val agent = AppBackupAgent()
		val backup = File.createTempFile("backup_", ".tmp")
		InstrumentationRegistry.getInstrumentation().context.assets
			.open("kotatsu_test.bak", AssetManager.ACCESS_STREAMING)
			.use { input ->
				backup.outputStream().use { output ->
					input.copyTo(output)
				}
			}
		backup.inputStream().use {
			agent.restoreBackupFile(it.fd, backup.length(), backupRepository)
		}
		runTest {
			assertEquals(6, historyRepository.observeAll().first().size)
			assertEquals(2, favouritesRepository.observeCategories().first().size)
			assertEquals(15, favouritesRepository.getAllManga().size)
		}
	}
}
