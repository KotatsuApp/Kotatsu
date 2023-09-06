package org.koitharu.kotatsu.core.prefs

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.ListModel

enum class NavItem(
	@IdRes val id: Int,
	@StringRes val title: Int,
	@DrawableRes val icon: Int,
) : ListModel {

	HISTORY(R.id.nav_history, R.string.history, R.drawable.ic_history_selector),
	FAVORITES(R.id.nav_favorites, R.string.favourites, R.drawable.ic_favourites_selector),
	LOCAL(R.id.nav_local, R.string.on_device, R.drawable.ic_storage_selector),
	EXPLORE(R.id.nav_explore, R.string.explore, R.drawable.ic_explore_selector),
	SUGGESTIONS(R.id.nav_suggestions, R.string.suggestions, R.drawable.ic_suggestion_selector),
	FEED(R.id.nav_feed, R.string.feed, R.drawable.ic_feed_selector),
	BOOKMARKS(R.id.nav_bookmarks, R.string.bookmarks, R.drawable.ic_bookmark_selector),
	;

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is NavItem && ordinal == other.ordinal
	}

	fun isAvailable(settings: AppSettings): Boolean = when (this) {
		SUGGESTIONS -> settings.isSuggestionsEnabled
		FEED -> settings.isTrackerEnabled
		else -> true
	}
}
