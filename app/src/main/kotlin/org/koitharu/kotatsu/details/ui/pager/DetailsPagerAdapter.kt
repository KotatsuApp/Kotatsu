package org.koitharu.kotatsu.details.ui.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.details.ui.pager.chapters.ChaptersFragment
import org.koitharu.kotatsu.details.ui.pager.pages.PagesFragment

class DetailsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity),
	TabLayoutMediator.TabConfigurationStrategy {

	override fun getItemCount(): Int = 2

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
