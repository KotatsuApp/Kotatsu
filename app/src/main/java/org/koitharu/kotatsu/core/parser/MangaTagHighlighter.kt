package org.koitharu.kotatsu.core.parser

import android.content.Context
import androidx.annotation.ColorRes
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.MangaTag
import javax.inject.Inject

@Reusable
class MangaTagHighlighter @Inject constructor(
	@ApplicationContext context: Context,
) {

	private val dict by lazy {
		context.resources.openRawResource(R.raw.tags_redlist).use {
			val set = HashSet<String>()
			it.bufferedReader().forEachLine { x -> set.add(x) }
			set
		}
	}

	@ColorRes
	fun getTint(tag: MangaTag): Int {
		return if (tag.title.lowercase() in dict) {
			R.color.warning
		} else {
			0
		}
	}
}
