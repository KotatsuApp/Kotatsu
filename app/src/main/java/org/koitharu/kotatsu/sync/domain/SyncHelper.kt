package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.content.*
import android.database.Cursor
import android.net.Uri
import androidx.annotation.WorkerThread
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_HISTORY
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_MANGA
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_MANGA_TAGS
import org.koitharu.kotatsu.core.db.MangaDatabase.Companion.TABLE_TAGS
import org.koitharu.kotatsu.parsers.util.json.mapJSONTo
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.sync.data.AccountInterceptor
import org.koitharu.kotatsu.utils.GZipInterceptor
import org.koitharu.kotatsu.utils.ext.toContentValues
import org.koitharu.kotatsu.utils.ext.toJson
import org.koitharu.kotatsu.utils.ext.toRequestBody

private const val AUTHORITY_HISTORY = "org.koitharu.kotatsu.history"
private const val AUTHORITY_FAVOURITES = "org.koitharu.kotatsu.favourites"

private const val FIELD_TIMESTAMP = "timestamp"

/**
 * Warning! This class may be used in another process
 */
@WorkerThread
class SyncHelper(
	context: Context,
	account: Account,
	private val provider: ContentProviderClient,
) {

	private val httpClient = OkHttpClient.Builder()
		.addInterceptor(AccountInterceptor(context, account))
		.addInterceptor(GZipInterceptor())
		.build()
	private val baseUrl = context.getString(R.string.url_sync_server)

	fun syncFavourites(syncResult: SyncResult) {
		val data = JSONObject()
		data.put(TABLE_FAVOURITE_CATEGORIES, getFavouriteCategories())
		data.put(TABLE_FAVOURITES, getFavourites())
		data.put(FIELD_TIMESTAMP, System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_FAVOURITES")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().parseJson()
		val timestamp = response.getLong(FIELD_TIMESTAMP)
		val categoriesResult = upsertFavouriteCategories(response.getJSONArray(TABLE_FAVOURITE_CATEGORIES), timestamp)
		syncResult.stats.numDeletes += categoriesResult.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts += categoriesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
		val favouritesResult = upsertFavourites(response.getJSONArray(TABLE_FAVOURITES), timestamp)
		syncResult.stats.numDeletes += favouritesResult.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts += favouritesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
	}

	fun syncHistory(syncResult: SyncResult) {
		val data = JSONObject()
		data.put(TABLE_HISTORY, getHistory())
		data.put(FIELD_TIMESTAMP, System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_HISTORY")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().parseJson()
		val result = upsertHistory(
			json = response.getJSONArray(TABLE_HISTORY),
			timestamp = response.getLong(FIELD_TIMESTAMP),
		)
		syncResult.stats.numDeletes += result.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts += result.drop(1).sumOf { it.count?.toLong() ?: 0L }
	}

	private fun upsertHistory(json: JSONArray, timestamp: Long): Array<ContentProviderResult> {
		val uri = uri(AUTHORITY_HISTORY, TABLE_HISTORY)
		val operations = ArrayList<ContentProviderOperation>()
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("updated_at < ?", arrayOf(timestamp.toString()))
			.build()
		json.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavouriteCategories(json: JSONArray, timestamp: Long): Array<ContentProviderResult> {
		val uri = uri(AUTHORITY_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)
		val operations = ArrayList<ContentProviderOperation>()
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("created_at < ?", arrayOf(timestamp.toString()))
			.build()
		json.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavourites(json: JSONArray, timestamp: Long): Array<ContentProviderResult> {
		val uri = uri(AUTHORITY_FAVOURITES, TABLE_FAVOURITES)
		val operations = ArrayList<ContentProviderOperation>()
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("created_at < ?", arrayOf(timestamp.toString()))
			.build()
		json.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun getHistory(): JSONArray {
		return provider.query(AUTHORITY_HISTORY, TABLE_HISTORY).use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(AUTHORITY_HISTORY, jo.getLong("manga_id")))
					json.put(jo)
				} while (cursor.moveToNext())
			}
			json
		}
	}

	private fun getFavourites(): JSONArray {
		return provider.query(AUTHORITY_FAVOURITES, TABLE_FAVOURITES).use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(AUTHORITY_FAVOURITES, jo.getLong("manga_id")))
					json.put(jo)
				} while (cursor.moveToNext())
			}
			json
		}
	}

	private fun getFavouriteCategories(): JSONArray {
		return provider.query(AUTHORITY_FAVOURITES, TABLE_FAVOURITE_CATEGORIES).use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					json.put(cursor.toJson())
				} while (cursor.moveToNext())
			}
			json
		}
	}

	private fun getManga(authority: String, id: Long): JSONObject {
		val manga = provider.query(
			uri(authority, TABLE_MANGA),
			null,
			"manga_id = ?",
			arrayOf(id.toString()),
			null,
		)?.use { cursor ->
			cursor.moveToFirst()
			cursor.toJson()
		}
		requireNotNull(manga)
		val tags = provider.query(
			uri(authority, TABLE_MANGA_TAGS),
			arrayOf("tag_id"),
			"manga_id = ?",
			arrayOf(id.toString()),
			null,
		)?.use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val tagId = cursor.getLong(0)
					json.put(getTag(authority, tagId))
				} while (cursor.moveToNext())
			}
			json
		}
		manga.put("tags", requireNotNull(tags))
		return manga
	}

	private fun getTag(authority: String, tagId: Long): JSONObject {
		val tag = provider.query(
			uri(authority, TABLE_TAGS),
			null,
			"tag_id = ?",
			arrayOf(tagId.toString()),
			null,
		)?.use { cursor ->
			if (cursor.moveToFirst()) {
				cursor.toJson()
			} else {
				null
			}
		}
		return requireNotNull(tag)
	}

	private fun ContentProviderClient.query(authority: String, table: String): Cursor {
		val uri = uri(authority, table)
		return query(uri, null, null, null, null)
			?: throw OperationApplicationException("Query failed: $uri")
	}

	private fun uri(authority: String, table: String) = Uri.parse("content://$authority/$table")
}