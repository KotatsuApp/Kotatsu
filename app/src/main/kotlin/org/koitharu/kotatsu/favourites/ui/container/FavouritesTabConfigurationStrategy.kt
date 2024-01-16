package org.koitharu.kotatsu.favourites.ui.container

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.util.PopupMenuMediator

class FavouritesTabConfigurationStrategy(
	private val adapter: FavouritesContainerAdapter,
	private val viewModel: FavouritesContainerViewModel,
) : TabConfigurationStrategy {

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = adapter.getItem(position)
		tab.text = item.title ?: tab.view.context.getString(R.string.all_favourites)
		tab.tag = item
		PopupMenuMediator(FavouriteTabPopupMenuProvider(tab.view.context, viewModel, item.id)).attach(tab.view)
	}
}
