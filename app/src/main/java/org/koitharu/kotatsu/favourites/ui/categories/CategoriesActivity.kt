package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.measureHeight

class CategoriesActivity :
	BaseActivity<ActivityCategoriesBinding>(),
	OnListItemClickListener<FavouriteCategory>,
	View.OnClickListener,
	CategoriesEditDelegate.CategoriesEditCallback,
	AllCategoriesToggleListener {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()

	private lateinit var adapter: CategoriesAdapter
	private lateinit var reorderHelper: ItemTouchHelper
	private lateinit var editDelegate: CategoriesEditDelegate

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		adapter = CategoriesAdapter(this, this)
		editDelegate = CategoriesEditDelegate(this, this)
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		binding.fabAdd.setOnClickListener(this)
		reorderHelper = ItemTouchHelper(ReorderHelperCallback())
		reorderHelper.attachToRecyclerView(binding.recyclerView)

		viewModel.allCategories.observe(this, ::onCategoriesChanged)
		viewModel.onError.observe(this, ::onError)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab_add -> startActivity(FavouritesCategoryEditActivity.newIntent(this))
		}
	}

	override fun onItemClick(item: FavouriteCategory, view: View) {
		val menu = PopupMenu(view.context, view)
		menu.inflate(R.menu.popup_category)
		menu.setOnMenuItemClickListener { menuItem ->
			when (menuItem.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(item)
				R.id.action_edit -> startActivity(FavouritesCategoryEditActivity.newIntent(this, item.id))
			}
			true
		}
		menu.show()
	}

	override fun onItemLongClick(item: FavouriteCategory, view: View): Boolean {
		val viewHolder = binding.recyclerView.findContainingViewHolder(view) ?: return false
		reorderHelper.startDrag(viewHolder)
		return true
	}

	override fun onAllCategoriesToggle(isVisible: Boolean) {
		viewModel.setAllCategoriesVisible(isVisible)
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
			bottom = 2 * insets.bottom + binding.fabAdd.measureHeight(),
		)
	}

	private fun onCategoriesChanged(categories: List<CategoryListModel>) {
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

	private inner class ReorderHelperCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0
	) {

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = viewHolder.itemViewType == target.itemViewType

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType

		override fun onMoved(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			fromPos: Int,
			target: RecyclerView.ViewHolder,
			toPos: Int,
			x: Int,
			y: Int,
		) {
			super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
			viewModel.reorderCategories(fromPos, toPos)
		}

		override fun isLongPressDragEnabled(): Boolean = false
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