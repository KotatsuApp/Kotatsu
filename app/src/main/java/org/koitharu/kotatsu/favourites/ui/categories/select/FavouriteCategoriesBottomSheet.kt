package org.koitharu.kotatsu.favourites.ui.categories.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.DialogFavoriteCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouriteCategoriesBottomSheet :
	BaseBottomSheet<DialogFavoriteCategoriesBinding>(),
	OnListItemClickListener<MangaCategoryItem>,
	CategoriesEditDelegate.CategoriesEditCallback,
	Toolbar.OnMenuItemClickListener {

	private val viewModel by viewModel<MangaCategoriesViewModel> {
		parametersOf(requireNotNull(arguments?.getParcelableArrayList<ParcelableManga>(KEY_MANGA_LIST)).map { it.manga })
	}

	private var adapter: MangaCategoriesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogFavoriteCategoriesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		binding.toolbar.setOnMenuItemClickListener(this)

		viewModel.content.observe(viewLifecycleOwner, this::onContentChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_create -> {
				startActivity(FavouritesCategoryEditActivity.newIntent(requireContext()))
				true
			}
			else -> false
		}
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.id, !item.isChecked)
	}

	override fun onDeleteCategory(category: FavouriteCategory) = Unit

	private fun onContentChanged(categories: List<MangaCategoryItem>) {
		adapter?.items = categories
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	companion object {

		private const val TAG = "FavouriteCategoriesDialog"
		private const val KEY_MANGA_LIST = "manga_list"

		fun show(fm: FragmentManager, manga: Manga) = Companion.show(fm, listOf(manga))

		fun show(fm: FragmentManager, manga: Collection<Manga>) = FavouriteCategoriesBottomSheet().withArgs(1) {
			putParcelableArrayList(
				KEY_MANGA_LIST,
				manga.mapTo(ArrayList(manga.size)) { ParcelableManga(it, withChapters = false) }
			)
		}.show(fm, TAG)
	}
}