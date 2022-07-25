package org.koitharu.kotatsu.favourites.ui.categories.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.SheetFavoriteCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouriteCategoriesBottomSheet :
	BaseBottomSheet<SheetFavoriteCategoriesBinding>(),
	OnListItemClickListener<MangaCategoryItem>,
	View.OnClickListener,
	Toolbar.OnMenuItemClickListener {

	@Inject
	lateinit var viewModelFactory: MangaCategoriesViewModel.Factory

	private val viewModel by assistedViewModels {
		viewModelFactory.create(
			requireNotNull(arguments?.getParcelableArrayList<ParcelableManga>(KEY_MANGA_LIST)).map { it.manga },
		)
	}

	private var adapter: MangaCategoriesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = SheetFavoriteCategoriesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		binding.buttonDone.setOnClickListener(this)
		binding.headerBar.toolbar.setOnMenuItemClickListener(this)

		viewModel.content.observe(viewLifecycleOwner, this::onContentChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> dismiss()
		}
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_create -> startActivity(FavouritesCategoryEditActivity.newIntent(requireContext()))
			else -> return false
		}
		return true
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.id, !item.isChecked)
	}

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
				manga.mapTo(ArrayList(manga.size)) { ParcelableManga(it, withChapters = false) },
			)
		}.show(fm, TAG)
	}
}
