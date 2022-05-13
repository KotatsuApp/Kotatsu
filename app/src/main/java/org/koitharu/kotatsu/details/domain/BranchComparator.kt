package org.koitharu.kotatsu.details.domain

class BranchComparator : Comparator<String?> {

	override fun compare(o1: String?, o2: String?): Int = compareValues(o1, o2)
}