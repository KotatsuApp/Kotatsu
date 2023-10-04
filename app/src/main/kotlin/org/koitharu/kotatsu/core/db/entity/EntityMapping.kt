package org.koitharu.kotatsu.core.db.entity

import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase

// Entity to model

fun TagEntity.toMangaTag() = MangaTag(
	key = this.key,
	title = this.title.toTitleCase(),
	source = MangaSource(this.source),
)

fun Collection<TagEntity>.toMangaTags() = mapToSet(TagEntity::toMangaTag)

fun Collection<TagEntity>.toMangaTagsList() = map(TagEntity::toMangaTag)

fun MangaEntity.toManga(tags: Set<MangaTag>) = Manga(
	id = this.id,
	title = this.title,
	altTitle = this.altTitle,
	state = this.state?.let { MangaState(it) },
	rating = this.rating,
	isNsfw = this.isNsfw,
	url = this.url,
	publicUrl = this.publicUrl,
	coverUrl = this.coverUrl,
	largeCoverUrl = this.largeCoverUrl,
	author = this.author,
	source = MangaSource(this.source),
	tags = tags,
)

fun MangaWithTags.toManga() = manga.toManga(tags.toMangaTags())

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
	id = "${key}_${source.name}".longHashCode(),
)

fun Collection<MangaTag>.toEntities() = map(MangaTag::toEntity)

// Other

fun SortOrder(name: String, fallback: SortOrder): SortOrder = runCatching {
	SortOrder.valueOf(name)
}.getOrDefault(fallback)

fun MangaState(name: String): MangaState? = runCatching {
	MangaState.valueOf(name)
}.getOrNull()
