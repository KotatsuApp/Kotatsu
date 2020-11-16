package org.koitharu.kotatsu.ui.settings.backup

import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.SingleState
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.ui.base.BaseMvpView
import org.koitharu.kotatsu.utils.progress.Progress

interface RestoreView : BaseMvpView {

	@AddToEndSingle
	fun onProgressChanged(progress: Progress?)

	@SingleState
	fun onRestoreDone(result: CompositeResult)
}