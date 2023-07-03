package org.koitharu.kotatsu.favourites.ui.container

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.FragmentFavouritesContainerBinding

@AndroidEntryPoint
class FavouritesContainerFragment : BaseFragment<FragmentFavouritesContainerBinding>(), ActionModeListener {

	private val viewModel: FavouritesContainerViewModel by viewModels()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentFavouritesContainerBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentFavouritesContainerBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = FavouritesContainerAdapter(this)
		binding.pager.adapter = adapter
		TabLayoutMediator(
			binding.tabs,
			binding.pager,
			adapter,
		).attach()
		binding.pager.offscreenPageLimit = 1
		actionModeDelegate.addListener(this)
		viewModel.categories.observe(viewLifecycleOwner, adapter)
	}

	override fun onDestroyView() {
		actionModeDelegate.removeListener(this)
		super.onDestroyView()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding?.tabs?.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onActionModeStarted(mode: ActionMode) {
		viewBinding?.run {
			pager.isUserInputEnabled = false
			tabs.isEnabled = false
		}
	}

	override fun onActionModeFinished(mode: ActionMode) {
		viewBinding?.run {
			pager.isUserInputEnabled = true
			tabs.isEnabled = true
		}
	}
}
