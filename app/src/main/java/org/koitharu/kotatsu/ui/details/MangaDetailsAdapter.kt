package org.koitharu.kotatsu.ui.details

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.koitharu.kotatsu.R

class MangaDetailsAdapter(private val resources: Resources, fm: FragmentManager) : FragmentPagerAdapter(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

	override fun getCount() = 2

	override fun getItem(position: Int): Fragment = when(position) {
		0 -> MangaDetailsFragment()
		1 -> ChaptersFragment()
		else -> throw IndexOutOfBoundsException("No fragment for position $position")
	}

	override fun getPageTitle(position: Int): CharSequence? = when(position) {
		0 -> resources.getString(R.string.details)
		1 -> resources.getString(R.string.chapters)
		else -> null
	}
}