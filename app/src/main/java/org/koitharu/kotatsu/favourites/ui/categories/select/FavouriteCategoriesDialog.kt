package org.koitharu.kotatsu.favourites.ui.categories.select

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_favorite_categories.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseBottomSheet
import org.koitharu.kotatsu.base.ui.dialog.TextInputDialog
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouriteCategoriesDialog : BaseBottomSheet(R.layout.dialog_favorite_categories),
	OnCategoryCheckListener {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()

	private val manga get() = arguments?.getParcelable<Manga>(ARG_MANGA)

	private var adapter: CategoriesSelectAdapter? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter =
			CategoriesSelectAdapter(
				this
			)
		recyclerView_categories.adapter = adapter
		textView_add.setOnClickListener {
			createCategory()
		}
		manga?.let {
			viewModel.observeMangaCategories(it.id)
		}

		viewModel.categories.observe(viewLifecycleOwner, ::onCategoriesChanged)
		viewModel.mangaCategories.observe(viewLifecycleOwner, ::onCheckedCategoriesChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	private fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		adapter?.replaceData(categories)
	}

	private fun onCheckedCategoriesChanged(checkedIds: Set<Long>) {
		adapter?.setCheckedIds(checkedIds)
	}

	override fun onCategoryChecked(category: FavouriteCategory) {
		viewModel.addToCategory(manga ?: return, category.id)
	}

	override fun onCategoryUnchecked(category: FavouriteCategory) {
		viewModel.removeFromCategory(manga ?: return, category.id)
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	private fun createCategory() {
		TextInputDialog.Builder(context ?: return)
			.setTitle(R.string.add_new_category)
			.setHint(R.string.enter_category_name)
			.setMaxLength(12, false)
			.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
			.setNegativeButton(android.R.string.cancel)
			.setPositiveButton(R.string.add) { _, name ->
				viewModel.createCategory(name)
			}.create()
			.show()
	}

	companion object {

		private const val ARG_MANGA = "manga"
		private const val TAG = "FavouriteCategoriesDialog"

		fun show(fm: FragmentManager, manga: Manga) = FavouriteCategoriesDialog()
			.withArgs(1) {
				putParcelable(ARG_MANGA, manga)
			}.show(
				fm,
				TAG
			)
	}
}