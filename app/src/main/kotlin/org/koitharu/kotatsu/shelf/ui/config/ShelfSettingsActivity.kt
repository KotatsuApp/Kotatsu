package org.koitharu.kotatsu.shelf.ui.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.ActivityShelfSettingsBinding
import com.google.android.material.R as materialR

@AndroidEntryPoint
class ShelfSettingsActivity :
	BaseActivity<ActivityShelfSettingsBinding>(),
	View.OnClickListener, ShelfSettingsListener {

	private val viewModel by viewModels<ShelfSettingsViewModel>()
	private lateinit var reorderHelper: ItemTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityShelfSettingsBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		viewBinding.buttonDone.setOnClickListener(this)
		val settingsAdapter = ShelfSettingsAdapter(this)
		with(viewBinding.recyclerView) {
			setHasFixedSize(true)
			adapter = settingsAdapter
			reorderHelper = ItemTouchHelper(SectionsReorderCallback()).also {
				it.attachToRecyclerView(this)
			}
		}
		viewModel.content.observe(this) { settingsAdapter.items = it }
	}

	override fun onItemCheckedChanged(item: ShelfSettingsItemModel, isChecked: Boolean) {
		viewModel.setItemChecked(item, isChecked)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder) {
		reorderHelper.startDrag(holder)
	}

	override fun onClick(v: View?) {
		finishAfterTransition()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
		viewBinding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
	}

	private inner class SectionsReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = viewHolder.itemViewType == target.itemViewType && viewModel.reorderSections(
			viewHolder.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun isLongPressDragEnabled() = false
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, ShelfSettingsActivity::class.java)
	}
}
