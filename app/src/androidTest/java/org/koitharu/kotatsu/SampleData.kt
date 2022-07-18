package org.koitharu.kotatsu

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.parsers.model.*
import java.util.*

object SampleData {

	val manga = Manga(
		id = 1105355890252749533,
		title = "Sasurai Emanon",
		altTitle = null,
		url = "/manga/sasurai_emanon/",
		publicUrl = "https://www.mangatown.com/manga/sasurai_emanon/",
		rating = 1.0f,
		isNsfw = false,
		coverUrl = "https://fmcdn.mangahere.com/store/manga/10992/ocover.jpg?token=905148d2f052f9d3604135933b958771c8b00077&ttl=1658214000&v=1578490983",
		tags = setOf(
			MangaTag(title = "Adventure", key = "0-adventure-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Mature", key = "0-mature-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Psychological", key = "0-psychological-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Slice Of Life", key = "0-slice_of_life-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Supernatural", key = "0-supernatural-0-0-0-0", source = MangaSource.MANGATOWN),
		),
		state = MangaState.ONGOING,
		author = "Kajio Shinji",
		largeCoverUrl = null,
		source = MangaSource.MANGATOWN,
	)

	val mangaDetails = manga.copy(
		tags = setOf(
			MangaTag(title = "Adventure", key = "0-adventure-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Mature", key = "0-mature-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Psychological", key = "0-psychological-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Slice Of Life", key = "0-slice_of_life-0-0-0-0", source = MangaSource.MANGATOWN),
			MangaTag(title = "Supernatural", key = "0-supernatural-0-0-0-0", source = MangaSource.MANGATOWN),
		),
		largeCoverUrl = null,
		description = """
			Based on the award-winning novel by Shinji Kajio, Memories of Emanon tells the story of a mysterious girl
			 who holds a 3-billion-year old memory, dating back to the moment life first appeared on Earth. The first
			  half of the volume is the colored Wandering Emanon '67 chapters (published before as Emanon Episode: 1).
			   The second half is Wandering Emanon set before the '67 chapters.
		""".trimIndent(),
		chapters = listOf(
			MangaChapter(
				id = -7214407414868456892,
				name = "Sasurai Emanon - 1",
				number = 1,
				url = "/manga/sasurai_emanon/c001/",
				scanlator = null,
				uploadDate = 1335906000000,
				branch = null,
				source = MangaSource.MANGATOWN,
			),
			MangaChapter(
				id = -7214407414868456861,
				name = "Sasurai Emanon - 2",
				number = 2,
				url = "/manga/sasurai_emanon/c002/",
				scanlator = null,
				uploadDate = 1335906000000,
				branch = null,
				source = MangaSource.MANGATOWN,
			),
			MangaChapter(
				id = -7214407414868456830,
				name = "Sasurai Emanon - 3",
				number = 3,
				url = "/manga/sasurai_emanon/c003/",
				scanlator = null,
				uploadDate = 1335906000000,
				branch = null,
				source = MangaSource.MANGATOWN,
			),
			MangaChapter(
				id = -7214407414868456799,
				name = "Sasurai Emanon - 4",
				number = 3,
				url = "/manga/sasurai_emanon/c004/",
				scanlator = null,
				uploadDate = 1335906000000,
				branch = null,
				source = MangaSource.MANGATOWN,
			),
		),
	)

	val tag = mangaDetails.tags.elementAt(2)

	val chapter = checkNotNull(mangaDetails.chapters)[2]

	val favouriteCategory = FavouriteCategory(
		id = 4,
		title = "Read later",
		sortKey = 1,
		order = SortOrder.NEWEST,
		createdAt = Date(1335906000000),
		isTrackingEnabled = true,
	)
}