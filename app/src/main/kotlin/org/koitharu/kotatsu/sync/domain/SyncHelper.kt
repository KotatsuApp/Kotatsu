package org.koitharu.kotatsu.sync.domain

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.Context
import android.content.OperationApplicationException
import android.content.SyncResult
import android.content.SyncStats
import android.database.Cursor
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.contentValuesOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.core.db.TABLE_MANGA
import org.koitharu.kotatsu.core.db.TABLE_MANGA_TAGS
import org.koitharu.kotatsu.core.db.TABLE_TAGS
import org.koitharu.kotatsu.core.logs.FileLogger
import org.koitharu.kotatsu.core.logs.SyncLogger
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.util.ext.parseJsonOrNull
import org.koitharu.kotatsu.core.util.ext.toContentValues
import org.koitharu.kotatsu.core.util.ext.toJson
import org.koitharu.kotatsu.core.util.ext.toRequestBody
import org.koitharu.kotatsu.parsers.util.json.mapJSONTo
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.data.SyncAuthenticator
import org.koitharu.kotatsu.sync.data.SyncInterceptor
import org.koitharu.kotatsu.sync.data.SyncSettings
import java.util.concurrent.TimeUnit

private const val FIELD_TIMESTAMP = "timestamp"

class SyncHelper @AssistedInject constructor(
	@ApplicationContext context: Context,
	@BaseHttpClient baseHttpClient: OkHttpClient,
	@Assisted private val account: Account,
	@Assisted private val provider: ContentProviderClient,
	private val settings: SyncSettings,
	@SyncLogger private val logger: FileLogger,
) {

	private val authorityHistory = context.getString(R.string.sync_authority_history)
	private val authorityFavourites = context.getString(R.string.sync_authority_favourites)
	private val httpClient = baseHttpClient.newBuilder()
		.authenticator(SyncAuthenticator(context, account, settings, SyncAuthApi(OkHttpClient())))
		.addInterceptor(SyncInterceptor(context, account))
		.build()
	private val baseUrl: String by lazy {
		settings.syncURL
	}
	private val defaultGcPeriod: Long // gc period if sync enabled
		get() = TimeUnit.DAYS.toMillis(4)

	@WorkerThread
	fun syncFavourites(stats: SyncStats) {
		val data = JSONObject()
		data.put(TABLE_FAVOURITE_CATEGORIES, getFavouriteCategories())
		data.put(TABLE_FAVOURITES, getFavourites())
		data.put(FIELD_TIMESTAMP, System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_FAVOURITES")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().log().parseJsonOrNull()
		if (response != null) {
			val categoriesResult = upsertFavouriteCategories(response.getJSONArray(TABLE_FAVOURITE_CATEGORIES))
			stats.numDeletes += categoriesResult.first().count?.toLong() ?: 0L
			stats.numInserts += categoriesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
			val favouritesResult = upsertFavourites(response.getJSONArray(TABLE_FAVOURITES))
			stats.numDeletes += favouritesResult.first().count?.toLong() ?: 0L
			stats.numInserts += favouritesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
			stats.numEntries += stats.numInserts + stats.numDeletes
		}
		gcFavourites()
	}

	@WorkerThread
	fun syncHistory(stats: SyncStats) {
		val data = JSONObject()
		data.put(TABLE_HISTORY, getHistory())
		data.put(FIELD_TIMESTAMP, System.currentTimeMillis())
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_HISTORY")
			.post(data.toRequestBody())
			.build()
		val response = httpClient.newCall(request).execute().log().parseJsonOrNull()
		if (response != null) {
			val result = upsertHistory(
				json = response.getJSONArray(TABLE_HISTORY),
			)
			stats.numDeletes += result.first().count?.toLong() ?: 0L
			stats.numInserts += result.drop(1).sumOf { it.count?.toLong() ?: 0L }
			stats.numEntries += stats.numInserts + stats.numDeletes
		}
		gcHistory()
	}

	fun onError(e: Throwable) {
		if (logger.isEnabled) {
			logger.log("Sync error", e)
		}
	}

	fun onSyncComplete(result: SyncResult) {
		if (logger.isEnabled) {
			logger.log("Sync finished: ${result.toDebugString()}")
			logger.flushBlocking()
		}
	}

	private fun upsertHistory(json: JSONArray): Array<ContentProviderResult> {
		val uri = uri(authorityHistory, TABLE_HISTORY)
		val operations = ArrayList<ContentProviderOperation>()
		json.mapJSONTo(operations) { jo ->
			operations.addAll(upsertManga(jo.removeJSONObject("manga"), authorityHistory))
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavouriteCategories(json: JSONArray): Array<ContentProviderResult> {
		val uri = uri(authorityFavourites, TABLE_FAVOURITE_CATEGORIES)
		val operations = ArrayList<ContentProviderOperation>()
		json.mapJSONTo(operations) { jo ->
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavourites(json: JSONArray): Array<ContentProviderResult> {
		val uri = uri(authorityFavourites, TABLE_FAVOURITES)
		val operations = ArrayList<ContentProviderOperation>()
		json.mapJSONTo(operations) { jo ->
			operations.addAll(upsertManga(jo.removeJSONObject("manga"), authorityFavourites))
			ContentProviderOperation.newInsert(uri)
				.withValues(jo.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertManga(json: JSONObject, authority: String): List<ContentProviderOperation> {
		val tags = json.removeJSONArray(TABLE_TAGS)
		val result = ArrayList<ContentProviderOperation>(tags.length() * 2 + 1)
		for (i in 0 until tags.length()) {
			val tag = tags.getJSONObject(i)
			result += ContentProviderOperation.newInsert(uri(authority, TABLE_TAGS))
				.withValues(tag.toContentValues())
				.build()
			result += ContentProviderOperation.newInsert(uri(authority, TABLE_MANGA_TAGS))
				.withValues(
					contentValuesOf(
						"manga_id" to json.getLong("manga_id"),
						"tag_id" to tag.getLong("tag_id"),
					),
				).build()
		}
		result.add(
			0,
			ContentProviderOperation.newInsert(uri(authority, TABLE_MANGA))
				.withValues(json.toContentValues())
				.build(),
		)
		return result
	}

	private fun getHistory(): JSONArray {
		return provider.query(authorityHistory, TABLE_HISTORY).use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(authorityHistory, jo.getLong("manga_id")))
					json.put(jo)
				} while (cursor.moveToNext())
			}
			json
		}
	}

	private fun getFavourites(): JSONArray {
		return provider.query(authorityFavourites, TABLE_FAVOURITES).use { cursor ->
			val json = JSONArray()
			if (cursor.moveToFirst()) {
				do {
					val jo = cursor.toJson()
					jo.put("manga", getManga(authorityFavourites, jo.getLong("manga_id")))
					json.put(jo)
				} while (cursor.moveToNext())
			}
			json
		}
	}

	private fun getFavouriteCategories(): JSONArray {
		return provider.query(authorityFavourites, TABLE_FAVOURITE_CATEGORIES).use { cursor ->
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

	private fun gcFavourites() {
		val deletedAt = System.currentTimeMillis() - defaultGcPeriod
		val selection = "deleted_at != 0 AND deleted_at < ?"
		val args = arrayOf(deletedAt.toString())
		provider.delete(uri(authorityFavourites, TABLE_FAVOURITES), selection, args)
		provider.delete(uri(authorityFavourites, TABLE_FAVOURITE_CATEGORIES), selection, args)
	}

	private fun gcHistory() {
		val deletedAt = System.currentTimeMillis() - defaultGcPeriod
		val selection = "deleted_at != 0 AND deleted_at < ?"
		val args = arrayOf(deletedAt.toString())
		provider.delete(uri(authorityHistory, TABLE_HISTORY), selection, args)
	}

	private fun ContentProviderClient.query(authority: String, table: String): Cursor {
		val uri = uri(authority, table)
		return query(uri, null, null, null, null)
			?: throw OperationApplicationException("Query failed: $uri")
	}

	private fun uri(authority: String, table: String) = Uri.parse("content://$authority/$table")

	private fun JSONObject.removeJSONObject(name: String) = remove(name) as JSONObject

	private fun JSONObject.removeJSONArray(name: String) = remove(name) as JSONArray

	private fun Response.log() = apply {
		if (logger.isEnabled) {
			logger.log("$code ${request.url}")
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(
			account: Account,
			contentProviderClient: ContentProviderClient,
		): SyncHelper
	}
}
