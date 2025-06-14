package org.koitharu.kotatsu

import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.time.Instant
import java.util.Date
import kotlin.reflect.KClass

object SampleData {

	private val moshi = Moshi.Builder()
		.add(DateAdapter())
		.add(InstantAdapter())
		.add(MangaSourceAdapter())
		.add(KotlinJsonAdapterFactory())
		.build()

	val manga: Manga = loadAsset("manga/header.json", Manga::class)

	val mangaDetails: Manga = loadAsset("manga/full.json", Manga::class)

	val tag = mangaDetails.tags.elementAt(2)

	val chapter = checkNotNull(mangaDetails.chapters)[2]

	val favouriteCategory: FavouriteCategory = loadAsset("categories/simple.json", FavouriteCategory::class)

	fun <T : Any> loadAsset(name: String, cls: KClass<T>): T {
		val assets = InstrumentationRegistry.getInstrumentation().context.assets
		return assets.open(name).use {
			moshi.adapter(cls.java).fromJson(it.source().buffer())
		} ?: throw RuntimeException("Cannot read asset from json \"$name\"")
	}

	private class DateAdapter : JsonAdapter<Date>() {

		@FromJson
		override fun fromJson(reader: JsonReader): Date? {
			val ms = reader.nextLong()
			return if (ms == 0L) {
				null
			} else {
				Date(ms)
			}
		}

		@ToJson
		override fun toJson(writer: JsonWriter, value: Date?) {
			writer.value(value?.time ?: 0L)
		}
	}

	private class MangaSourceAdapter : JsonAdapter<MangaSource>() {

		@FromJson
		override fun fromJson(reader: JsonReader): MangaSource? {
			val name = reader.nextString() ?: return null
			return MangaSource(name)
		}

		@ToJson
		override fun toJson(writer: JsonWriter, value: MangaSource?) {
			writer.value(value?.name)
		}
	}

	private class InstantAdapter : JsonAdapter<Instant>() {

		@FromJson
		override fun fromJson(reader: JsonReader): Instant? {
			val ms = reader.nextLong()
			return if (ms == 0L) {
				null
			} else {
				Instant.ofEpochMilli(ms)
			}
		}

		@ToJson
		override fun toJson(writer: JsonWriter, value: Instant?) {
			writer.value(value?.toEpochMilli() ?: 0L)
		}
	}
}
