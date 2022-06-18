package org.koitharu.kotatsu.tracker.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.source
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga

@RunWith(AndroidJUnit4::class)
class TrackerTest : KoinTest {

	private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
	private val mangaAdapter = moshi.adapter(Manga::class.java)
	private val historyRegistry by inject<HistoryRepository>()
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
		val assets = InstrumentationRegistry.getInstrumentation().context.assets
		val manga = assets.open("manga/$name").use {
			mangaAdapter.fromJson(it.source().buffer())
		} ?: throw RuntimeException("Cannot read manga from json \"$name\"")
		dataRepository.storeManga(manga)
		return manga
	}
}