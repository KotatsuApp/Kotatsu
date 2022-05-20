package org.koitharu.kotatsu.core.db.entity

import java.util.*
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.utils.ext.longHashCode

// Entity to model

fun TagEntity.toMangaTag() = MangaTag(
	key = this.key,
	title = this.title.toTitleCase(),
	source = MangaSource.valueOf(this.source),
)

fun Collection<TagEntity>.toMangaTags() = mapToSet(TagEntity::toMangaTag)

fun MangaEntity.toManga(tags: Set<MangaTag>) = Manga(
	id = this.id,
	title = this.title,
	altTitle = this.altTitle,
	state = this.state?.let { MangaState.valueOf(it) },
	rating = this.rating,
	isNsfw = this.isNsfw,
	url = this.url,
	publicUrl = this.publicUrl,
	coverUrl = this.coverUrl,
	largeCoverUrl = this.largeCoverUrl,
	author = this.author,
	source = MangaSource.valueOf(this.source),
	tags = tags
)

fun MangaWithTags.toManga() = manga.toManga(tags.toMangaTags())

fun TrackLogWithManga.toTrackingLogItem() = TrackingLogItem(
	id = trackLog.id,
	chapters = trackLog.chapters.split('\n').filterNot { x -> x.isEmpty() },
	manga = manga.toManga(tags.toMangaTags()),
	createdAt = Date(trackLog.createdAt)
)

// Model to entity

fun Manga.toEntity() = MangaEntity(
	id = id,
	url = url,
	publicUrl = publicUrl,
	source = source.name,
	largeCoverUrl = largeCoverUrl,
	coverUrl = coverUrl,
	altTitle = altTitle,
	rating = rating,
	isNsfw = isNsfw,
	state = state?.name,
	title = title,
	author = author,
)

fun MangaTag.toEntity() = TagEntity(
	title = title,
	key = key,
	source = source.name,
	id = "${key}_${source.name}".longHashCode()
)

fun Collection<MangaTag>.toEntities() = map(MangaTag::toEntity)

// Other

@Suppress("FunctionName")
fun SortOrder(name: String, fallback: SortOrder): SortOrder = runCatching {
	SortOrder.valueOf(name)
}.getOrDefault(fallback)