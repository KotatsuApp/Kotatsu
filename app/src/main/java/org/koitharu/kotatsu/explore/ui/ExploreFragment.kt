package org.koitharu.kotatsu.explore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.databinding.FragmentExploreBinding
import org.koitharu.kotatsu.explore.ui.adapter.ExploreAdapter
import org.koitharu.kotatsu.explore.ui.adapter.ExploreListEventListener
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.settings.SettingsActivity

class ExploreFragment : BaseFragment<FragmentExploreBinding>(),
	RecyclerViewOwner,
	ExploreListEventListener, OnListItemClickListener<ExploreItem.Source> {

	private val viewModel by viewModel<ExploreViewModel>()
	private var exploreAdapter: ExploreAdapter? = null
	private var paddingHorizontal = 0

	override val recyclerView: RecyclerView
		get() = binding.recyclerView

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentExploreBinding {
		return FragmentExploreBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		exploreAdapter = ExploreAdapter(get(), viewLifecycleOwner, this, this)
		with(binding.recyclerView) {
			adapter = exploreAdapter
			setHasFixedSize(true)
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			paddingHorizontal = spacing
		}
		viewModel.content.observe(viewLifecycleOwner) {
			exploreAdapter?.items = it
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		exploreAdapter = null
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		binding.recyclerView.updatePadding(
			left = insets.left + paddingHorizontal,
			right = insets.right + paddingHorizontal,
			bottom = insets.bottom,
		)
	}

	override fun onManageClick(view: View) {
		startActivity(SettingsActivity.newManageSourcesIntent(view.context))
	}

	override fun onClick(v: View) {
		val intent = when (v.id) {
			R.id.button_history -> HistoryActivity.newIntent(v.context)
			else -> return
		}
		startActivity(intent)
	}

	override fun onItemClick(item: ExploreItem.Source, view: View) {
		val intent = MangaListActivity.newIntent(view.context, item.source)
		startActivity(intent)
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

	companion object {

		fun newInstance() = ExploreFragment()
	}
}