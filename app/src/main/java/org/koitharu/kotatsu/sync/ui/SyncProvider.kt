package org.koitharu.kotatsu.sync.ui

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.core.db.*
import java.util.concurrent.Callable

abstract class SyncProvider : ContentProvider() {

	private val database by inject<MangaDatabase>()
	private val supportedTables = setOf(
		TABLE_FAVOURITES,
		TABLE_MANGA,
		TABLE_TAGS,
		TABLE_FAVOURITE_CATEGORIES,
		TABLE_HISTORY,
		TABLE_MANGA_TAGS,
	)

	override fun onCreate(): Boolean {
		return true
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	): Cursor? = if (getTableName(uri) != null) {
		val sqlQuery = SupportSQLiteQueryBuilder.builder(uri.lastPathSegment)
			.columns(projection)
			.selection(selection, selectionArgs)
			.orderBy(sortOrder)
			.create()
		database.openHelper.readableDatabase.query(sqlQuery)
	} else {
		null
	}

	override fun getType(uri: Uri): String? {
		return getTableName(uri)?.let { "vnd.android.cursor.dir/" }
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		val table = getTableName(uri)
		if (values == null || table == null) {
			return null
		}
		val db = database.openHelper.writableDatabase
		db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
		return null
	}

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		val table = getTableName(uri)
		if (table == null) {
			return 0
		}
		return database.openHelper.writableDatabase.delete(table, selection, selectionArgs)
	}

	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
		val table = getTableName(uri)
		if (values == null || table == null) {
			return 0
		}
		return database.openHelper.writableDatabase
			.update(table, SQLiteDatabase.CONFLICT_IGNORE, values, selection, selectionArgs)
	}

	override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
		return database.runInTransaction(Callable { super.applyBatch(operations) })
	}

	override fun bulkInsert(uri: Uri, values: Array<out ContentValues>): Int {
		return database.runInTransaction(Callable { super.bulkInsert(uri, values) })
	}

	private fun getTableName(uri: Uri): String? {
		return uri.pathSegments.singleOrNull()?.takeIf { it in supportedTables }
	}
}