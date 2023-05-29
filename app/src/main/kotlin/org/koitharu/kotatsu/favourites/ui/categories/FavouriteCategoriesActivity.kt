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
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.scaleUpActivityOptionsOf
import org.koitharu.kotatsu.databinding.ActivityCategoriesBinding
import org.koitharu.kotatsu.favourites.ui.FavouritesActivity
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoriesAdapter
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.SortOrder
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

	private lateinit var exitReorderModeCallback: ExitReorderModeCallback
	private lateinit var adapter: CategoriesAdapter
	private lateinit var selectionController: ListSelectionController
	private var reorderHelper: ItemTouchHelper? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCategoriesBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		exitReorderModeCallback = ExitReorderModeCallback(viewModel)
		adapter = CategoriesAdapter(coil, this, this, this)
		selectionController = ListSelectionController(
			activity = this,
			decoration = CategoriesSelectionDecoration(this),
			registryOwner = this,
			callback = CategoriesSelectionCallback(viewBinding.recyclerView, viewModel),
		)
		viewBinding.buttonDone.setOnClickListener(this)
		selectionController.attachToRecyclerView(viewBinding.recyclerView)
		viewBinding.recyclerView.setHasFixedSize(true)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.fabAdd.setOnClickListener(this)
		onBackPressedDispatcher.addCallback(exitReorderModeCallback)

		viewModel.detalizedCategories.observe(this, ::onCategoriesChanged)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
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
			bottom = insets.bottom,
		)
	}

	private fun onCategoriesChanged(categories: List<ListModel>) {
		adapter.items = categories
		invalidateOptionsMenu()
	}

	private fun onReorderModeChanged(isReorderMode: Boolean) {
		val transition = Fade().apply {
			duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
		}
		TransitionManager.beginDelayedTransition(viewBinding.toolbar, transition)
		reorderHelper?.attachToRecyclerView(null)
		reorderHelper = if (isReorderMode) {
			selectionController.clear()
			viewBinding.fabAdd.hide()
			ItemTouchHelper(ReorderHelperCallback()).apply {
				attachToRecyclerView(viewBinding.recyclerView)
			}
		} else {
			viewBinding.fabAdd.show()
			null
		}
		viewBinding.recyclerView.isNestedScrollingEnabled = !isReorderMode
		invalidateOptionsMenu()
		viewBinding.buttonDone.isVisible = isReorderMode
		exitReorderModeCallback.isEnabled = isReorderMode
	}

	private inner class ReorderHelperCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
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

	private class ExitReorderModeCallback(
		private val viewModel: FavouritesCategoriesViewModel,
	) : OnBackPressedCallback(viewModel.isInReorderMode()) {

		override fun handleOnBackPressed() {
			viewModel.setReorderMode(false)
		}
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
