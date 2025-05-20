package org.koitharu.kotatsu.details.domain

import org.koitharu.kotatsu.core.util.LocaleStringComparator
import org.koitharu.kotatsu.details.ui.model.MangaBranch

class BranchComparator : Comparator<MangaBranch> {

	private val delegate = LocaleStringComparator()

	override fun compare(o1: MangaBranch, o2: MangaBranch): Int = delegate.compare(o1.name, o2.name)
}
