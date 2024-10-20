package org.koitharu.kotatsu.search.ui.suggestion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.VoiceInputContract
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentSearchSuggestionBinding
import org.koitharu.kotatsu.search.ui.suggestion.adapter.SearchSuggestionAdapter
import javax.inject.Inject

@AndroidEntryPoint
class SearchSuggestionFragment :
	BaseFragment<FragmentSearchSuggestionBinding>(),
	SearchSuggestionItemCallback.SuggestionItemListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by activityViewModels<SearchSuggestionViewModel>()
	private val voiceInputLauncher = registerForActivityResult(VoiceInputContract()) { result ->
		if (result != null) {
			viewModel.onQueryChanged(result)
		}
	}

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentSearchSuggestionBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentSearchSuggestionBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = SearchSuggestionAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			listener = requireActivity() as SearchSuggestionListener,
		)
		addMenuProvider(SearchSuggestionMenuProvider(binding.root.context, voiceInputLauncher, viewModel))
		binding.root.adapter = adapter
		binding.root.setHasFixedSize(true)
		viewModel.suggestion.observe(viewLifecycleOwner, adapter)
		ItemTouchHelper(SearchSuggestionItemCallback(this))
			.attachToRecyclerView(binding.root)
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

	override fun onRemoveQuery(query: String) {
		viewModel.deleteQuery(query)
	}

	override fun onResume() {
		super.onResume()
		viewModel.onResume()
	}

	companion object {

		@Deprecated("",
			ReplaceWith(
				"SearchSuggestionFragment()",
				"org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionFragment"
			)
		)
		fun newInstance() = SearchSuggestionFragment()
	}
}
