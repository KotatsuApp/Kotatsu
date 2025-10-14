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
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITES
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES
import org.koitharu.kotatsu.core.db.TABLE_HISTORY
import org.koitharu.kotatsu.core.db.TABLE_MANGA
import org.koitharu.kotatsu.core.db.TABLE_MANGA_TAGS
import org.koitharu.kotatsu.core.db.TABLE_TAGS
import org.koitharu.kotatsu.core.network.BaseHttpClient
import org.koitharu.kotatsu.core.util.ext.buildContentValues
import org.koitharu.kotatsu.core.util.ext.map
import org.koitharu.kotatsu.core.util.ext.mapToSet
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.sync.data.SyncAuthApi
import org.koitharu.kotatsu.sync.data.SyncAuthenticator
import org.koitharu.kotatsu.sync.data.SyncInterceptor
import org.koitharu.kotatsu.sync.data.SyncSettings
import org.koitharu.kotatsu.sync.data.model.FavouriteCategorySyncDto
import org.koitharu.kotatsu.sync.data.model.FavouriteSyncDto
import org.koitharu.kotatsu.sync.data.model.HistorySyncDto
import org.koitharu.kotatsu.sync.data.model.MangaSyncDto
import org.koitharu.kotatsu.sync.data.model.MangaTagSyncDto
import org.koitharu.kotatsu.sync.data.model.SyncDto
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class SyncHelper @AssistedInject constructor(
	@ApplicationContext context: Context,
	@BaseHttpClient baseHttpClient: OkHttpClient,
	@Assisted private val account: Account,
	@Assisted private val provider: ContentProviderClient,
	private val settings: SyncSettings,
) {

	private val authorityHistory = context.getString(R.string.sync_authority_history)
	private val authorityFavourites = context.getString(R.string.sync_authority_favourites)
	private val mediaTypeJson = "application/json".toMediaType()
	private val httpClient = baseHttpClient.newBuilder()
		.authenticator(SyncAuthenticator(context, account, settings, SyncAuthApi(OkHttpClient())))
		.addInterceptor(SyncInterceptor(context, account))
		.build()
	private val baseUrl: String by lazy {
		settings.syncUrl
	}
	private val defaultGcPeriod: Long // gc period if sync enabled
		get() = TimeUnit.DAYS.toMillis(4)

	@WorkerThread
	fun syncFavourites(stats: SyncStats) {
		val payload = Json.encodeToString(
			SyncDto(
				history = null,
				favourites = getFavourites(),
				categories = getFavouriteCategories(),
				timestamp = System.currentTimeMillis(),
			),
		)
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_FAVOURITES")
			.post(payload.toRequestBody(mediaTypeJson))
			.build()
		val response = httpClient.newCall(request).execute().parseDtoOrNull()
		response?.categories?.let { categories ->
			val categoriesResult = upsertFavouriteCategories(categories)
			stats.numDeletes += categoriesResult.firstOrNull()?.count?.toLong() ?: 0L
			stats.numInserts += categoriesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
		}
		response?.favourites?.let { favourites ->
			val favouritesResult = upsertFavourites(favourites)
			stats.numDeletes += favouritesResult.firstOrNull()?.count?.toLong() ?: 0L
			stats.numInserts += favouritesResult.drop(1).sumOf { it.count?.toLong() ?: 0L }
			stats.numEntries += stats.numInserts + stats.numDeletes
		}
		gcFavourites()
	}

	@Blocking
	@WorkerThread
	fun syncHistory(stats: SyncStats) {
		val payload = Json.encodeToString(
			SyncDto(
				history = getHistory(),
				favourites = null,
				categories = null,
				timestamp = System.currentTimeMillis(),
			),
		)
		val request = Request.Builder()
			.url("$baseUrl/resource/$TABLE_HISTORY")
			.post(payload.toRequestBody(mediaTypeJson))
			.build()
		val response = httpClient.newCall(request).execute().parseDtoOrNull()
		response?.history?.let { history ->
			val result = upsertHistory(history)
			stats.numDeletes += result.firstOrNull()?.count?.toLong() ?: 0L
			stats.numInserts += result.drop(1).sumOf { it.count?.toLong() ?: 0L }
			stats.numEntries += stats.numInserts + stats.numDeletes
		}
		gcHistory()
	}

	fun onError(e: Throwable) {
		e.printStackTraceDebug()
	}

	fun onSyncComplete(result: SyncResult) {
		if (BuildConfig.DEBUG) {
			Log.i("Sync", "Sync finished: ${result.toDebugString()}")
		}
	}

	private fun upsertHistory(history: List<HistorySyncDto>): Array<ContentProviderResult> {
		val uri = uri(authorityHistory, TABLE_HISTORY)
		val operations = ArrayList<ContentProviderOperation>()
		history.mapTo(operations) {
			operations.addAll(upsertManga(it.manga, authorityHistory))
			ContentProviderOperation.newInsert(uri)
				.withValues(it.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavouriteCategories(categories: List<FavouriteCategorySyncDto>): Array<ContentProviderResult> {
		val uri = uri(authorityFavourites, TABLE_FAVOURITE_CATEGORIES)
		val operations = ArrayList<ContentProviderOperation>()
		categories.mapTo(operations) {
			ContentProviderOperation.newInsert(uri)
				.withValues(it.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertFavourites(favourites: List<FavouriteSyncDto>): Array<ContentProviderResult> {
		val uri = uri(authorityFavourites, TABLE_FAVOURITES)
		val operations = ArrayList<ContentProviderOperation>()
		favourites.mapTo(operations) {
			operations.addAll(upsertManga(it.manga, authorityFavourites))
			ContentProviderOperation.newInsert(uri)
				.withValues(it.toContentValues())
				.build()
		}
		return provider.applyBatch(operations)
	}

	private fun upsertManga(manga: MangaSyncDto, authority: String): List<ContentProviderOperation> {
		val tags = manga.tags
		val result = ArrayList<ContentProviderOperation>(tags.size * 2 + 1)
		for (tag in tags) {
			result += ContentProviderOperation.newInsert(uri(authority, TABLE_TAGS))
				.withValues(tag.toContentValues())
				.build()
			result += ContentProviderOperation.newInsert(uri(authority, TABLE_MANGA_TAGS))
				.withValues(
					buildContentValues(2) {
						put("manga_id", manga.id)
						put("tag_id", tag.id)
					},
				).build()
		}
		result.add(
			0,
			ContentProviderOperation.newInsert(uri(authority, TABLE_MANGA))
				.withValues(manga.toContentValues())
				.build(),
		)
		return result
	}

	private fun getHistory(): List<HistorySyncDto> {
		return provider.query(authorityHistory, TABLE_HISTORY).use { cursor ->
			val result = ArrayList<HistorySyncDto>(cursor.count)
			if (cursor.moveToFirst()) {
				do {
					val mangaId = cursor.getLong(cursor.getColumnIndexOrThrow("manga_id"))
					result.add(HistorySyncDto(cursor, getManga(authorityHistory, mangaId)))
				} while (cursor.moveToNext())
			}
			result
		}
	}

	private fun getFavourites(): List<FavouriteSyncDto> {
		return provider.query(authorityFavourites, TABLE_FAVOURITES).map { cursor ->
			val manga = getManga(authorityFavourites, cursor.getLong(cursor.getColumnIndexOrThrow("manga_id")))
			FavouriteSyncDto(cursor, manga)
		}
	}

	private fun getFavouriteCategories(): List<FavouriteCategorySyncDto> =
		provider.query(authorityFavourites, TABLE_FAVOURITE_CATEGORIES).map { cursor ->
			FavouriteCategorySyncDto(cursor)
		}

	private fun getManga(authority: String, id: Long): MangaSyncDto {
		val tags = requireNotNull(
			provider.query(
				uri(authority, TABLE_MANGA_TAGS),
				arrayOf("tag_id"),
				"manga_id = ?",
				arrayOf(id.toString()),
				null,
			)?.mapToSet {
				val tagId = it.getLong(it.getColumnIndexOrThrow("tag_id"))
				getTag(authority, tagId)
			},
		)
		return requireNotNull(
			provider.query(
				uri(authority, TABLE_MANGA),
				null,
				"manga_id = ?",
				arrayOf(id.toString()),
				null,
			)?.use { cursor ->
				cursor.moveToFirst()
				MangaSyncDto(cursor, tags)
			},
		)
	}

	private fun getTag(authority: String, tagId: Long): MangaTagSyncDto = requireNotNull(
		provider.query(
			uri(authority, TABLE_TAGS),
			null,
			"tag_id = ?",
			arrayOf(tagId.toString()),
			null,
		)?.use { cursor ->
			if (cursor.moveToFirst()) {
				MangaTagSyncDto(cursor)
			} else {
				null
			}
		},
	)

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

	private fun uri(authority: String, table: String) = "content://$authority/$table".toUri()

	private fun Response.parseDtoOrNull(): SyncDto? = use {
		when {
			!isSuccessful -> throw IOException(body.string())
			code == HttpURLConnection.HTTP_NO_CONTENT -> null
			else -> Json.decodeFromString<SyncDto>(body.string())
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
