package org.koitharu.kotatsu.base.domain

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.room.withTransaction
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.await

private const val MIN_WEBTOON_RATIO = 2

class MangaDataRepository @Inject constructor(
	private val okHttpClient: OkHttpClient,
	private val db: MangaDatabase,
) {

	suspend fun savePreferences(manga: Manga, mode: ReaderMode) {
		val tags = manga.tags.toEntities()
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(manga.toEntity(), tags)
			db.preferencesDao.upsert(
				MangaPrefsEntity(
					mangaId = manga.id,
					mode = mode.id,
				),
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao.find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun findMangaById(mangaId: Long): Manga? {
		return db.mangaDao.find(mangaId)?.toManga()
	}

	suspend fun resolveIntent(intent: MangaIntent): Manga? = when {
		intent.manga != null -> intent.manga
		intent.mangaId != 0L -> findMangaById(intent.mangaId)
		else -> null // TODO resolve uri
	}

	suspend fun storeManga(manga: Manga) {
		val tags = manga.tags.toEntities()
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(manga.toEntity(), tags)
		}
	}

	suspend fun findTags(source: MangaSource): Set<MangaTag> {
		return db.tagsDao.findTags(source.name).toMangaTags()
	}

	/**
	 * Automatic determine type of manga by page size
	 * @return ReaderMode.WEBTOON if page is wide
	 */
	suspend fun determineMangaIsWebtoon(repository: MangaRepository, pages: List<MangaPage>): Boolean {
		val pageIndex = (pages.size * 0.3).roundToInt()
		val page = requireNotNull(pages.getOrNull(pageIndex)) { "No pages" }
		val url = repository.getPageUrl(page)
		val uri = Uri.parse(url)
		val size = if (uri.scheme == "cbz") {
			runInterruptible(Dispatchers.IO) {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				zip.getInputStream(entry).use {
					getBitmapSize(it)
				}
			}
		} else {
			val request = Request.Builder()
				.url(url)
				.get()
				.header(CommonHeaders.REFERER, page.referer)
				.cacheControl(CommonHeaders.CACHE_CONTROL_DISABLED)
				.build()
			okHttpClient.newCall(request).await().use {
				runInterruptible(Dispatchers.IO) {
					getBitmapSize(it.body?.byteStream())
				}
			}
		}
		return size.width * MIN_WEBTOON_RATIO < size.height
	}

	companion object {

		suspend fun getImageMimeType(file: File): String? = runInterruptible(Dispatchers.IO) {
			val options = BitmapFactory.Options().apply {
				inJustDecodeBounds = true
			}
			BitmapFactory.decodeFile(file.path, options)?.recycle()
			options.outMimeType
		}

		private fun getBitmapSize(input: InputStream?): Size {
			val options = BitmapFactory.Options().apply {
				inJustDecodeBounds = true
			}
			BitmapFactory.decodeStream(input, null, options)?.recycle()
			val imageHeight: Int = options.outHeight
			val imageWidth: Int = options.outWidth
			check(imageHeight > 0 && imageWidth > 0)
			return Size(imageWidth, imageHeight)
		}
	}
}
