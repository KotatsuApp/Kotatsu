package org.koitharu.kotatsu.details.ui.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.util.ext.doOnPageChanged
import org.koitharu.kotatsu.core.util.ext.menuView
import org.koitharu.kotatsu.core.util.ext.recyclerView
import org.koitharu.kotatsu.core.util.ext.setTabsEnabled
import org.koitharu.kotatsu.core.util.ext.showDistinct
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.SheetChaptersPagesBinding
import org.koitharu.kotatsu.details.ui.DetailsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersPagesSheet : BaseAdaptiveSheet<SheetChaptersPagesBinding>(), ActionModeListener {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by activityViewModels<DetailsViewModel>()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetChaptersPagesBinding {
		return SheetChaptersPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetChaptersPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		disableFitToContents()

		val args = arguments ?: Bundle.EMPTY
		val adapter = DetailsPagerAdapter2(this, args.getBoolean(ARG_SHOW_PAGES, settings.isPagesTabEnabled))
		binding.pager.recyclerView?.isNestedScrollingEnabled = false
		binding.pager.offscreenPageLimit = adapter.itemCount
		binding.pager.adapter = adapter
		binding.pager.doOnPageChanged(::onPageChanged)
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()
		binding.pager.setCurrentItem(args.getInt(ARG_TAB, settings.defaultDetailsTab), false)
		binding.tabs.isVisible = adapter.itemCount > 1

		val menuProvider = ChapterPagesMenuProvider(viewModel, this, binding.pager, settings)
		onBackPressedDispatcher.addCallback(viewLifecycleOwner, menuProvider)
		binding.toolbar.addMenuProvider(menuProvider)

		actionModeDelegate.addListener(this, viewLifecycleOwner)
	}

	override fun onActionModeStarted(mode: ActionMode) {
		expandAndLock()
		viewBinding?.toolbar?.menuView?.isEnabled = false
	}

	override fun onActionModeFinished(mode: ActionMode) {
		unlock()
		viewBinding?.toolbar?.menuView?.isEnabled = true
	}

	override fun expandAndLock() {
		super.expandAndLock()
		adjustLockState()
	}

	override fun unlock() {
		super.unlock()
		adjustLockState()
	}

	private fun adjustLockState() {
		viewBinding?.run {
			pager.isUserInputEnabled = !isLocked
			tabs.setTabsEnabled(!isLocked)
		}
	}

	private fun onPageChanged(position: Int) {
		viewBinding?.toolbar?.invalidateMenu()
		settings.lastDetailsTab = position
	}

	companion object {

		const val TAB_CHAPTERS = 0
		const val TAB_PAGES = 1
		const val TAB_BOOKMARKS = 2
		private const val ARG_TAB = "tag"
		private const val ARG_SHOW_PAGES = "pages"
		private const val TAG = "ChaptersPagesSheet"

		fun show(fm: FragmentManager) {
			ChaptersPagesSheet().showDistinct(fm, TAG)
		}

		fun show(fm: FragmentManager, showPagesTab: Boolean) {
			ChaptersPagesSheet().withArgs(1) {
				putBoolean(ARG_SHOW_PAGES, showPagesTab)
			}.showDistinct(fm, TAG)
		}

		fun show(fm: FragmentManager, showPagesTab: Boolean, defaultTab: Int) {
			ChaptersPagesSheet().withArgs(2) {
				putBoolean(ARG_SHOW_PAGES, showPagesTab)
				putInt(ARG_TAB, defaultTab)
			}.showDistinct(fm, TAG)
		}
	}
}
