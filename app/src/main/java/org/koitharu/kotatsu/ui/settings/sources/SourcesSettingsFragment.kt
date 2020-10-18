package org.koitharu.kotatsu.ui.settings.sources

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_settings_sources.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.base.BaseFragment
import org.koitharu.kotatsu.ui.base.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.settings.SettingsActivity

class SourcesSettingsFragment : BaseFragment(R.layout.fragment_settings_sources),
	OnRecyclerItemClickListener<MangaSource> {

	private lateinit var reorderHelper: ItemTouchHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		reorderHelper = ItemTouchHelper(SourcesReorderCallback())
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.remote_sources)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		recyclerView.addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))
		recyclerView.adapter = SourcesAdapter(this)
		reorderHelper.attachToRecyclerView(recyclerView)
	}

	override fun onDestroyView() {
		reorderHelper.attachToRecyclerView(null)
		super.onDestroyView()
	}

	override fun onItemClick(item: MangaSource, position: Int, view: View) {
		(activity as? SettingsActivity)?.openMangaSourceSettings(item)
	}

	override fun onItemLongClick(item: MangaSource, position: Int, view: View): Boolean {
		reorderHelper.startDrag(
			recyclerView.findViewHolderForAdapterPosition(position) ?: return false
		)
		return true
	}
}