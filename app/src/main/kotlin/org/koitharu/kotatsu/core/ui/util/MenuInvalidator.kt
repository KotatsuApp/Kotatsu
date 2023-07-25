package org.koitharu.kotatsu.core.ui.util

import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.FlowCollector

class MenuInvalidator(
	private val fragment: Fragment,
) : FlowCollector<Any> {

	override suspend fun emit(value: Any) {
		fragment.activity?.invalidateMenu()
	}
}
