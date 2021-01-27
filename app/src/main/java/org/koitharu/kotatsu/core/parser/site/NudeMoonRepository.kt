package org.koitharu.kotatsu.core.parser.site

/*
class NudeMoonRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.NUDEMOON

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
		SortOrder.RATING
	)

	init {
		loaderContext.insertCookies(
			conf.getDomain(DEFAULT_DOMAIN),
			"NMfYa=1;",
			"nm_mobile=0;"
		)
	}

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val domain = conf.getDomain(DEFAULT_DOMAIN)
		val url = when {
			!query.isNullOrEmpty() -> "https://$domain//search?stext=${query.urlEncoded()}&rowstart=$offset"
			tag != null -> "https://$domain/tags/${tag.key}&rowstart=$offset"
			else -> "https://$domain/all_manga?${getSortKey(sortOrder)}&rowstart=$offset"
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().run {
			selectFirst("td.shoutbox") ?: selectFirst("td.main-bg")
		} ?: throw ParseException("Cannot find root")
		return root.select("table.news_pic2").mapNotNull { row ->
			val a = row.selectFirst("td.bg_style1")?.selectFirst("a")
				?: return@mapNotNull null
			val href = a.absUrl("href")
			val title = a.selectFirst("h2")?.text().orEmpty()
			val info = row.selectFirst("div.tbl2") ?: return@mapNotNull null
			Manga(
				id = href.longHashCode(),
				url = href,
				title = title.substringAfter(" / "),
				altTitle = title.substringBefore(" / ", "")
					.takeUnless { it.isBlank() },
				author = info.getElementsContainingOwnText("Автор:")?.firstOrNull()
					?.nextElementSibling()?.ownText(),
				coverUrl = row.selectFirst("img.news_pic2")?.absUrl("src")
					.orEmpty(),
				tags = row.selectFirst("span.tag-links")?.select("a")
					?.mapToSet {
						MangaTag(
							title = it.text(),
							key = it.attr("href").substringAfterLast('/').urlEncoded(),
							source = source
						)
					}.orEmpty(),
				source = source
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.url).parseHtml()
		val root = doc.body().selectFirst("table.shoutbox")
			?: throw ParseException("Cannot find root")
		val info = root.select("div.tbl2")
		return manga.copy(
			description = info.select("div.blockquote").lastOrNull()?.html(),
			tags = info.select("span.tag-links").firstOrNull()?.select("a")?.mapToSet {
				MangaTag(
					title = it.text(),
					key = it.attr("href").substringAfterLast('/').urlEncoded(),
					source = source
				)
			} ?: manga.tags,
			chapters = listOf(
				MangaChapter(
					id = manga.id,
					url = manga.url,
					source = source,
					number = 1,
					name = manga.title
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		conf.getDomain(DEFAULT_DOMAIN)
		val doc = loaderContext.httpGet(chapter.url).parseHtml()
		val root = doc.body().selectFirst("td.main-body")
			?: throw ParseException("Cannot find root")
		return root.getElementsByAttributeValueMatching("href", pageUrlPatter).mapNotNull { a ->
			val url = a.absUrl("href")
			MangaPage(
				id = url.longHashCode(),
				url = url,
				referer = chapter.url,
				preview = a.selectFirst("img")?.absUrl("src"),
				source = source
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val doc = loaderContext.httpGet(page.url).parseHtml()
		return doc.body().getElementById("gallery").attr("src").inContextOf(doc)
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = conf.getDomain(DEFAULT_DOMAIN)
		val doc = loaderContext.httpGet("https://$domain/all_manga").parseHtml()
		val root = doc.body().getElementsContainingOwnText("Поиск манги по тегам")
			.firstOrNull()?.parents()?.find { it.tag().normalName() == "tbody" }
			?.selectFirst("td.textbox")?.selectFirst("td.small")
			?: throw ParseException("Tags root not found")
		return root.select("a").mapToSet {
			MangaTag(
				title = it.text(),
				key = it.attr("href").substringAfterLast('/')
					.removeSuffix("+").urlEncoded(),
				source = source
			)
		}
	}

	override fun onCreatePreferences() = arraySetOf(SourceSettings.KEY_DOMAIN)

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.POPULARITY -> "views"
			SortOrder.NEWEST -> "date"
			SortOrder.RATING -> "like"
			else -> "like"
		}

	private companion object {

		private const val DEFAULT_DOMAIN = "nude-moon.me"
		private val pageUrlPatter = Pattern.compile(".*\\?page=[0-9]+$")
	}
}*/
