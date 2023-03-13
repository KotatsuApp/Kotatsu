package org.koitharu.kotatsu.base.domain

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaPrefsEntity
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.reader.domain.ReaderColorFilter
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val MIN_WEBTOON_RATIO = 2

@Singleton
class MangaDataRepository @Inject constructor(
	private val okHttpClient: OkHttpClient,
	private val db: MangaDatabase,
) {

	suspend fun saveReaderMode(manga: Manga, mode: ReaderMode) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.preferencesDao.find(manga.id) ?: newEntity(manga.id)
			db.preferencesDao.upsert(entity.copy(mode = mode.id))
		}
	}

	suspend fun saveColorFilter(manga: Manga, colorFilter: ReaderColorFilter?) {
		db.withTransaction {
			storeManga(manga)
			val entity = db.preferencesDao.find(manga.id) ?: newEntity(manga.id)
			db.preferencesDao.upsert(
				entity.copy(
					cfBrightness = colorFilter?.brightness ?: 0f,
					cfContrast = colorFilter?.contrast ?: 0f,
				),
			)
		}
	}

	suspend fun getReaderMode(mangaId: Long): ReaderMode? {
		return db.preferencesDao.find(mangaId)?.let { ReaderMode.valueOf(it.mode) }
	}

	suspend fun getColorFilter(mangaId: Long): ReaderColorFilter? {
		return db.preferencesDao.find(mangaId)?.getColorFilterOrNull()
	}

	fun observeColorFilter(mangaId: Long): Flow<ReaderColorFilter?> {
		return db.preferencesDao.observe(mangaId)
			.map { it?.getColorFilterOrNull() }
			.distinctUntilChanged()
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

	private fun MangaPrefsEntity.getColorFilterOrNull(): ReaderColorFilter? {
		return if (cfBrightness != 0f || cfContrast != 0f) {
			ReaderColorFilter(cfBrightness, cfContrast)
		} else {
			null
		}
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
				.tag(MangaSource::class.java, page.source)
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

	private fun newEntity(mangaId: Long) = MangaPrefsEntity(
		mangaId = mangaId,
		mode = -1,
		cfBrightness = 0f,
		cfContrast = 0f,
	)

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
