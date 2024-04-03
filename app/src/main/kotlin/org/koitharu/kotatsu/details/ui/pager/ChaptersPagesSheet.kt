package org.koitharu.kotatsu.details.ui.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet
import org.koitharu.kotatsu.core.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.util.ext.recyclerView
import org.koitharu.kotatsu.core.util.ext.setTabsEnabled
import org.koitharu.kotatsu.databinding.SheetChaptersPagesBinding
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersPagesSheet : BaseAdaptiveSheet<SheetChaptersPagesBinding>(), ActionModeListener {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetChaptersPagesBinding {
		return SheetChaptersPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetChaptersPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = DetailsPagerAdapter2(this, settings)
		binding.pager.recyclerView?.isNestedScrollingEnabled = false
		binding.pager.offscreenPageLimit = 1
		binding.pager.adapter = adapter
		TabLayoutMediator(binding.tabs, binding.pager, adapter).attach()
		binding.pager.setCurrentItem(settings.defaultDetailsTab, false)
		binding.tabs.isVisible = adapter.itemCount > 1

		actionModeDelegate.addListener(this, viewLifecycleOwner)
	}

	override fun onActionModeStarted(mode: ActionMode) {
		setExpanded(true, true)
		viewBinding?.run {
			pager.isUserInputEnabled = false
			tabs.setTabsEnabled(false)
		}
	}

	override fun onActionModeFinished(mode: ActionMode) {
		setExpanded(isExpanded, false)
		viewBinding?.run {
			pager.isUserInputEnabled = true
			tabs.setTabsEnabled(true)
		}
	}
}
