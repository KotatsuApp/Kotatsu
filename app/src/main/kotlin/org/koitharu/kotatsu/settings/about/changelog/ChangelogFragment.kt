package org.koitharu.kotatsu.settings.about.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.DialogErrorObserver
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.util.ext.consumeAll
import org.koitharu.kotatsu.core.util.ext.container
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.showOrHide
import org.koitharu.kotatsu.core.util.ext.start
import org.koitharu.kotatsu.databinding.FragmentChangelogBinding

@AndroidEntryPoint
class ChangelogFragment : BaseFragment<FragmentChangelogBinding>() {

	private val viewModel: ChangelogViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentChangelogBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentChangelogBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val markwon = Markwon.create(binding.root.context)
		viewModel.isLoading.observe(viewLifecycleOwner) {
			binding.progressBar.showOrHide(it)
		}
		viewModel.onError.observeEvent(viewLifecycleOwner, DialogErrorObserver(binding.root, this))
		viewModel.changelog.filterNotNull()
			.map { markwon.toMarkdown(it) }
			.flowOn(Dispatchers.Default)
			.observe(viewLifecycleOwner) {
				markwon.setParsedMarkdown(binding.textViewContent, it)
			}
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(R.string.changelog)
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat
	): WindowInsetsCompat {
		val typeMask = WindowInsetsCompat.Type.systemBars()
		val barsInsets = insets.getInsets(typeMask)
		val isTablet = !resources.getBoolean(R.bool.is_tablet)
		val isMaster = container?.id == R.id.container_master
		val basePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		requireViewBinding().textViewContent.setPaddingRelative(
			basePadding + if (isTablet && !isMaster) 0 else barsInsets.start(v),
			basePadding,
			basePadding + if (isTablet && isMaster) 0 else barsInsets.end(v),
			basePadding + barsInsets.bottom,
		)
		return insets.consumeAll(typeMask)
	}
}
