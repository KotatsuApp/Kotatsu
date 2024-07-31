package org.koitharu.kotatsu.core.model

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.collection.MutableObjectIntMap
import androidx.core.os.LocaleListCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.iterator
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.util.formatSimple
import org.koitharu.kotatsu.parsers.util.mapToSet
import com.google.android.material.R as materialR

@JvmName("mangaIds")
fun Collection<Manga>.ids() = mapToSet { it.id }

fun Collection<Manga>.distinctById() = distinctBy { it.id }

@JvmName("chaptersIds")
fun Collection<MangaChapter>.ids() = mapToSet { it.id }

fun Collection<MangaChapter>.findById(id: Long) = find { x -> x.id == id }

fun Collection<ChapterListItem>.countChaptersByBranch(): Int {
	if (size <= 1) {
		return size
	}
	val acc = MutableObjectIntMap<String?>()
	for (item in this) {
		val branch = item.chapter.branch
		acc[branch] = acc.getOrDefault(branch, 0) + 1
	}
	var max = 0
	acc.forEachValue { x -> if (x > max) max = x }
	return max
}

@get:StringRes
val MangaState.titleResId: Int
	get() = when (this) {
		MangaState.ONGOING -> R.string.state_ongoing
		MangaState.FINISHED -> R.string.state_finished
		MangaState.ABANDONED -> R.string.state_abandoned
		MangaState.PAUSED -> R.string.state_paused
		MangaState.UPCOMING -> R.string.state_upcoming
	}

@get:DrawableRes
val MangaState.iconResId: Int
	get() = when (this) {
		MangaState.ONGOING -> R.drawable.ic_play
		MangaState.FINISHED -> R.drawable.ic_state_finished
		MangaState.ABANDONED -> R.drawable.ic_state_abandoned
		MangaState.PAUSED -> R.drawable.ic_action_pause
		MangaState.UPCOMING -> materialR.drawable.ic_clock_black_24dp
	}

@get:StringRes
val ContentRating.titleResId: Int
	get() = when (this) {
		ContentRating.SAFE -> R.string.rating_safe
		ContentRating.SUGGESTIVE -> R.string.rating_suggestive
		ContentRating.ADULT -> R.string.rating_adult
	}

fun Manga.findChapter(id: Long): MangaChapter? {
	return chapters?.findById(id)
}

fun Manga.getPreferredBranch(history: MangaHistory?): String? {
	val ch = chapters
	if (ch.isNullOrEmpty()) {
		return null
	}
	if (history != null) {
		val currentChapter = ch.findById(history.chapterId)
		if (currentChapter != null) {
			return currentChapter.branch
		}
	}
	val groups = ch.groupBy { it.branch }
	if (groups.size == 1) {
		return groups.keys.first()
	}
	for (locale in LocaleListCompat.getAdjustedDefault()) {
		val displayLanguage = locale.getDisplayLanguage(locale)
		val displayName = locale.getDisplayName(locale)
		val candidates = HashMap<String?, List<MangaChapter>>(3)
		for (branch in groups.keys) {
			if (branch != null && (
					branch.contains(displayLanguage, ignoreCase = true) ||
						branch.contains(displayName, ignoreCase = true)
					)
			) {
				candidates[branch] = groups[branch] ?: continue
			}
		}
		if (candidates.isNotEmpty()) {
			return candidates.maxBy { it.value.size }.key
		}
	}
	return groups.maxByOrNull { it.value.size }?.key
}

val Manga.isLocal: Boolean
	get() = source == LocalMangaSource

val Manga.appUrl: Uri
	get() = Uri.parse("https://kotatsu.app/manga").buildUpon()
		.appendQueryParameter("source", source.name)
		.appendQueryParameter("name", title)
		.appendQueryParameter("url", url)
		.build()

fun MangaChapter.formatNumber(): String? = if (number > 0f) {
	number.formatSimple()
} else {
	null
}

fun Manga.chaptersCount(): Int {
	if (chapters.isNullOrEmpty()) {
		return 0
	}
	val counters = MutableObjectIntMap<String?>()
	var max = 0
	chapters?.forEach { x ->
		val c = counters.getOrDefault(x.branch, 0) + 1
		counters[x.branch] = c
		if (max < c) {
			max = c
		}
	}
	return max
}
