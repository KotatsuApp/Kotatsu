package org.koitharu.kotatsu.ui.list.favourites

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_favourites.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.domain.favourites.OnFavouritesChangeListener
import org.koitharu.kotatsu.ui.base.BaseFragment
import org.koitharu.kotatsu.ui.list.favourites.categories.CategoriesActivity
import org.koitharu.kotatsu.ui.list.favourites.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.ui.list.favourites.categories.FavouriteCategoriesPresenter
import org.koitharu.kotatsu.ui.list.favourites.categories.FavouriteCategoriesView
import org.koitharu.kotatsu.utils.ext.showPopupMenu
import java.util.*
import kotlin.collections.ArrayList

class FavouritesContainerFragment : BaseFragment(R.layout.fragment_favourites),
	FavouriteCategoriesView, OnFavouritesChangeListener, FavouritesTabLongClickListener,
	CategoriesEditDelegate.CategoriesEditCallback {

	private val presenter by moxyPresenter(factory = ::FavouriteCategoriesPresenter)
	private val editDelegate by lazy(LazyThreadSafetyMode.NONE) {
		CategoriesEditDelegate(requireContext(), this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = FavouritesPagerAdapter(this, this)
		pager.adapter = adapter
		TabLayoutMediator(tabs, pager, adapter).attach()
		FavouritesRepository.subscribe(this)
	}

	override fun onDestroyView() {
		FavouritesRepository.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		val data = ArrayList<FavouriteCategory>(categories.size + 1)
		data += FavouriteCategory(0L, getString(R.string.all_favourites), -1, Date())
		data += categories
		(pager.adapter as? FavouritesPagerAdapter)?.replaceData(data)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_favourites, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_categories -> {
			context?.let {
				startActivity(CategoriesActivity.newIntent(it))
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.favourites)
	}

	override fun onCheckedCategoriesChanged(checkedIds: Set<Int>) = Unit

	override fun onError(e: Throwable) {
		Snackbar.make(pager, e.message ?: return, Snackbar.LENGTH_LONG).show()
	}

	override fun onFavouritesChanged(mangaId: Long) = Unit

	override fun onCategoriesChanged() {
		presenter.loadAllCategories()
	}

	override fun onTabLongClick(tabView: View, category: FavouriteCategory): Boolean {
		val menuRes = if (category.id == 0L) R.menu.popup_category_empty else R.menu.popup_category
		tabView.showPopupMenu(menuRes) {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(category)
				R.id.action_rename -> editDelegate.renameCategory(category)
				R.id.action_create -> editDelegate.createCategory()
			}
			true
		}
		return true
	}

	override fun onDeleteCategory(category: FavouriteCategory) {
		presenter.deleteCategory(category.id)
	}

	override fun onRenameCategory(category: FavouriteCategory, newName: String) {
		presenter.renameCategory(category.id, newName)
	}

	override fun onCreateCategory(name: String) {
		presenter.createCategory(name)
	}

	companion object {

		fun newInstance() = FavouritesContainerFragment()
	}
}