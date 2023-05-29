package org.koitharu.kotatsu.details.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MangaDetailsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

	override fun getItemCount() = 2

	override fun createFragment(position: Int): Fragment = when (position) {
		0 -> DetailsFragment()
		1 -> ChaptersFragment()
		else -> throw IndexOutOfBoundsException("No fragment for position $position")
	}
}