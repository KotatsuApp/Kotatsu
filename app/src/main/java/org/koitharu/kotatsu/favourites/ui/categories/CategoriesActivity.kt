package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_categories.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.showPopupMenu

class CategoriesActivity : BaseActivity(), OnRecyclerItemClickListener<FavouriteCategory>,
	View.OnClickListener, CategoriesEditDelegate.CategoriesEditCallback {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()

	private lateinit var adapter: CategoriesAdapter
	private lateinit var reorderHelper: ItemTouchHelper
	private lateinit var editDelegate: CategoriesEditDelegate

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_categories)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fab_add.imageTintList = ColorStateList.valueOf(Color.WHITE)
		adapter = CategoriesAdapter(this)
		editDelegate = CategoriesEditDelegate(this, this)
		recyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
		recyclerView.adapter = adapter
		fab_add.setOnClickListener(this)
		reorderHelper = ItemTouchHelper(ReorderHelperCallback())
		reorderHelper.attachToRecyclerView(recyclerView)

		viewModel.categories.observe(this, ::onCategoriesChanged)
		viewModel.onError.observe(this, ::onError)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab_add -> editDelegate.createCategory()
		}
	}

	override fun onItemClick(item: FavouriteCategory, position: Int, view: View) {
		view.showPopupMenu(R.menu.popup_category) {
			when (it.itemId) {
				R.id.action_remove -> editDelegate.deleteCategory(item)
				R.id.action_rename -> editDelegate.renameCategory(item)
			}
			true
		}
	}

	override fun onItemLongClick(item: FavouriteCategory, position: Int, view: View): Boolean {
		reorderHelper.startDrag(
			recyclerView.findViewHolderForAdapterPosition(position) ?: return false
		)
		return true
	}

	private fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		adapter.replaceData(categories)
		textView_holder.isVisible = categories.isEmpty()
	}

	private fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
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

	private inner class ReorderHelperCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP, 0
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			val oldPos = viewHolder.bindingAdapterPosition
			val newPos = target.bindingAdapterPosition
			adapter.moveItem(oldPos, newPos)
			viewModel.storeCategoriesOrder(adapter.items.map { it.id })
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, CategoriesActivity::class.java)
	}
}