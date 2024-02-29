package org.koitharu.kotatsu.core.os

import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.SampleData
import org.koitharu.kotatsu.awaitForIdle
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.history.data.HistoryRepository
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppShortcutManagerTest {

	@get:Rule
	var hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	@Inject
	lateinit var database: MangaDatabase

	@Before
	fun setUp() {
		hiltRule.inject()
		database.clearAllTables()
	}

	@Test
	fun testUpdateShortcuts() = runTest {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			return@runTest
		}
		database.invalidationTracker.addObserver(appShortcutManager)
		awaitUpdate()
		assertTrue(getShortcuts().isEmpty())
		historyRepository.addOrUpdate(
			manga = SampleData.manga,
			chapterId = SampleData.chapter.id,
			page = 4,
			scroll = 2,
			percent = 0.3f,
			force = false,
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
		appShortcutManager.await()
	}
}
