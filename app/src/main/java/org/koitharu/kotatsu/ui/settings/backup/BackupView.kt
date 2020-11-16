package org.koitharu.kotatsu.ui.settings.backup

import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.SingleState
import org.koitharu.kotatsu.ui.base.BaseMvpView
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.File

interface BackupView : BaseMvpView {

	@AddToEndSingle
	fun onProgressChanged(progress: Progress?)

	@SingleState
	fun onBackupDone(file: File)
}