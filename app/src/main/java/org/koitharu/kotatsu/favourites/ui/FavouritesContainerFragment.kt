package org.koitharu.kotatsu.favourites.ui

import android.os.Bundle
import android.view.*
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.databinding.FragmentFavouritesBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.utils.RecycledViewPoolHolder
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.showPopupMenu
import java.util.*

class FavouritesContainerFragment : BaseFragment<FragmentFavouritesBinding>(),
	FavouritesTabLongClickListener, CategoriesEditDelegate.CategoriesEditCallback,
	RecycledViewPoolHolder {

	override val recycledViewPool = RecyclerView.RecycledViewPool()

	private val viewModel by viewModel<FavouritesCategoriesViewModel>(
		mode = LazyThreadSafetyMode.NONE
	)
	private val editDelegate by lazy(LazyThreadSafetyMode.NONE) {
		CategoriesEditDelegate(requireContext(), this)
	}
	private var pagerAdapter: FavouritesPagerAdapter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentFavouritesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = FavouritesPagerAdapter(this, this)
		viewModel.categories.value?.let {
			adapter.replaceData(wrapCategories(it))
		}
		binding.pager.adapter = adapter
		pagerAdapter = adapter
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()

		viewModel.categories.observe(viewLifecycleOwner, ::onCategoriesChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		pagerAdapter = null
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.tabs.updatePadding(
			left = insets.left,
			right = insets.right
		)
	}

	private fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		pagerAdapter?.replaceData(wrapCategories(categories))
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
		return context?.getString(R.string.favourites)
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	override fun onTabLongClick(tabView: View, category: FavouriteCategory): Boolean {
		val menuRes = if (category.id == 0L) R.menu.popup_category_empty else R.menu.popup_category
		tabView.showPopupMenu(menuRes, { menu ->
			createOrderSubmenu(menu, category)
		}) {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(category)
				R.id.action_rename -> editDelegate.renameCategory(category)
				R.id.action_create -> editDelegate.createCategory()
				R.id.action_order -> return@showPopupMenu false
				else -> {
					val order = CategoriesActivity.SORT_ORDERS.getOrNull(it.order)
						?: return@showPopupMenu false
					viewModel.setCategoryOrder(category.id, order)
				}
			}
			true
		}
		return true
	}

	override fun onDeleteCategory(category: FavouriteCategory) {
		viewModel.deleteCategory(category.id)
	}

	override fun onRenameCategory(category: FavouriteCategory, newName: String) {
		viewModel.renameCategory(category.id, newName)
	}

	override fun onCreateCategory(name: String) {
		viewModel.createCategory(name)
	}

	private fun wrapCategories(categories: List<FavouriteCategory>): List<FavouriteCategory> {
		val data = ArrayList<FavouriteCategory>(categories.size + 1)
		data += FavouriteCategory(0L, getString(R.string.all_favourites), -1, SortOrder.NEWEST, Date())
		data += categories
		return data
	}

	private fun createOrderSubmenu(menu: Menu, category: FavouriteCategory) {
		val submenu = menu.findItem(R.id.action_order)?.subMenu ?: return
		for ((i, item) in CategoriesActivity.SORT_ORDERS.withIndex()) {
			val menuItem = submenu.add(
				R.id.group_order,
				Menu.NONE,
				i,
				item.titleRes
			)
			menuItem.isCheckable = true
			menuItem.isChecked = item == category.order
		}
		submenu.setGroupCheckable(R.id.group_order, true, true)
	}

	companion object {

		fun newInstance() = FavouritesContainerFragment()
	}
}