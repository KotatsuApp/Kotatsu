package org.koitharu.kotatsu.core.os

import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koitharu.kotatsu.SampleData
import org.koitharu.kotatsu.awaitForIdle
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.history.domain.HistoryRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ShortcutsUpdaterTest : KoinTest {

	private val historyRepository by inject<HistoryRepository>()
	private val shortcutsUpdater by inject<ShortcutsUpdater>()
	private val database by inject<MangaDatabase>()

	@Before
	fun setUp() {
		database.clearAllTables()
	}

	@Test
	fun testUpdateShortcuts() = runTest {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			return@runTest
		}
		awaitUpdate()
		assertTrue(getShortcuts().isEmpty())
		historyRepository.addOrUpdate(
			manga = SampleData.manga,
			chapterId = SampleData.chapter.id,
			page = 4,
			scroll = 2,
			percent = 0.3f
		)
		awaitUpdate()

		val shortcuts = getShortcuts()
		assertEquals(1, shortcuts.size)
	}

	private fun getShortcuts(): List<ShortcutInfo> {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val manager = checkNotNull(context.getSystemService<ShortcutManager>())
		return manager.dynamicShortcuts.filterNot { it.id == "com.squareup.leakcanary.dynamic_shortcut" }
	}

	private suspend fun awaitUpdate() {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.awaitForIdle()
		shortcutsUpdater.await()
	}
}