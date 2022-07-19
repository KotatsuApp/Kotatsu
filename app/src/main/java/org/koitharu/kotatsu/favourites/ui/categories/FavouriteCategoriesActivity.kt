package org.koitharu.kotatsu.favourites.ui.categories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.measureHeight
import org.koitharu.kotatsu.utils.ext.scaleUpActivityOptionsOf

class FavouriteCategoriesActivity :
	BaseActivity<ActivityCategoriesBinding>(),
	FavouriteCategoriesListListener,
	View.OnClickListener,
	ListStateHolderListener {

	private val viewModel by viewModel<FavouritesCategoriesViewModel>()

	private lateinit var adapter: CategoriesAdapter
	private lateinit var selectionController: ListSelectionController
	private var reorderHelper: ItemTouchHelper? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		adapter = CategoriesAdapter(get(), this, this, this)
		selectionController = ListSelectionController(
			activity = this,
			decoration = CategoriesSelectionDecoration(this),
			registryOwner = this,
			callback = CategoriesSelectionCallback(binding.recyclerView, viewModel),
		)
		binding.buttonDone.setOnClickListener(this)
		selectionController.attachToRecyclerView(binding.recyclerView)
		binding.recyclerView.setHasFixedSize(true)
		binding.recyclerView.adapter = adapter
		binding.fabAdd.setOnClickListener(this)

		viewModel.detalizedCategories.observe(this, ::onCategoriesChanged)
		viewModel.onError.observe(this, ::onError)
		viewModel.isInReorderMode.observe(this, ::onReorderModeChanged)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_categories, menu)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.action_reorder)?.isVisible = !viewModel.isInReorderMode() && !viewModel.isEmpty()
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_reorder -> {
				viewModel.setReorderMode(true)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onBackPressed() {
		if (viewModel.isInReorderMode()) {
			viewModel.setReorderMode(false)
		} else {
			super.onBackPressed()
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> viewModel.setReorderMode(false)
			R.id.fab_add -> startActivity(FavouritesCategoryEditActivity.newIntent(this))
		}
	}

	override fun onItemClick(item: FavouriteCategory, view: View) {
		if (viewModel.isInReorderMode() || selectionController.onItemClick(item.id)) {
			return
		}
		val intent = FavouritesActivity.newIntent(this, item)
		val options = scaleUpActivityOptionsOf(view)
		startActivity(intent, options.toBundle())
	}

	override fun onItemLongClick(item: FavouriteCategory, view: View): Boolean {
		return !viewModel.isInReorderMode() && selectionController.onItemLongClick(item.id)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean {
		return reorderHelper?.startDrag(holder) != null
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

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

	private fun onCategoriesChanged(categories: List<ListModel>) {
		adapter.items = categories
		invalidateOptionsMenu()
	}

	private fun onError(e: Throwable) {
		Snackbar.make(binding.recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
			.show()
	}

	private fun onReorderModeChanged(isReorderMode: Boolean) {
		val transition = Fade().apply {
			duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
		}
		TransitionManager.beginDelayedTransition(binding.toolbar, transition)
		reorderHelper?.attachToRecyclerView(null)
		reorderHelper = if (isReorderMode) {
			selectionController.clear()
			binding.fabAdd.hide()
			ItemTouchHelper(ReorderHelperCallback()).apply {
				attachToRecyclerView(binding.recyclerView)
			}
		} else {
			binding.fabAdd.show()
			null
		}
		binding.recyclerView.isNestedScrollingEnabled = !isReorderMode
		invalidateOptionsMenu()
		binding.buttonDone.isVisible = isReorderMode
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

		fun newIntent(context: Context) = Intent(context, FavouriteCategoriesActivity::class.java)
	}
}
