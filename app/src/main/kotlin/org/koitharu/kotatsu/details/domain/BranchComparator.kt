package org.koitharu.kotatsu.details.domain

import org.koitharu.kotatsu.details.ui.model.MangaBranch

class BranchComparator : Comparator<MangaBranch> {

	override fun compare(o1: MangaBranch, o2: MangaBranch): Int = compareValues(o1.name, o2.name)
}
