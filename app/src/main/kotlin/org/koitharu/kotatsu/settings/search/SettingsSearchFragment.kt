package org.koitharu.kotatsu.settings.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentSearchSuggestionBinding
import org.koitharu.kotatsu.list.ui.adapter.ListItemType

@AndroidEntryPoint
class SettingsSearchFragment : BaseFragment<FragmentSearchSuggestionBinding>(), OnListItemClickListener<SettingsItem> {

	private val viewModel: SettingsSearchViewModel by activityViewModels()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSearchSuggestionBinding {
		return FragmentSearchSuggestionBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentSearchSuggestionBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = BaseListAdapter<SettingsItem>()
			.addDelegate(ListItemType.NAV_ITEM, settingsItemAD(this))
		binding.root.adapter = adapter
		binding.root.setHasFixedSize(true)
		viewModel.content.observe(viewLifecycleOwner, adapter)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val extraPadding = resources.getDimensionPixelOffset(R.dimen.list_spacing)
		requireViewBinding().root.updatePadding(
			top = extraPadding,
			right = insets.right,
			left = insets.left,
			bottom = insets.bottom,
		)
	}

	override fun onItemClick(item: SettingsItem, view: View) = viewModel.navigateToPreference(item)
}
