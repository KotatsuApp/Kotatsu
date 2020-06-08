package org.koitharu.kotatsu.ui.details

import android.os.Bundle
import android.view.View
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.list.MangaListFragment

class RelatedMangaFragment : MangaListFragment<Unit>(), MangaDetailsView {

	private val presenter by moxyPresenter(factory = MangaDetailsPresenter.Companion::getInstance)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		isSwipeRefreshEnabled = false
	}

	override fun onRequestMoreItems(offset: Int) {
		if (offset == 0) {
			presenter.loadRelated()
		}
	}

	override fun onMangaUpdated(manga: Manga) = Unit

	override fun onHistoryChanged(history: MangaHistory?) = Unit

	override fun onFavouriteChanged(categories: List<FavouriteCategory>) = Unit

	override fun onMangaRemoved(manga: Manga) = Unit

	override fun onNewChaptersChanged(newChapters: Int) = Unit

	override fun onListChanged(list: List<Manga>) = super<MangaListFragment>.onListChanged(list)

	override fun onListAppended(list: List<Manga>) = super<MangaListFragment>.onListAppended(list)

	override fun onListError(e: Throwable) = super<MangaListFragment>.onListError(e)
}