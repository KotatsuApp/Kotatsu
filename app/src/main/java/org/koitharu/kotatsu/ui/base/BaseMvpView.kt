package org.koitharu.kotatsu.ui.base

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution

interface BaseMvpView : MvpView {

	@OneExecution
	fun onError(e: Throwable)

	@AddToEndSingle
	fun onLoadingStateChanged(isLoading: Boolean)
}