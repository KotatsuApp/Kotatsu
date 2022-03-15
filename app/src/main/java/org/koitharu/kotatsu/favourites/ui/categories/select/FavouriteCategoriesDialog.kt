package org.koitharu.kotatsu.favourites.ui.categories.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.databinding.DialogFavoriteCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouriteCategoriesDialog : BaseBottomSheet<DialogFavoriteCategoriesBinding>(),
	OnListItemClickListener<MangaCategoryItem>, CategoriesEditDelegate.CategoriesEditCallback,
	View.OnClickListener {

	private val viewModel by viewModel<MangaCategoriesViewModel> {
		parametersOf(requireNotNull(arguments?.getParcelable<ParcelableManga>(MangaIntent.KEY_MANGA)).manga)
	}

	private var adapter: MangaCategoriesAdapter? = null
	private val editDelegate by lazy(LazyThreadSafetyMode.NONE) {
		CategoriesEditDelegate(requireContext(), this@FavouriteCategoriesDialog)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogFavoriteCategoriesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		binding.textViewAdd.setOnClickListener(this)

		viewModel.content.observe(viewLifecycleOwner, this::onContentChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.textView_add -> editDelegate.createCategory()
		}
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.id, !item.isChecked)
	}

	override fun onDeleteCategory(category: FavouriteCategory) = Unit

	override fun onRenameCategory(category: FavouriteCategory, newName: String) = Unit

	override fun onCreateCategory(name: String) {
		viewModel.createCategory(name)
	}

	private fun onContentChanged(categories: List<MangaCategoryItem>) {
		adapter?.items = categories
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	companion object {

		private const val TAG = "FavouriteCategoriesDialog"

		fun show(fm: FragmentManager, manga: Manga) = FavouriteCategoriesDialog()
			.withArgs(1) {
				putParcelable(MangaIntent.KEY_MANGA, ParcelableManga(manga))
			}.show(fm, TAG)
	}
}