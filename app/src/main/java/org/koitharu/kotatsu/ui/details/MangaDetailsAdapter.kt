package org.koitharu.kotatsu.ui.details

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MangaDetailsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

	override fun getItemCount() = 3

	override fun createFragment(position: Int): Fragment = when(position) {
		0 -> MangaDetailsFragment()
		1 -> ChaptersFragment()
		2 -> RelatedMangaFragment()
		else -> throw IndexOutOfBoundsException("No fragment for position $position")
	}
}