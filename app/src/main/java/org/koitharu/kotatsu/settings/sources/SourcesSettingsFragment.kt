package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigAdapter
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigItem
import org.koitharu.kotatsu.settings.sources.adapter.SourceConfigListener

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
		val sourcesAdapter = SourceConfigAdapter(this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
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

	override fun onHeaderClick(header: SourceConfigItem.LocaleHeader) {
		viewModel.expandOrCollapse(header.localeId)
	}

	private inner class SourcesReorderCallback : ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.DOWN or ItemTouchHelper.UP,
		0,
	) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			if (viewHolder.itemViewType != target.itemViewType) {
				return false
			}
			val oldPos = viewHolder.bindingAdapterPosition
			val newPos = target.bindingAdapterPosition
			viewModel.reorderSources(oldPos, newPos)
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

		override fun isLongPressDragEnabled() = false
	}
}