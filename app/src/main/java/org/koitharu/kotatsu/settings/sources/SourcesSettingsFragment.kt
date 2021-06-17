package org.koitharu.kotatsu.settings.sources

import android.os.Bundle
import android.view.*
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.databinding.FragmentSettingsSourcesBinding
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.onboard.OnboardDialogFragment

class SourcesSettingsFragment : BaseFragment<FragmentSettingsSourcesBinding>(),
	OnListItemClickListener<MangaSource> {

	private lateinit var reorderHelper: ItemTouchHelper

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
		with(binding.recyclerView) {
			addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
			adapter = SourcesAdapter(get(), this@SourcesSettingsFragment)
			reorderHelper.attachToRecyclerView(this)
		}
	}

	override fun onDestroyView() {
		reorderHelper.attachToRecyclerView(null)
		super.onDestroyView()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		// TODO handle changes in dialog
		// inflater.inflate(R.menu.opt_sources, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when(item.itemId) {
			R.id.action_languages -> {
				OnboardDialogFragment.show(parentFragmentManager)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
			left = insets.left,
			right = insets.right
		)
	}

	override fun onItemClick(item: MangaSource, view: View) {
		(activity as? SettingsActivity)?.openMangaSourceSettings(item)
	}

	override fun onItemLongClick(item: MangaSource, view: View): Boolean {
		reorderHelper.startDrag(
			binding.recyclerView.findContainingViewHolder(view) ?: return false
		)
		return true
	}
}