package org.koitharu.kotatsu.favourites.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.children
import androidx.core.view.isVisible
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
import org.koitharu.kotatsu.databinding.ItemEmptyStateBinding
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesEditDelegate
import org.koitharu.kotatsu.favourites.ui.categories.FavouritesCategoriesViewModel
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class FavouritesContainerFragment :
	BaseFragment<FragmentFavouritesBinding>(),
	FavouritesTabLongClickListener,
	CategoriesEditDelegate.CategoriesEditCallback,
	ActionModeListener,
	View.OnClickListener {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()
	private val editDelegate by lazy(LazyThreadSafetyMode.NONE) {
		CategoriesEditDelegate(requireContext(), this)
	}
	private var pagerAdapter: FavouritesPagerAdapter? = null
	private var stubBinding: ItemEmptyStateBinding? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentFavouritesBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = FavouritesPagerAdapter(this, this)
		viewModel.visibleCategories.value?.let(::onCategoriesChanged)
		binding.pager.adapter = adapter
		pagerAdapter = adapter
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()
		actionModeDelegate.addListener(this, viewLifecycleOwner)
		addMenuProvider(FavouritesContainerMenuProvider(view.context))

		viewModel.visibleCategories.observe(viewLifecycleOwner, ::onCategoriesChanged)
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
	}

	override fun onDestroyView() {
		pagerAdapter = null
		stubBinding = null
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
		binding.tabs.apply {
			updatePadding(
				left = insets.left,
				right = insets.right
			)
		}
	}

	private fun onCategoriesChanged(categories: List<CategoryListModel>) {
		pagerAdapter?.replaceData(categories)
		if (categories.isEmpty()) {
			binding.pager.isVisible = false
			binding.tabs.isVisible = false
			showStub()
		} else {
			binding.pager.isVisible = true
			binding.tabs.isVisible = true
			(stubBinding?.root ?: binding.stubEmptyState).isVisible = false
		}
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

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> startActivity(FavouritesCategoryEditActivity.newIntent(v.context))
		}
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
				R.id.action_edit -> startActivity(FavouritesCategoryEditActivity.newIntent(tabView.context, category.id))
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
				R.id.action_create -> startActivity(FavouritesCategoryEditActivity.newIntent(requireContext()))
				R.id.action_hide -> viewModel.setAllCategoriesVisible(false)
			}
			true
		}
		menu.show()
	}

	private fun showStub() {
		val stub = stubBinding ?: ItemEmptyStateBinding.bind(binding.stubEmptyState.inflate())
		stub.root.isVisible = true
		stub.icon.setImageResource(R.drawable.ic_heart_outline)
		stub.textPrimary.setText(R.string.text_empty_holder_primary)
		stub.textSecondary.setText(R.string.empty_favourite_categories)
		stub.buttonRetry.setText(R.string.add)
		stub.buttonRetry.isVisible = true
		stub.buttonRetry.setOnClickListener(this)
		stubBinding = stub
	}

	companion object {

		fun newInstance() = FavouritesContainerFragment()
	}
}