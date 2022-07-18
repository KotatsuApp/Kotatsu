package org.koitharu.kotatsu.core.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koitharu.kotatsu.core.db.migrations.*
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MangaDatabaseTest {

	@get:Rule
	val helper: MigrationTestHelper = MigrationTestHelper(
		InstrumentationRegistry.getInstrumentation(),
		MangaDatabase::class.java,
	)

	@Test
	@Throws(IOException::class)
	fun migrateAll() {
		assertEquals(DATABASE_VERSION, migrations.last().endVersion)
		helper.createDatabase(TEST_DB, 1).close()
		for (migration in migrations) {
			helper.runMigrationsAndValidate(
				TEST_DB,
				migration.endVersion,
				true,
				migration
			).close()
		}
	}

	private companion object {

		const val TEST_DB = "test-db"

		val migrations = arrayOf(
			Migration1To2(),
			Migration2To3(),
			Migration3To4(),
			Migration4To5(),
			Migration5To6(),
			Migration6To7(),
			Migration7To8(),
			Migration8To9(),
			Migration9To10(),
			Migration10To11(),
			Migration11To12(),
		)
	}
}