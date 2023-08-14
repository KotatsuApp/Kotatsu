package org.koitharu.kotatsu.core.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaDatabaseTest {

	@get:Rule
	val helper: MigrationTestHelper = MigrationTestHelper(
		InstrumentationRegistry.getInstrumentation(),
		MangaDatabase::class.java,
	)

	private val migrations = getDatabaseMigrations(InstrumentationRegistry.getInstrumentation().targetContext)

	@Test
	fun versions() {
		assertEquals(1, migrations.first().startVersion)
		repeat(migrations.size) { i ->
			assertEquals(i + 1, migrations[i].startVersion)
			assertEquals(i + 2, migrations[i].endVersion)
		}
		assertEquals(DATABASE_VERSION, migrations.last().endVersion)
	}

	@Test
	fun migrateAll() {
		helper.createDatabase(TEST_DB, 1).close()
		for (migration in migrations) {
			helper.runMigrationsAndValidate(
				TEST_DB,
				migration.endVersion,
				true,
				migration,
			).close()
		}
	}

	@Test
	fun prePopulate() {
		val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
		helper.createDatabase(TEST_DB, DATABASE_VERSION).use {
			DatabasePrePopulateCallback(resources).onCreate(it)
		}
	}

	private companion object {

		const val TEST_DB = "test-db"
	}
}
