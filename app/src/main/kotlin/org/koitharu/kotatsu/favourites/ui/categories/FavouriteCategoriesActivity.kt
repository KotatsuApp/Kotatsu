package org.koitharu.kotatsu.favourites.ui.categories

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import javax.inject.Inject

@AndroidEntryPoint
class FavouriteCategoriesActivity :
	BaseActivity<ActivityCategoriesBinding>(),
	FavouriteCategoriesListListener,
	View.OnClickListener,
	ListStateHolderListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<FavouritesCategoriesViewModel>()

	private lateinit var adapter: CategoriesAdapter
	private lateinit var selectionController: ListSelectionController
	private lateinit var reorderHelper: ItemTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		adapter = CategoriesAdapter(coil, this, this, this)
		selectionController = ListSelectionController(
			appCompatDelegate = delegate,
			decoration = CategoriesSelectionDecoration(this),
			registryOwner = this,
			callback = CategoriesSelectionCallback(viewBinding.recyclerView, viewModel),
		)
		selectionController.attachToRecyclerView(viewBinding.recyclerView)
		viewBinding.recyclerView.setHasFixedSize(true)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.recyclerView.addItemDecoration(TypedListSpacingDecoration(this, false))
		viewBinding.fabAdd.setOnClickListener(this)

		reorderHelper = ItemTouchHelper(ReorderHelperCallback()).apply {
			attachToRecyclerView(viewBinding.recyclerView)
		}

		viewModel.content.observe(this, ::onCategoriesChanged)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab_add -> startActivity(FavouritesCategoryEditActivity.newIntent(this))
		}
	}

	override fun onItemClick(item: FavouriteCategory?, view: View) {
		if (item == null) {
			if (selectionController.count == 0) {
				startActivity(FavouritesActivity.newIntent(view.context))
			}
			return
		}
		if (selectionController.onItemClick(item.id)) {
			return
		}
		val intent = FavouritesActivity.newIntent(view.context, item)
		startActivity(intent)
	}

	override fun onEditClick(item: FavouriteCategory, view: View) {
		if (selectionController.onItemClick(item.id)) {
			return
		}
		val intent = FavouritesCategoryEditActivity.newIntent(view.context, item.id)
		startActivity(intent)
	}

	override fun onItemLongClick(item: FavouriteCategory?, view: View): Boolean {
		return item != null && selectionController.onItemLongClick(view, item.id)
	}

	override fun onItemContextClick(item: FavouriteCategory?, view: View): Boolean {
		return item != null && selectionController.onItemContextClick(view, item.id)
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		viewBinding.fabAdd.hide()
		viewModel.setActionsEnabled(false)
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		viewBinding.fabAdd.show()
		viewModel.setActionsEnabled(true)
	}

	override fun onShowAllClick(isChecked: Boolean) {
		viewModel.setAllCategoriesVisible(isChecked)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean {
		reorderHelper.startDrag(holder)
		return true
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + insets.right
			leftMargin = topMargin + insets.left
			bottomMargin = topMargin + insets.bottom
		}
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom + viewBinding.recyclerView.paddingTop,
		)
	}

	private suspend fun onCategoriesChanged(categories: List<ListModel>) {
		adapter.emit(categories)
		invalidateOptionsMenu()
	}

	private inner class ReorderHelperCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			return if (actionModeDelegate.isActionModeStarted) {
				0
			} else {
				super.getDragDirs(recyclerView, viewHolder)
			}
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean {
			if (viewHolder.itemViewType != target.itemViewType) {
				return false
			}
			val fromPos = viewHolder.bindingAdapterPosition
			val toPos = target.bindingAdapterPosition
			if (fromPos == toPos || fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
				return false
			}
			adapter.reorderItems(fromPos, toPos)
			return true
		}

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType

		override fun isLongPressDragEnabled(): Boolean = false

		override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
			super.onSelectedChanged(viewHolder, actionState)
			viewBinding.recyclerView.isNestedScrollingEnabled = actionState == ItemTouchHelper.ACTION_STATE_IDLE
		}

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			viewModel.saveOrder(adapter.items ?: return)
		}
	}
}
