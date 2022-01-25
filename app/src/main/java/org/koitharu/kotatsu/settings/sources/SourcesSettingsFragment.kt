package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigItemDecoration
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener
import org.koitharu.kotatsu.settings.sources.model.SourceConfigItem

class SourcesSettingsFragment : BaseFragment<FragmentSettingsSourcesBinding>(),
	SourceConfigListener {

	private lateinit var reorderHelper: ItemTouchHelper
	private val viewModel by viewModel<SourcesSettingsViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		reorderHelper = ItemTouchHelper(SourcesReorderCallback())
		setHasOptionsMenu(true)
	}

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentSettingsSourcesBinding.inflate(inflater, container, false)

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.remote_sources)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sourcesAdapter = SourceConfigAdapter(this, get(), viewLifecycleOwner)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			addItemDecoration(SourceConfigItemDecoration(view.context))
			adapter = sourcesAdapter
			reorderHelper.attachToRecyclerView(this)
		}
		viewModel.items.observe(viewLifecycleOwner) {
			sourcesAdapter.items = it
		}
	}

	override fun onDestroyView() {
		reorderHelper.attachToRecyclerView(null)
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right
		)
	}

	override fun onItemSettingsClick(item: SourceConfigItem.SourceItem) {
		(activity as? SettingsActivity)?.openMangaSourceSettings(item.source)
	}

	override fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		viewModel.setEnabled(item.source, isEnabled)
	}

	override fun onDragHandleTouch(holder: RecyclerView.ViewHolder) {
		reorderHelper.startDrag(holder)
	}

	override fun onHeaderClick(header: SourceConfigItem.LocaleGroup) {
		viewModel.expandOrCollapse(header.localeId)
	}

	private inner class SourcesReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = viewHolder.itemViewType == target.itemViewType && viewModel.reorderSources(
			viewHolder.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun canDropOver(
			recyclerView: RecyclerView,
			current: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder,
		): Boolean = current.itemViewType == target.itemViewType && viewModel.canReorder(
			current.bindingAdapterPosition,
			target.bindingAdapterPosition,
		)

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun isLongPressDragEnabled() = false
	}
}