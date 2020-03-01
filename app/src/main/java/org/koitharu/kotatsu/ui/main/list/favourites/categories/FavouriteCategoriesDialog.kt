package org.koitharu.kotatsu.ui.main.list.favourites.categories

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_favorite_categories.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.ui.common.BaseBottomSheet
import org.koitharu.kotatsu.ui.common.dialog.TextInputDialog
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouriteCategoriesDialog() : BaseBottomSheet(R.layout.dialog_favorite_categories),
	FavouriteCategoriesView,
	OnCategoryCheckListener {

	private val presenter by moxyPresenter(factory = ::FavouriteCategoriesPresenter)

	private val manga get() = arguments?.getParcelable<Manga>(ARG_MANGA)

	private var adapter: CategoriesAdapter? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = CategoriesAdapter(this)
		recyclerView_categories.adapter = adapter
		textView_add.setOnClickListener {
			createCategory()
		}
		manga?.let {
			presenter.loadMangaCategories(it)
		}
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		adapter?.replaceData(categories)
	}

	override fun onCheckedCategoriesChanged(checkedIds: Set<Int>) {
		adapter?.setCheckedIds(checkedIds)
	}

	override fun onCategoryChecked(category: FavouriteCategory) {
		presenter.addToCategory(manga ?: return, category.id)
	}

	override fun onCategoryUnchecked(category: FavouriteCategory) {
		presenter.removeFromCategory(manga ?: return, category.id)
	}

	override fun onError(e: Exception) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	private fun createCategory() {
		TextInputDialog.Builder(context ?: return)
			.setTitle(R.string.add_new_category)
			.setHint(R.string.enter_category_name)
			.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
			.setNegativeButton(android.R.string.cancel)
			.setPositiveButton(R.string.add) { _, name ->
				presenter.createCategory(name)
			}.create()
			.show()
	}

	companion object {

		private const val ARG_MANGA = "manga"
		private const val TAG = "FavouriteCategoriesDialog"

		fun show(fm: FragmentManager, manga: Manga) = FavouriteCategoriesDialog().withArgs(1) {
			putParcelable(ARG_MANGA, manga)
		}.show(fm, TAG)
	}
}