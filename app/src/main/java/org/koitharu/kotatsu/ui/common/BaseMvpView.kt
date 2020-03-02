package org.koitharu.kotatsu.ui.common

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution

interface BaseMvpView : MvpView {

	@OneExecution
	fun onError(e: Throwable)

	@AddToEndSingle
	fun onLoadingStateChanged(isLoading: Boolean)
}