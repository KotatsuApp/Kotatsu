package org.koitharu.kotatsu.ui.list

import moxy.viewstate.strategy.alias.OneExecution
import org.koitharu.kotatsu.ui.common.BaseMvpView
import org.koitharu.kotatsu.ui.reader.ReaderState

interface MainView : BaseMvpView {

	@OneExecution
	fun onOpenReader(state: ReaderState)
}