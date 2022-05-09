package org.koitharu.kotatsu.favourites.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.FragmentFavouritesBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.main.ui.AppBarOwner
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.measureHeight
import org.koitharu.kotatsu.utils.ext.resolveDp

class FavouritesContainerFragment :
	BaseFragment<FragmentFavouritesBinding>(),
	FavouritesTabLongClickListener,
	CategoriesEditDelegate.CategoriesEditCallback,
	ActionModeListener {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()
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
		viewModel.visibleCategories.value?.let {
			adapter.replaceData(it)
		}
		binding.pager.adapter = adapter
		pagerAdapter = adapter
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()
		actionModeDelegate.addListener(this, viewLifecycleOwner)

		viewModel.visibleCategories.observe(viewLifecycleOwner, ::onCategoriesChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		pagerAdapter = null
		super.onDestroyView()
	}

	override fun onActionModeStarted(mode: ActionMode) {
		binding.pager.isUserInputEnabled = false
		binding.tabs.setTabsEnabled(false)
	}

	override fun onActionModeFinished(mode: ActionMode) {
		binding.pager.isUserInputEnabled = true
		binding.tabs.setTabsEnabled(true)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val headerHeight = (activity as? AppBarOwner)?.appBar?.measureHeight() ?: insets.top
		binding.root.updatePadding(
			top = headerHeight - insets.top
		)
		binding.pager.updatePadding(
			// 8 dp is needed so that the top of the list is not attached to tabs (visible when ActionMode is active)
			top = -headerHeight + resources.resolveDp(8)
		)
		binding.tabs.apply {
			updatePadding(
				left = insets.left,
				right = insets.right
			)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	private fun onCategoriesChanged(categories: List<CategoryListModel>) {
		pagerAdapter?.replaceData(categories)
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

	private fun onError(e: Throwable) {
		Snackbar.make(binding.pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
	}

	override fun onTabLongClick(tabView: View, item: CategoryListModel): Boolean {
		when (item) {
			is CategoryListModel.All -> showAllCategoriesMenu(tabView)
			is CategoryListModel.CategoryItem -> showCategoryMenu(tabView, item.category)
		}
		return true
	}

	override fun onDeleteCategory(category: FavouriteCategory) {
		viewModel.deleteCategory(category.id)
	}

	private fun TabLayout.setTabsEnabled(enabled: Boolean) {
		val tabStrip = getChildAt(0) as? ViewGroup ?: return
		for (tab in tabStrip.children) {
			tab.isEnabled = enabled
		}
	}

	private fun showCategoryMenu(tabView: View, category: FavouriteCategory) {
		val menu = PopupMenu(tabView.context, tabView)
		menu.inflate(R.menu.popup_category)
		menu.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(category)
				R.id.action_edit -> FavouritesCategoryEditActivity.newIntent(tabView.context, category.id)
				else -> return@setOnMenuItemClickListener false
			}
			true
		}
		menu.show()
	}

	private fun showAllCategoriesMenu(tabView: View) {
		val menu = PopupMenu(tabView.context, tabView)
		menu.inflate(R.menu.popup_category_all)
		menu.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_create -> FavouritesCategoryEditActivity.newIntent(requireContext())
				R.id.action_hide -> viewModel.setAllCategoriesVisible(false)
			}
			true
		}
		menu.show()
	}

	companion object {

		fun newInstance() = FavouritesContainerFragment()
	}
}