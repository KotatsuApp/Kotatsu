package org.koitharu.kotatsu.domain.local

import androidx.annotation.WorkerThread
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import org.koitharu.kotatsu.utils.ext.toFileName
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@WorkerThread
class MangaZip(private val file: File) {

	private val dir = file.parentFile?.sub(file.name + ".dir")?.takeIf { it.mkdir() }
		?: throw RuntimeException("Cannot create temporary directory")

	private lateinit var index: JSONObject

	fun prepare(manga: Manga) {
		extract()
		index = dir.sub("index.json").takeIfReadable()?.readText()?.let { JSONObject(it) } ?: JSONObject()

		index.put("id", manga.id)
		index.put("title", manga.title)
		index.put("title_alt", manga.altTitle)
		index.put("url", manga.url)
		index.put("cover", manga.coverUrl)
		index.put("description", manga.description)
		index.put("rating", manga.rating)
		index.put("source", manga.source.name)
		index.put("cover_large", manga.largeCoverUrl)
		index.put("tags", JSONArray().also { a ->
			for (tag in manga.tags) {
				val jo = JSONObject()
				jo.put("key", tag.key)
				jo.put("title", tag.title)
				a.put(jo)
			}
		})
		index.put("chapters", JSONObject())
		index.put("app_id", BuildConfig.APPLICATION_ID)
		index.put("app_version", BuildConfig.VERSION_CODE)
	}

	fun cleanup() {
		dir.deleteRecursively()
	}

	fun compress() {
		dir.sub("index.json").writeText(index.toString(4))
		ZipOutputStream(file.outputStream()).use { out ->
			for (file in dir.listFiles().orEmpty()) {
				val entry = ZipEntry(file.name)
				out.putNextEntry(entry)
				file.inputStream().use { stream ->
					stream.copyTo(out)
				}
				out.closeEntry()
			}
		}
	}

	private fun extract() {
		if (!file.exists()) {
			return
		}
		ZipInputStream(file.inputStream()).use { input ->
			while(true) {
				val entry = input.nextEntry ?: return
				if (!entry.isDirectory) {
					dir.sub(entry.name).outputStream().use { out->
						input.copyTo(out)
					}
				}
				input.closeEntry()
			}
		}
	}

	fun addCover(file: File) {
		val name = FILENAME_PATTERN.format(0, 0)
		file.copyTo(dir.sub(name), overwrite = true)
	}

	fun addPage(page: MangaPage, chapter: MangaChapter, file: File, pageNumber: Int) {
		val name = FILENAME_PATTERN.format(chapter.number, pageNumber)
		file.copyTo(dir.sub(name), overwrite = true)
		val chapters = index.getJSONObject("chapters")
		if (!chapters.has(chapter.number.toString())) {
			val jo = JSONObject()
			jo.put("id", chapter.id)
			jo.put("url", chapter.url)
			jo.put("name", chapter.name)
			chapters.put(chapter.number.toString(), jo)
		}
	}

	companion object {

		private const val FILENAME_PATTERN = "%03d%03d"

		fun findInDir(root: File, manga: Manga): MangaZip {
			val name = manga.title.toFileName() + ".cbz"
			val file = File(root, name)
			return MangaZip(file)
		}
	}
}