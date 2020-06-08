package org.koitharu.kotatsu.ui.list.favourites.categories

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_categories.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.common.dialog.TextInputDialog
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.showPopupMenu

class CategoriesActivity : BaseActivity(), OnRecyclerItemClickListener<FavouriteCategory>,
	FavouriteCategoriesView, View.OnClickListener {

	private val presenter by moxyPresenter(factory = ::FavouriteCategoriesPresenter)

	private lateinit var adapter: CategoriesAdapter
	private lateinit var reorderHelper: ItemTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_categories)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fab_add.imageTintList = ColorStateList.valueOf(Color.WHITE)
		adapter = CategoriesAdapter(this)
		recyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
		recyclerView.adapter = adapter
		fab_add.setOnClickListener(this)
		reorderHelper = ItemTouchHelper(ReorderHelperCallback())
		reorderHelper.attachToRecyclerView(recyclerView)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab_add -> createCategory()
		}
	}

	override fun onItemClick(item: FavouriteCategory, position: Int, view: View) {
		view.showPopupMenu(R.menu.popup_category) {
			when (it.itemId) {
				R.id.action_remove -> deleteCategory(item)
				R.id.action_rename -> renameCategory(item)
			}
			true
		}
	}

	override fun onItemLongClick(item: FavouriteCategory, position: Int, view: View): Boolean {
		reorderHelper.startDrag(recyclerView.findViewHolderForAdapterPosition(position) ?: return false)
		return true
	}

	override fun onCategoriesChanged(categories: List<FavouriteCategory>) {
		adapter.replaceData(categories)
		textView_holder.isVisible = categories.isEmpty()
	}

	override fun onCheckedCategoriesChanged(checkedIds: Set<Int>) = Unit

	override fun onError(e: Throwable) {
		Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
			.show()
	}

	private fun deleteCategory(category: FavouriteCategory) {
		MaterialAlertDialogBuilder(this)
			.setMessage(getString(R.string.category_delete_confirm, category.title))
			.setTitle(R.string.remove_category)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.remove) { _, _ ->
				presenter.deleteCategory(category.id)
			}.create()
			.show()
	}

	private fun renameCategory(category: FavouriteCategory) {
		TextInputDialog.Builder(this)
			.setTitle(R.string.rename)
			.setText(category.title)
			.setHint(R.string.enter_category_name)
			.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
			.setNegativeButton(android.R.string.cancel)
			.setMaxLength(12, false)
			.setPositiveButton(R.string.rename) { _, name ->
				presenter.renameCategory(category.id, name)
			}.create()
			.show()
	}

	private fun createCategory() {
		TextInputDialog.Builder(this)
			.setTitle(R.string.add_new_category)
			.setHint(R.string.enter_category_name)
			.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
			.setNegativeButton(android.R.string.cancel)
			.setMaxLength(12, false)
			.setPositiveButton(R.string.add) { _, name ->
				presenter.createCategory(name)
			}.create()
			.show()
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
			presenter.storeCategoriesOrder(adapter.items.map { it.id })
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, CategoriesActivity::class.java)
	}
}