package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.showPopupMenu

class CategoriesActivity : BaseActivity<ActivityCategoriesBinding>(),
	OnListItemClickListener<FavouriteCategory>,
	View.OnClickListener, CategoriesEditDelegate.CategoriesEditCallback {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()

	private lateinit var adapter: CategoriesAdapter
	private lateinit var reorderHelper: ItemTouchHelper
	private lateinit var editDelegate: CategoriesEditDelegate

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		binding.fabAdd.imageTintList = ColorStateList.valueOf(Color.WHITE)
		adapter = CategoriesAdapter(this)
		editDelegate = CategoriesEditDelegate(this, this)
		binding.recyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		binding.fabAdd.setOnClickListener(this)
		reorderHelper = ItemTouchHelper(ReorderHelperCallback())
		reorderHelper.attachToRecyclerView(binding.recyclerView)

		viewModel.categories.observe(this, ::onCategoriesChanged)
		viewModel.onError.observe(this, ::onError)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab_add -> editDelegate.createCategory()
		}
	}

	override fun onItemClick(item: FavouriteCategory, view: View) {
		view.showPopupMenu(R.menu.popup_category, { menu ->
			createOrderSubmenu(menu, item)
		}) {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(item)
				R.id.action_rename -> editDelegate.renameCategory(item)
				R.id.action_order -> return@showPopupMenu false
				else -> {
					val order = SORT_ORDERS.getOrNull(it.order) ?: return@showPopupMenu false
					viewModel.setCategoryOrder(item.id, order)
				}
			}
			true
		}
	}

	override fun onItemLongClick(item: FavouriteCategory, view: View): Boolean {
		reorderHelper.startDrag(
			binding.recyclerView.findContainingViewHolder(view) ?: return false
		)
		return true
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + insets.right
			leftMargin = topMargin + insets.left
			bottomMargin = topMargin + insets.bottom
		}
		binding.recyclerView.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom
		)
		binding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
			top = insets.top
		)
	}

	private fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		adapter.items = categories
		binding.textViewHolder.isVisible = categories.isEmpty()
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
			.show()
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

	private fun createOrderSubmenu(menu: Menu, category: FavouriteCategory) {
		val submenu = menu.findItem(R.id.action_order)?.subMenu ?: return
		for ((i, item) in SORT_ORDERS.withIndex()) {
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

	private inner class ReorderHelperCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean = true

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun onMoved(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			fromPos: Int,
			target: RecyclerView.ViewHolder,
			toPos: Int,
			x: Int,
			y: Int
		) {
			super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
			viewModel.reorderCategories(fromPos, toPos)
		}
	}

	companion object {

		val SORT_ORDERS = arrayOf(
			SortOrder.ALPHABETICAL,
			SortOrder.NEWEST,
			SortOrder.RATING,
		)

		fun newIntent(context: Context) = Intent(context, CategoriesActivity::class.java)
	}
}