package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.base.BaseMvpView

interface ReaderView : BaseMvpView {

	@AddToEndSingle
	fun onInitReader(manga: Manga, mode: ReaderMode)

	@OneExecution
	fun onPageSaved(uri: Uri?)
}