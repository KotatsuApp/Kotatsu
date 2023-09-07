package org.koitharu.kotatsu.favourites.ui.categories.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetFavoriteCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

@AndroidEntryPoint
class FavouriteSheet :
	BaseAdaptiveSheet<SheetFavoriteCategoriesBinding>(),
	OnListItemClickListener<MangaCategoryItem> {

	private val viewModel: MangaCategoriesViewModel by viewModels()

	private var adapter: MangaCategoriesAdapter? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetFavoriteCategoriesBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(
		binding: SheetFavoriteCategoriesBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		viewModel.content.observe(viewLifecycleOwner, this::onContentChanged)
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.category.id, !item.isChecked)
	}

	private fun onContentChanged(categories: List<ListModel>) {
		adapter?.items = categories
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	companion object {

		private const val TAG = "FavouriteCategoriesDialog"
		const val KEY_MANGA_LIST = "manga_list"

		fun show(fm: FragmentManager, manga: Manga) = Companion.show(fm, listOf(manga))

		fun show(fm: FragmentManager, manga: Collection<Manga>) =
			FavouriteSheet().withArgs(1) {
				putParcelableArrayList(
					KEY_MANGA_LIST,
					manga.mapTo(ArrayList(manga.size)) {
						ParcelableManga(it)
					},
				)
			}.showDistinct(fm, TAG)
	}
}
