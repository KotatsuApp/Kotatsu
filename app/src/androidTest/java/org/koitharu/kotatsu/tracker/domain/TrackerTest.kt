package org.koitharu.kotatsu.tracker.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koitharu.kotatsu.SampleData
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.parsers.model.Manga
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TrackerTest : KoinTest {

	private val repository by inject<TrackingRepository>()
	private val dataRepository by inject<MangaDataRepository>()
	private val tracker by inject<Tracker>()

	@Test
	fun noUpdates() = runTest {
		val manga = loadManga("full.json")
		tracker.deleteTrack(manga.id)

		tracker.checkUpdates(manga, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(manga.id))
		tracker.checkUpdates(manga, commit = true).apply {
			assertTrue(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(manga.id))
	}

	@Test
	fun hasUpdates() = runTest {
		val mangaFirst = loadManga("first_chapters.json")
		val mangaFull = loadManga("full.json")
		tracker.deleteTrack(mangaFirst.id)

		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assertEquals(3, newChapters.size)
		}
		assertEquals(3, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(3, repository.getNewChaptersCount(mangaFirst.id))
	}

	@Test
	fun badIds() = runTest {
		val mangaFirst = loadManga("first_chapters.json")
		val mangaBad = loadManga("bad_ids.json")
		tracker.deleteTrack(mangaFirst.id)

		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaBad, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
	}

	@Test
	fun badIds2() = runTest {
		val mangaFirst = loadManga("first_chapters.json")
		val mangaBad = loadManga("bad_ids.json")
		val mangaFull = loadManga("full.json")
		tracker.deleteTrack(mangaFirst.id)

		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assertEquals(3, newChapters.size)
		}
		assertEquals(3, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaBad, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
	}

	@Test
	fun fullReset() = runTest {
		val mangaFull = loadManga("full.json")
		val mangaFirst = loadManga("first_chapters.json")
		val mangaEmpty = loadManga("empty.json")
		tracker.deleteTrack(mangaFull.id)

		assertEquals(0, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaEmpty, commit = true).apply {
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assertEquals(3, newChapters.size)
		}
		assertEquals(3, repository.getNewChaptersCount(mangaFull.id))
		tracker.checkUpdates(mangaEmpty, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFull.id))
	}

	@Test
	fun syncWithHistory() = runTest {
		val mangaFull = loadManga("full.json")
		val mangaFirst = loadManga("first_chapters.json")
		tracker.deleteTrack(mangaFull.id)

		tracker.checkUpdates(mangaFirst, commit = true).apply {
			assertFalse(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(0, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assertEquals(3, newChapters.size)
		}
		assertEquals(3, repository.getNewChaptersCount(mangaFirst.id))

		val chapter = requireNotNull(mangaFull.chapters).run { get(lastIndex - 1) }
		repository.syncWithHistory(mangaFull, chapter.id)

		assertEquals(1, repository.getNewChaptersCount(mangaFirst.id))
		tracker.checkUpdates(mangaFull, commit = true).apply {
			assertTrue(isValid)
			assert(newChapters.isEmpty())
		}
		assertEquals(1, repository.getNewChaptersCount(mangaFirst.id))
	}

	private suspend fun loadManga(name: String): Manga {
		val manga = SampleData.loadAsset("manga/$name", Manga::class)
		dataRepository.storeManga(manga)
		return manga
	}
}