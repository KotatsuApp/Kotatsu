package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.Context
import android.content.SyncResult
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
import org.koitharu.kotatsu.utils.ext.toContentValues
import org.koitharu.kotatsu.utils.ext.toJson
import org.koitharu.kotatsu.utils.ext.toRequestBody

private const val AUTHORITY_HISTORY = "org.koitharu.kotatsu.history"
private const val AUTHORITY_FAVOURITES = "org.koitharu.kotatsu.favourites"
/**
 * Warning! This class may be used in another process
 */
class SyncRepository(
	context: Context,
	account: Account,
	private val provider: ContentProviderClient,
) {

	private val httpClient = OkHttpClient.Builder()
		.addInterceptor(AccountInterceptor(context, account))
		.build()
	private val baseUrl = context.getString(R.string.url_sync_server)

	@WorkerThread
	fun syncFavouriteCategories(syncResult: SyncResult) {
		val uri = uri(AUTHORITY_FAVOURITES, TABLE_FAVOURITE_CATEGORIES)
		val data = JSONObject()
		provider.query(uri, null, null, null, null)?.use { cursor ->
			val favourites = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					favourites.put(cursor.toJson())
				} while (cursor.moveToNext())
			}
			data.put(TABLE_FAVOURITES, favourites)
		}
		data.put("timestamp", System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_FAVOURITE_CATEGORIES")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().parseJson()
		val operations = ArrayList<ContentProviderOperation>()
		val timestamp = response.getLong("timestamp")
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("created_at < ?", arrayOf(timestamp.toString()))
			.build()
		val ja = response.getJSONArray(TABLE_FAVOURITE_CATEGORIES)
		ja.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}

		val result = provider.applyBatch(operations)
		syncResult.stats.numDeletes = result.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts = result.drop(1).sumOf { it.count?.toLong() ?: 0L }
	}

	@WorkerThread
	fun syncFavourites(syncResult: SyncResult) {
		val uri = uri(AUTHORITY_FAVOURITES, TABLE_FAVOURITES)
		val data = JSONObject()
		provider.query(uri, null, null, null, null)?.use { cursor ->
			val jsonArray = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(AUTHORITY_FAVOURITES, jo.getLong("manga_id")))
					jsonArray.put(jo)
				} while (cursor.moveToNext())
			}
			data.put(TABLE_FAVOURITES, jsonArray)
		}
		data.put("timestamp", System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_FAVOURITES")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().parseJson()
		val operations = ArrayList<ContentProviderOperation>()
		val timestamp = response.getLong("timestamp")
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("created_at < ?", arrayOf(timestamp.toString()))
			.build()
		val ja = response.getJSONArray(TABLE_FAVOURITES)
		ja.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}

		val result = provider.applyBatch(operations)
		syncResult.stats.numDeletes = result.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts = result.drop(1).sumOf { it.count?.toLong() ?: 0L }
	}

	@WorkerThread
	fun syncHistory(syncResult: SyncResult) {
		val uri = uri(AUTHORITY_HISTORY, TABLE_HISTORY)
		val data = JSONObject()
		provider.query(uri, null, null, null, null)?.use { cursor ->
			val jsonArray = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(AUTHORITY_HISTORY, jo.getLong("manga_id")))
					jsonArray.put(jo)
				} while (cursor.moveToNext())
			}
			data.put(TABLE_HISTORY, jsonArray)
		}
		data.put("timestamp", System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_HISTORY")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().parseJson()
		val operations = ArrayList<ContentProviderOperation>()
		val timestamp = response.getLong("timestamp")
		operations += ContentProviderOperation.newDelete(uri)
			.withSelection("updated_at < ?", arrayOf(timestamp.toString()))
			.build()
		val ja = response.getJSONArray(TABLE_HISTORY)
		ja.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}

		val result = provider.applyBatch(operations)
		syncResult.stats.numDeletes = result.first().count?.toLong() ?: 0L
		syncResult.stats.numInserts = result.drop(1).sumOf { it.count?.toLong() ?: 0L }
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

	private fun uri(authority: String, table: String) = Uri.parse("content://$authority/$table")
}