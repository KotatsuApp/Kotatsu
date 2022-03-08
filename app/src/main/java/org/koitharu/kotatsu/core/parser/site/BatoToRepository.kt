package org.koitharu.kotatsu.core.parser.site

import android.util.Base64
import androidx.collection.ArraySet
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val PAGE_SIZE = 60
private const val PAGE_SIZE_SEARCH = 20

class BatoToRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.BATOTO

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.NEWEST,
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.ALPHABETICAL
	)

	override val defaultDomain: String = "bato.to"

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		if (!query.isNullOrEmpty()) {
			return search(offset, query)
		}
		val page = (offset / PAGE_SIZE) + 1

		@Suppress("NON_EXHAUSTIVE_WHEN_STATEMENT")
		val url = buildString {
			append("https://")
			append(getDomain())
			append("/browse?sort=")
			when (sortOrder) {
				null,
				SortOrder.UPDATED -> append("update.za")
				SortOrder.POPULARITY -> append("views_a.za")
				SortOrder.NEWEST -> append("create.za")
				SortOrder.ALPHABETICAL -> append("title.az")
			}
			if (!tags.isNullOrEmpty()) {
				append("&genres=")
				appendAll(tags, ",") { it.key }
			}
			append("&page=")
			append(page)
		}
		return parseList(url, page)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val root = loaderContext.httpGet(manga.url.withDomain()).parseHtml()
			.getElementById("mainer") ?: parseFailed("Cannot find root")
		val details = root.selectFirst(".detail-set") ?: parseFailed("Cannot find detail-set")
		val attrs = details.selectFirst(".attr-main")?.select(".attr-item")?.associate {
			it.child(0).text().trim() to it.child(1)
		}.orEmpty()
		return manga.copy(
			title = root.selectFirst("h3.item-title")?.text() ?: manga.title,
			isNsfw = !root.selectFirst("alert")?.getElementsContainingOwnText("NSFW").isNullOrEmpty(),
			largeCoverUrl = details.selectFirst("img[src]")?.absUrl("src"),
			description = details.getElementById("limit-height-body-summary")
				?.selectFirst(".limit-html")
				?.html(),
			tags = manga.tags + attrs["Genres:"]?.parseTags().orEmpty(),
			state = when (attrs["Release status:"]?.text()) {
				"Ongoing" -> MangaState.ONGOING
				"Completed" -> MangaState.FINISHED
				else -> manga.state
			},
			author = attrs["Authors:"]?.text()?.trim() ?: manga.author,
			chapters = root.selectFirst(".episode-list")
				?.selectFirst(".main")
				?.children()
				?.reversed()
				?.mapIndexedNotNull { i, div ->
					div.parseChapter(i)
				}.orEmpty()
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val scripts = loaderContext.httpGet(fullUrl).parseHtml().select("script")
		for (script in scripts) {
			val scriptSrc = script.html()
			val p = scriptSrc.indexOf("const images =")
			if (p == -1) continue
			val start = scriptSrc.indexOf('[', p)
			val end = scriptSrc.indexOf(';', start)
			if (start == -1 || end == -1) {
				continue
			}
			val images = JSONArray(scriptSrc.substring(start, end))
			val batoJs = scriptSrc.substringBetweenFirst("batojs =", ";")?.trim(' ', '"', '\n')
				?: parseFailed("Cannot find batojs")
			val server = scriptSrc.substringBetweenFirst("server =", ";")?.trim(' ', '"', '\n')
				?: parseFailed("Cannot find server")
			val password = loaderContext.evaluateJs(batoJs)?.removeSurrounding('"')
				?: parseFailed("Cannot evaluate batojs")
			val serverDecrypted = decryptAES(server, password).removeSurrounding('"')
			val result = ArrayList<MangaPage>(images.length())
			repeat(images.length()) { i ->
				val url = images.getString(i)
				result += MangaPage(
					id = generateUid(url),
					url = if (url.startsWith("http")) url else "$serverDecrypted$url",
					referer = fullUrl,
					preview = null,
					source = source,
				)
			}
			return result
		}
		parseFailed("Cannot find images list")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val scripts = loaderContext.httpGet(
			"https://${getDomain()}/browse"
		).parseHtml().select("script")
		for (script in scripts) {
			val genres = script.html().substringBetweenFirst("const _genres =", ";") ?: continue
			val jo = JSONObject(genres)
			val result = ArraySet<MangaTag>(jo.length())
			jo.keys().forEach { key ->
				val item = jo.getJSONObject(key)
				result += MangaTag(
					title = item.getString("text").toTitleCase(),
					key = item.getString("file"),
					source = source,
				)
			}
			return result
		}
		parseFailed("Cannot find gernes list")
	}

	override fun getFaviconUrl(): String = "https://styles.amarkcdn.com/img/batoto/favicon.ico?v0"

	private suspend fun search(offset: Int, query: String): List<Manga> {
		val page = (offset / PAGE_SIZE_SEARCH) + 1
		val url = buildString {
			append("https://")
			append(getDomain())
			append("/search?word=")
			append(query.replace(' ', '+'))
			append("&page=")
			append(page)
		}
		return parseList(url, page)
	}

	private fun getActivePage(body: Element): Int = body.select("nav ul.pagination > li.page-item.active")
		.lastOrNull()
		?.text()
		?.toIntOrNull() ?: parseFailed("Cannot determine current page")

	private suspend fun parseList(url: String, page: Int): List<Manga> {
		val body = loaderContext.httpGet(url).parseHtml().body()
		if (body.selectFirst(".browse-no-matches") != null) {
			return emptyList()
		}
		val activePage = getActivePage(body)
		if (activePage != page) {
			return emptyList()
		}
		val root = body.getElementById("series-list") ?: parseFailed("Cannot find root")
		return root.children().map { div ->
			val a = div.selectFirst("a") ?: parseFailed()
			val href = a.relUrl("href")
			val title = div.selectFirst(".item-title")?.text() ?: parseFailed("Title not found")
			Manga(
				id = generateUid(href),
				title = title,
				altTitle = div.selectFirst(".item-alias")?.text()?.takeUnless { it == title },
				url = href,
				publicUrl = a.absUrl("href"),
				rating = Manga.NO_RATING,
				isNsfw = false,
				coverUrl = div.selectFirst("img[src]")?.absUrl("src").orEmpty(),
				largeCoverUrl = null,
				description = null,
				tags = div.selectFirst(".item-genre")?.parseTags().orEmpty(),
				state = null,
				author = null,
				source = source,
			)
		}
	}

	private fun Element.parseTags() = children().mapToSet { span ->
		val text = span.ownText()
		MangaTag(
			title = text.toTitleCase(),
			key = text.lowercase(Locale.ENGLISH).replace(' ', '_'),
			source = source,
		)
	}

	private fun Element.parseChapter(index: Int): MangaChapter? {
		val a = selectFirst("a.chapt") ?: return null
		val extra = selectFirst(".extra")
		val href = a.relUrl("href")
		return MangaChapter(
			id = generateUid(href),
			name = a.text(),
			number = index + 1,
			url = href,
			scanlator = extra?.getElementsByAttributeValueContaining("href", "/group/")?.text(),
			uploadDate = runCatching {
				parseChapterDate(extra?.select("i")?.lastOrNull()?.ownText())
			}.getOrDefault(0),
			branch = null,
			source = source,
		)
	}

	private fun parseChapterDate(date: String?): Long {
		if (date.isNullOrEmpty()) {
			return 0
		}
		val value = date.substringBefore(' ').toInt()
		val field = when {
			"sec" in date -> Calendar.SECOND
			"min" in date -> Calendar.MINUTE
			"hour" in date -> Calendar.HOUR
			"day" in date -> Calendar.DAY_OF_MONTH
			"week" in date -> Calendar.WEEK_OF_YEAR
			"month" in date -> Calendar.MONTH
			"year" in date -> Calendar.YEAR
			else -> return 0
		}
		val calendar = Calendar.getInstance()
		calendar.add(field, -value)
		return calendar.timeInMillis
	}

	private fun decryptAES(encrypted: String, password: String): String {
		val cipherData = Base64.decode(encrypted, Base64.DEFAULT)
		val saltData = cipherData.copyOfRange(8, 16)
		val (key, iv) = generateKeyAndIV(
			keyLength = 32,
			ivLength = 16,
			iterations = 1,
			salt = saltData,
			password = password.toByteArray(StandardCharsets.UTF_8),
			md = MessageDigest.getInstance("MD5"),
		)
		val encryptedData = cipherData.copyOfRange(16, cipherData.size)
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, key, iv)
		return cipher.doFinal(encryptedData).toString(Charsets.UTF_8)
	}

	@Suppress("SameParameterValue")
	private fun generateKeyAndIV(
		keyLength: Int,
		ivLength: Int,
		iterations: Int,
		salt: ByteArray,
		password: ByteArray,
		md: MessageDigest,
	): Pair<SecretKeySpec, IvParameterSpec> {
		val digestLength = md.digestLength
		val requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength
		val generatedData = ByteArray(requiredLength)
		var generatedLength = 0
		md.reset()
		while (generatedLength < keyLength + ivLength) {
			if (generatedLength > 0) {
				md.update(generatedData, generatedLength - digestLength, digestLength)
			}
			md.update(password)
			md.update(salt, 0, 8)
			md.digest(generatedData, generatedLength, digestLength)
			repeat(iterations - 1) {
				md.update(generatedData, generatedLength, digestLength)
				md.digest(generatedData, generatedLength, digestLength)
			}
			generatedLength += digestLength
		}

		return SecretKeySpec(generatedData.copyOfRange(0, keyLength), "AES") to IvParameterSpec(
			if (ivLength > 0) {
				generatedData.copyOfRange(keyLength, keyLength + ivLength)
			} else byteArrayOf()
		)
	}
}