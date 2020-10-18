package org.koitharu.kotatsu.ui.utils.protect

import moxy.viewstate.strategy.alias.SingleState
import org.koitharu.kotatsu.ui.base.BaseMvpView

interface ProtectView : BaseMvpView {

	@SingleState
	fun onUnlockSuccess()
}