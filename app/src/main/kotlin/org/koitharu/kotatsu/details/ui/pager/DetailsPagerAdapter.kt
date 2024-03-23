package org.koitharu.kotatsu.details.ui.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.pager.chapters.ChaptersFragment
import org.koitharu.kotatsu.details.ui.pager.pages.PagesFragment

class DetailsPagerAdapter(
	activity: FragmentActivity,
	settings: AppSettings,
) : FragmentStateAdapter(activity),
	TabLayoutMediator.TabConfigurationStrategy {

	val isPagesTabEnabled = settings.isPagesTabEnabled

	override fun getItemCount(): Int = if (isPagesTabEnabled) 2 else 1

	override fun createFragment(position: Int): Fragment = when (position) {
		0 -> ChaptersFragment()
		1 -> PagesFragment()
		else -> throw IllegalArgumentException("Invalid position $position")
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		tab.setText(
			when (position) {
				0 -> R.string.chapters
				1 -> R.string.pages
				else -> 0
			},
		)
	}
}
