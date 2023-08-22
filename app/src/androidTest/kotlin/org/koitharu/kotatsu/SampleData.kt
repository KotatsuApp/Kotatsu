package org.koitharu.kotatsu

import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.*
import kotlin.reflect.KClass

object SampleData {

	private val moshi = Moshi.Builder()
		.add(DateAdapter())
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
}